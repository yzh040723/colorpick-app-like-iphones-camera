package com.example.colorpick.gpu

import android.graphics.Bitmap
import com.example.colorpick.ui.editor.AdjustmentParams
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object LutGenerator {

    const val LUT_SIZE = 32

    fun generate(filterParams: AdjustmentParams): Bitmap {
        val width = LUT_SIZE * LUT_SIZE
        val height = LUT_SIZE
        val pixels = IntArray(width * height)
        val inv = 1f / (LUT_SIZE - 1f)

        for (b in 0 until LUT_SIZE) {
            val bf = b * inv
            for (g in 0 until LUT_SIZE) {
                val gf = g * inv
                val rowOffset = b * width + g * LUT_SIZE
                for (r in 0 until LUT_SIZE) {
                    val rf = r * inv
                    val graded = applyColorGrade(floatArrayOf(rf, gf, bf), filterParams)
                    val rr = (graded[0] * 255f).toInt().coerceIn(0, 255)
                    val gg = (graded[1] * 255f).toInt().coerceIn(0, 255)
                    val bb = (graded[2] * 255f).toInt().coerceIn(0, 255)
                    pixels[rowOffset + r] = 0xFF000000.toInt() or (rr shl 16) or (gg shl 8) or bb
                }
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun applyColorGrade(rgbIn: FloatArray, p: AdjustmentParams): FloatArray {
        var r = rgbIn[0]
        var g = rgbIn[1]
        var b = rgbIn[2]

        val exposure = p.exposure / 100f
        val contrast = p.contrast / 100f
        val brightness = p.brightness / 100f
        val highlights = p.highlights / 100f
        val shadows = p.shadows / 100f
        val blacks = p.blacks / 100f
        val saturation = p.saturation / 100f
        val vibrance = p.vibrance / 100f
        val temperature = p.temperature / 100f
        val tint = p.tint / 100f
        val warmth = p.warmth / 100f
        val hueShift = p.hueShift / 100f
        val colorGrading = p.colorGrading / 100f
        val highlightTint = p.highlightTint / 100f
        val shadowTint = p.shadowTint / 100f
        val lightShift = p.lightShift / 100f

        // Tone
        r *= (1f + exposure * 0.5f)
        g *= (1f + exposure * 0.5f)
        b *= (1f + exposure * 0.5f)
        r += brightness * 0.3f
        g += brightness * 0.3f
        b += brightness * 0.3f
        r = (r - 0.5f) * (1f + contrast) + 0.5f
        g = (g - 0.5f) * (1f + contrast) + 0.5f
        b = (b - 0.5f) * (1f + contrast) + 0.5f
        r = max(r + blacks * 0.005f, 0f)
        g = max(g + blacks * 0.005f, 0f)
        b = max(b + blacks * 0.005f, 0f)

        // Highlights / shadows
        val L = lum(r, g, b)
        val highlightMask = smoothstep(0.45f, 1f, L)
        val shadowMask = 1f - smoothstep(0f, 0.45f, L)
        r = mix(r, r * (1f + highlights * 0.35f), highlightMask)
        g = mix(g, g * (1f + highlights * 0.35f), highlightMask)
        b = mix(b, b * (1f + highlights * 0.35f), highlightMask)
        r = mix(r, r * (1f + shadows * 0.35f), shadowMask)
        g = mix(g, g * (1f + shadows * 0.35f), shadowMask)
        b = mix(b, b * (1f + shadows * 0.35f), shadowMask)

        // Clamp after tonal curve to prevent channel overflow
        var clamped = clamp3(r, g, b)
        r = clamped[0]; g = clamped[1]; b = clamped[2]

        // Saturation
        val gray = lum(r, g, b)
        r = mix(gray, r, 1f + saturation)
        g = mix(gray, g, 1f + saturation)
        b = mix(gray, b, 1f + saturation)

        // Vibrance
        var hsv = rgb2hsv(clamp3(r, g, b))
        val satBoost = vibrance * (1f - hsv[1]) * 0.35f
        hsv[1] = (hsv[1] + satBoost).coerceIn(0f, 1f)
        val rgbVib = hsv2rgb(hsv)
        r = rgbVib[0]
        g = rgbVib[1]
        b = rgbVib[2]

        // Temperature / tint / warmth (subtle to avoid color fringing)
        r += (temperature + warmth) * 0.22f
        b -= (temperature - warmth * 0.5f) * 0.22f
        g += tint * 0.22f

        // Hue shift
        if (abs(hueShift) > 0.001f) {
            hsv = rgb2hsv(clamp3(r, g, b))
            hsv[0] = (hsv[0] + hueShift) % 1f
            if (hsv[0] < 0f) hsv[0] += 1f
            val rgbHue = hsv2rgb(hsv)
            r = rgbHue[0]
            g = rgbHue[1]
            b = rgbHue[2]
        }

        // Highlight / shadow tints (very subtle to prevent red/blue fringing)
        if (abs(highlightTint) > 0.001f) {
            val tintStrength = highlightMask * (highlightTint * 0.25f).coerceIn(0f, 1f)
            r = mix(r, 1f * (0.45f + L * 0.65f), tintStrength)
            g = mix(g, 0.78f * (0.45f + L * 0.65f), tintStrength)
            b = mix(b, 0.55f * (0.45f + L * 0.65f), tintStrength)
        }
        if (abs(shadowTint) > 0.001f) {
            val tintStrength = shadowMask * (shadowTint * 0.25f).coerceIn(0f, 1f)
            r = mix(r, 0.22f, tintStrength)
            g = mix(g, 0.30f, tintStrength)
            b = mix(b, 0.50f, tintStrength)
        }

        // Clamp after tints before further non-linear ops
        clamped = clamp3(r, g, b)
        r = clamped[0]; g = clamped[1]; b = clamped[2]

        // Color grading & light shift (gentler lift/gain to keep transitions smooth)
        if (abs(colorGrading) > 0.001f) {
            val lift = colorGrading * 0.020f
            val gain = 1f + colorGrading * 0.03f
            r = pow(max(r * gain + lift, 0.001f), 1f - colorGrading * 0.015f)
            g = pow(max(g * gain + lift, 0.001f), 1f - colorGrading * 0.015f)
            b = pow(max(b * gain + lift, 0.001f), 1f - colorGrading * 0.015f)
        }
        if (abs(lightShift) > 0.001f) {
            val lift = lightShift * 0.015f
            r = pow(max(r + lift, 0.001f), 1f - lightShift * 0.020f)
            g = pow(max(g + lift, 0.001f), 1f - lightShift * 0.020f)
            b = pow(max(b + lift, 0.001f), 1f - lightShift * 0.020f)
        }

        return clamp3(r, g, b)
    }

    private fun lum(r: Float, g: Float, b: Float) = 0.299f * r + 0.587f * g + 0.114f * b

    private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun clamp3(r: Float, g: Float, b: Float): FloatArray {
        return floatArrayOf(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
    }

    private fun pow(x: Float, y: Float) = x.pow(y)

    private fun rgb2hsv(rgb: FloatArray): FloatArray {
        val r = rgb[0]
        val g = rgb[1]
        val b = rgb[2]
        val maxC = max(max(r, g), b)
        val minC = min(min(r, g), b)
        val d = maxC - minC
        val h: Float
        when {
            d == 0f -> h = 0f
            maxC == r -> h = ((g - b) / d + 6f) % 6f
            maxC == g -> h = (b - r) / d + 2f
            else -> h = (r - g) / d + 4f
        }
        val s = if (maxC == 0f) 0f else d / maxC
        return floatArrayOf(h / 6f, s, maxC)
    }

    private fun hsv2rgb(hsv: FloatArray): FloatArray {
        val h = hsv[0] * 6f
        val s = hsv[1]
        val v = hsv[2]
        val i = h.toInt()
        val f = h - i
        val p = v * (1f - s)
        val q = v * (1f - f * s)
        val t = v * (1f - (1f - f) * s)
        return when (i % 6) {
            0 -> floatArrayOf(v, t, p)
            1 -> floatArrayOf(q, v, p)
            2 -> floatArrayOf(p, v, t)
            3 -> floatArrayOf(p, q, v)
            4 -> floatArrayOf(t, p, v)
            else -> floatArrayOf(v, p, q)
        }
    }
}
