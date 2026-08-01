package com.example.colorpick.gpu

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.colorpick.ui.editor.AdjustmentParams
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "CameraFrameRenderer"

class CameraFrameRenderer : GLSurfaceView.Renderer {

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uYTexture;
        uniform sampler2D uUTexture;
        uniform sampler2D uVTexture;
        uniform vec2 uFrameSize;
        uniform float uExposure;
        uniform float uContrast;
        uniform float uBrightness;
        uniform float uHighlights;
        uniform float uShadows;
        uniform float uBlacks;
        uniform float uSaturation;
        uniform float uTemperature;
        uniform float uTint;
        uniform float uWarmth;
        uniform float uHueShift;
        uniform float uVignette;

        vec3 sampleRGB(vec2 coord) {
            float y = texture2D(uYTexture, coord).r;
            float u = texture2D(uUTexture, coord).r - 0.5;
            float v = texture2D(uVTexture, coord).r - 0.5;
            float r = y + 1.402 * v;
            float g = y - 0.344 * u - 0.714 * v;
            float b = y + 1.772 * u;
            return vec3(r, g, b);
        }

        float lum(vec3 c) {
            return dot(c, vec3(0.299, 0.587, 0.114));
        }

        vec3 clamp3(vec3 c) {
            return clamp(c, 0.0, 1.0);
        }

        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }

        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }

        void main() {
            vec3 rgb = clamp3(sampleRGB(vTexCoord));

            // Exposure / brightness / contrast / blacks
            rgb *= (1.0 + uExposure * 0.5);
            rgb += uBrightness * 0.3;
            rgb = (rgb - 0.5) * (1.0 + uContrast) + 0.5;
            rgb = max(rgb + uBlacks * 0.005, 0.0);

            // Highlights / shadows
            float L = lum(rgb);
            float hMask = smoothstep(0.45, 1.0, L);
            float sMask = 1.0 - smoothstep(0.0, 0.45, L);
            rgb = mix(rgb, rgb * (1.0 + uHighlights * 0.35), hMask);
            rgb = mix(rgb, rgb * (1.0 + uShadows * 0.35), sMask);
            rgb = clamp3(rgb);

            // Saturation
            float gray = lum(rgb);
            rgb = mix(vec3(gray), rgb, 1.0 + uSaturation);

            // Temperature / tint / warmth (subtle)
            rgb.r += (uTemperature + uWarmth) * 0.22;
            rgb.b -= (uTemperature - uWarmth * 0.5) * 0.22;
            rgb.g += uTint * 0.22;
            rgb = clamp3(rgb);

            // Hue shift
            if (abs(uHueShift) > 0.001) {
                vec3 hsv = rgb2hsv(rgb);
                hsv.x = fract(hsv.x + uHueShift);
                rgb = clamp3(hsv2rgb(hsv));
            }

            // Vignette
            if (uVignette > 0.0) {
                vec2 center = vTexCoord - 0.5;
                float dist = length(center);
                float vig = smoothstep(0.5, 1.2, dist);
                rgb *= mix(1.0, 1.0 - uVignette * 0.8, vig);
            }

            gl_FragColor = vec4(clamp3(rgb), 1.0);
        }
    """.trimIndent()

    private val vertexCoords = floatArrayOf(
        -1f, 1f, 0f,
        -1f, -1f, 0f,
        1f, -1f, 0f,
        1f, 1f, 0f
    )

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    private var program: Int = 0
    private val textures = IntArray(3)
    private val vertexBuffer: FloatBuffer
    private val texBuffer: FloatBuffer
    private val drawListBuffer: java.nio.ShortBuffer

    @Volatile
    private var params: AdjustmentParams = AdjustmentParams()

    @Volatile
    private var rotationDegrees: Int = 0

    @Volatile
    private var isMirrored: Boolean = false

    private val frameLock = Object()
    // Triple-buffered frame planes: the analyzer thread writes into a free plane set
    // while the GL thread uploads the pending one. A frame is never copied into a
    // buffer that is being read, which previously tore frames into ghosting/color
    // smearing when the scene moved.
    private var pendingPlanes: FramePlanes? = null
    private val freePlanes: MutableList<FramePlanes> =
        mutableListOf(FramePlanes(), FramePlanes(), FramePlanes())
    private var frameWidth: Int = 0
    private var frameHeight: Int = 0
    private var viewportWidth: Int = 0
    private var viewportHeight: Int = 0

    @Volatile
    var onFrameAvailable: (() -> Unit)? = null

    private class FramePlanes {
        var width: Int = 0
        var height: Int = 0
        var yRowStride: Int = 0
        var uRowStride: Int = 0
        var vRowStride: Int = 0
        var uvPixelStride: Int = 1
        var yBuffer: ByteBuffer = ByteBuffer.allocateDirect(4)
        var uBuffer: ByteBuffer = ByteBuffer.allocateDirect(4)
        var vBuffer: ByteBuffer = ByteBuffer.allocateDirect(4)

        fun ensureCapacity(width: Int, height: Int) {
            val ySize = width * height
            val uvSize = (width / 2) * (height / 2)
            if (yBuffer.capacity() < ySize) yBuffer = ByteBuffer.allocateDirect(ySize)
            if (uBuffer.capacity() < uvSize) uBuffer = ByteBuffer.allocateDirect(uvSize)
            if (vBuffer.capacity() < uvSize) vBuffer = ByteBuffer.allocateDirect(uvSize)
            yBuffer.clear()
            uBuffer.clear()
            vBuffer.clear()
        }
    }

    init {
        val bb = ByteBuffer.allocateDirect(vertexCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply {
            put(vertexCoords)
            position(0)
        }

        val tb = ByteBuffer.allocateDirect(8 * 4)
        tb.order(ByteOrder.nativeOrder())
        texBuffer = tb.asFloatBuffer()
        updateTexCoords(0)

        val dlb = ByteBuffer.allocateDirect(drawOrder.size * 2)
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer().apply {
            put(drawOrder)
            position(0)
        }
    }

    fun setParams(newParams: AdjustmentParams) {
        params = newParams
    }

    fun setFilter(lutBitmap: Bitmap?, mix: Float) {
        // LUT filtering has been replaced by direct color grading in the shader.
    }

    fun setRotation(degrees: Int) {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == rotationDegrees) return
        rotationDegrees = normalized
        updateVertexCoords(normalized, frameWidth, frameHeight, viewportWidth, viewportHeight)
        updateTexCoords(normalized, isMirrored)
    }

    fun setMirrored(mirrored: Boolean) {
        if (mirrored == isMirrored) return
        isMirrored = mirrored
        updateTexCoords(rotationDegrees, mirrored)
    }

    fun setViewport(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (width == viewportWidth && height == viewportHeight) return
        viewportWidth = width
        viewportHeight = height
        updateVertexCoords(rotationDegrees, frameWidth, frameHeight, viewportWidth, viewportHeight)
        updateTexCoords(rotationDegrees, isMirrored)
    }

    private fun updateVertexCoords(
        degrees: Int,
        frameW: Int = frameWidth,
        frameH: Int = frameHeight,
        viewportW: Int = viewportWidth,
        viewportH: Int = viewportHeight
    ) {
        if (frameW <= 0 || frameH <= 0 || viewportW <= 0 || viewportH <= 0) {
            val coords = floatArrayOf(
                -1f, 1f, 0f,
                -1f, -1f, 0f,
                1f, -1f, 0f,
                1f, 1f, 0f
            )
            vertexBuffer.apply { clear(); put(coords); position(0) }
            return
        }

        val displayFrameW = if (degrees % 180 == 0) frameW.toFloat() else frameH.toFloat()
        val displayFrameH = if (degrees % 180 == 0) frameH.toFloat() else frameW.toFloat()
        val frameRatio = displayFrameW / displayFrameH
        val viewportRatio = viewportW.toFloat() / viewportH.toFloat()

        val scaleX = if (frameRatio > viewportRatio) frameRatio / viewportRatio else 1f
        val scaleY = if (frameRatio < viewportRatio) viewportRatio / frameRatio else 1f

        val coords = floatArrayOf(
            -scaleX, scaleY, 0f,
            -scaleX, -scaleY, 0f,
            scaleX, -scaleY, 0f,
            scaleX, scaleY, 0f
        )
        vertexBuffer.apply { clear(); put(coords); position(0) }
    }

    private fun updateTexCoords(degrees: Int, mirrored: Boolean = isMirrored) {
        val baseCoords = when (degrees) {
            90 -> floatArrayOf(0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f)
            180 -> floatArrayOf(1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f)
            270 -> floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f)
            else -> floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
        }

        val mirrorCoords = if (mirrored) {
            FloatArray(8).apply {
                for (i in 0 until 4) {
                    this[i * 2] = 1f - baseCoords[i * 2]
                    this[i * 2 + 1] = baseCoords[i * 2 + 1]
                }
            }
        } else {
            baseCoords
        }

        texBuffer.apply { clear(); put(mirrorCoords); position(0) }
    }

    fun updateFrame(
        yBufferSrc: ByteBuffer,
        uBufferSrc: ByteBuffer,
        vBufferSrc: ByteBuffer,
        width: Int,
        height: Int,
        yRowStride: Int,
        uRowStride: Int,
        vRowStride: Int,
        uvPixelStride: Int
    ) {
        if (width <= 0 || height <= 0) return

        // Take a plane set that the GL thread is not currently reading.
        val planes: FramePlanes?
        synchronized(frameLock) {
            planes = freePlanes.removeFirstOrNull()
        }
        if (planes == null) {
            // The GL thread is still uploading previous frames; drop this one instead
            // of tearing a buffer shared with an in-flight upload. Still request a
            // render so the surface never starves waiting for a request.
            onFrameAvailable?.invoke()
            return
        }

        planes.ensureCapacity(width, height)
        copyPlane(planes.yBuffer, yBufferSrc, yRowStride, width, height)
        val uvHeight = height / 2
        val uvWidth = width / 2
        if (uvPixelStride == 1) {
            copyPlane(planes.uBuffer, uBufferSrc, uRowStride, uvWidth, uvHeight)
            copyPlane(planes.vBuffer, vBufferSrc, vRowStride, uvWidth, uvHeight)
        } else {
            sampleInterleavedPlane(planes.uBuffer, uBufferSrc, uRowStride, uvPixelStride, uvWidth, uvHeight)
            sampleInterleavedPlane(planes.vBuffer, vBufferSrc, vRowStride, uvPixelStride, uvWidth, uvHeight)
        }
        planes.width = width
        planes.height = height
        planes.yRowStride = width
        planes.uRowStride = uvWidth
        planes.vRowStride = uvWidth
        planes.uvPixelStride = 1

        synchronized(frameLock) {
            pendingPlanes = planes
        }
        onFrameAvailable?.invoke()
    }

    private fun copyPlane(dst: ByteBuffer, src: ByteBuffer, rowStride: Int, width: Int, height: Int) {
        if (rowStride == width) {
            for (row in 0 until height) {
                val rowSrc = src.duplicate().apply { position(row * rowStride); limit(position() + width) }
                dst.put(rowSrc)
            }
        } else {
            for (row in 0 until height) {
                val rowSrc = src.duplicate().apply { position(row * rowStride); limit(position() + width) }
                dst.put(rowSrc)
            }
        }
        dst.position(0)
    }

    private fun sampleInterleavedPlane(
        dst: ByteBuffer,
        src: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        width: Int,
        height: Int
    ) {
        for (row in 0 until height) {
            val rowStart = row * rowStride
            for (col in 0 until width) {
                dst.put(src.get(rowStart + col * pixelStride))
            }
        }
        dst.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        program = GlUtils.createProgram(vertexShaderCode, fragmentShaderCode)
        if (program == 0) {
            Log.e(TAG, "Shader program failed to create; preview will be black")
        }
        GLES20.glGenTextures(3, textures, 0)
        uploadNeutralFrame(2, 2)
    }

    private fun uploadNeutralFrame(width: Int, height: Int) {
        val ySize = width * height
        val uvSize = maxOf((width / 2) * (height / 2), 1)
        val y = ByteBuffer.allocateDirect(ySize).apply {
            for (i in 0 until ySize) put(0.toByte())
            position(0)
        }
        val uv = ByteBuffer.allocateDirect(uvSize).apply {
            for (i in 0 until uvSize) put(128.toByte())
            position(0)
        }
        frameWidth = width
        frameHeight = height
        uploadPlane(textures[0], width, height, width, y)
        uploadPlane(textures[1], width / 2, height / 2, width / 2, uv)
        uploadPlane(textures[2], width / 2, height / 2, width / 2, uv)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportWidth = width
        viewportHeight = height
        updateVertexCoords(rotationDegrees, frameWidth, frameHeight, viewportWidth, viewportHeight)
        updateTexCoords(rotationDegrees, isMirrored)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return

        val planes: FramePlanes?
        synchronized(frameLock) {
            planes = pendingPlanes
            pendingPlanes = null
        }

        if (planes != null) {
            uploadFrame(planes)
            // Only now is it safe for the analyzer thread to write into this set again.
            synchronized(frameLock) {
                freePlanes.add(planes)
            }
        }

        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, texBuffer)

        bindTexture(0, textures[0], "uYTexture")
        bindTexture(1, textures[1], "uUTexture")
        bindTexture(2, textures[2], "uVTexture")

        setUniform("uFrameSize", frameWidth.toFloat(), frameHeight.toFloat())
        setUniform("uExposure", params.exposure / 100f)
        setUniform("uContrast", params.contrast / 100f)
        setUniform("uBrightness", params.brightness / 100f)
        setUniform("uHighlights", params.highlights / 100f)
        setUniform("uShadows", params.shadows / 100f)
        setUniform("uBlacks", params.blacks / 100f)
        setUniform("uSaturation", params.saturation / 100f)
        setUniform("uTemperature", params.temperature / 100f)
        setUniform("uTint", params.tint / 100f)
        setUniform("uWarmth", params.warmth / 100f)
        setUniform("uHueShift", params.hueShift / 100f)
        setUniform("uVignette", params.vignette / 100f)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun bindTexture(slot: Int, textureId: Int, name: String) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + slot)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, name), slot)
    }

    private fun configureTexture(textureId: Int, nearest: Boolean = false) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        val minFilter = if (nearest) GLES20.GL_NEAREST else GLES20.GL_LINEAR
        val magFilter = if (nearest) GLES20.GL_NEAREST else GLES20.GL_LINEAR
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, magFilter)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun uploadFrame(frame: FramePlanes) {
        frameWidth = frame.width
        frameHeight = frame.height
        updateVertexCoords(rotationDegrees, frame.width, frame.height, viewportWidth, viewportHeight)
        updateTexCoords(rotationDegrees, isMirrored)

        uploadPlane(textures[0], frame.width, frame.height, frame.yRowStride, frame.yBuffer)
        uploadPlane(textures[1], frame.width / 2, frame.height / 2, frame.uRowStride, frame.uBuffer)
        uploadPlane(textures[2], frame.width / 2, frame.height / 2, frame.vRowStride, frame.vBuffer)
    }

    private fun uploadPlane(textureId: Int, width: Int, height: Int, rowStride: Int, buffer: ByteBuffer) {
        if (width <= 0 || height <= 0) return
        val isChroma = textureId == textures[1] || textureId == textures[2]
        configureTexture(textureId, nearest = isChroma)

        val requiredBytes = if (rowStride == width) width * height else rowStride * (height - 1) + width
        if (buffer.remaining() < requiredBytes) {
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, null
            )
            return
        }

        if (rowStride == width) {
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, buffer
            )
        } else {
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
                width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, null
            )
            for (row in 0 until height) {
                buffer.position(row * rowStride)
                val rowBuffer = buffer.slice()
                rowBuffer.limit(width)
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D, 0, 0, row, width, 1,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, rowBuffer
                )
            }
            buffer.position(0)
        }
    }

    private fun setUniform(name: String, value: Float) {
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, name), value)
    }

    private fun setUniform(name: String, x: Float, y: Float) {
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, name), x, y)
    }
}
