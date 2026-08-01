package com.example.colorpick.gpu

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.colorpick.ui.editor.AdjustmentParams
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PhotoRenderer(private val bitmap: Bitmap) : GLSurfaceView.Renderer {

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
        uniform sampler2D uTexture;
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
            vec4 color = texture2D(uTexture, vTexCoord);
            vec3 rgb = clamp3(color.rgb);

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

            if (uVignette > 0.0) {
                vec2 center = vTexCoord - 0.5;
                float dist = length(center);
                float vig = smoothstep(0.5, 1.2, dist);
                rgb *= mix(1.0, 1.0 - uVignette * 0.8, vig);
            }

            gl_FragColor = vec4(clamp3(rgb), color.a);
        }
    """.trimIndent()

    private val vertexCoords = floatArrayOf(
        -1f, 1f, 0f,
        -1f, -1f, 0f,
        1f, -1f, 0f,
        1f, 1f, 0f
    )

    private val texCoords = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 1f,
        1f, 0f
    )

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

    private var program: Int = 0
    private var imageTextureId: Int = 0
    private val vertexBuffer: FloatBuffer
    private val texBuffer: FloatBuffer
    private val drawListBuffer: java.nio.ShortBuffer

    @Volatile
    private var params: AdjustmentParams = AdjustmentParams()

    init {
        val bb = ByteBuffer.allocateDirect(vertexCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply {
            put(vertexCoords)
            position(0)
        }

        val tb = ByteBuffer.allocateDirect(texCoords.size * 4)
        tb.order(ByteOrder.nativeOrder())
        texBuffer = tb.asFloatBuffer().apply {
            put(texCoords)
            position(0)
        }

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

    fun setFilter(lut: Bitmap?, mix: Float) {
        // LUT filtering has been replaced by direct color grading in the shader.
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        program = GlUtils.createProgram(vertexShaderCode, fragmentShaderCode)
        imageTextureId = GlUtils.loadTexture(bitmap)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return

        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 8, texBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTextureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "uTexture"), 0)

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

    private fun setUniform(name: String, value: Float) {
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, name), value)
    }

    fun release() {
        if (imageTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(imageTextureId), 0)
            imageTextureId = 0
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }
}
