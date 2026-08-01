package com.example.colorpick.ui.editor

import androidx.compose.ui.graphics.Color

private val TAG = "AdjustmentParams"

val Color.hslHue: Float
    get() {
        val r = red.coerceIn(0f, 1f)
        val g = green.coerceIn(0f, 1f)
        val b = blue.coerceIn(0f, 1f)
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        return when {
            delta == 0f -> 0f
            max == r -> (((g - b) / delta) + 6f) % 6f
            max == g -> ((b - r) / delta) + 2f
            else -> (((r - g) / delta) + 4f + 6f) % 6f
        } * 60f
    }

val Color.hslSaturation: Float
    get() {
        val r = red.coerceIn(0f, 1f)
        val g = green.coerceIn(0f, 1f)
        val b = blue.coerceIn(0f, 1f)
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val lightness = (max + min) / 2f
        return if (delta == 0f || lightness == 0f || lightness == 1f) 0f else delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    }

val Color.hslLightness: Float
    get() {
        val r = red.coerceIn(0f, 1f)
        val g = green.coerceIn(0f, 1f)
        val b = blue.coerceIn(0f, 1f)
        return (maxOf(r, g, b) + minOf(r, g, b)) / 2f
    }

data class AdjustmentParams(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val brightness: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val blacks: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val warmth: Float = 0f,
    val clarity: Float = 0f,
    val sharpness: Float = 0f,
    val noiseReduction: Float = 0f,
    val vignette: Float = 0f,
    val hueShift: Float = 0f,
    val colorGrading: Float = 0f,
    val highlightTint: Float = 0f,
    val shadowTint: Float = 0f,
    val lightShift: Float = 0f
) {
    fun reset(): AdjustmentParams = AdjustmentParams()

    fun scaled(factor: Float): AdjustmentParams = AdjustmentParams(
        exposure = exposure * factor,
        contrast = contrast * factor,
        brightness = brightness * factor,
        highlights = highlights * factor,
        shadows = shadows * factor,
        blacks = blacks * factor,
        saturation = saturation * factor,
        vibrance = vibrance * factor,
        temperature = temperature * factor,
        tint = tint * factor,
        warmth = warmth * factor,
        clarity = clarity * factor,
        sharpness = sharpness * factor,
        noiseReduction = noiseReduction * factor,
        vignette = vignette * factor,
        hueShift = hueShift * factor,
        colorGrading = colorGrading * factor,
        highlightTint = highlightTint * factor,
        shadowTint = shadowTint * factor,
        lightShift = lightShift * factor
    )

    operator fun plus(other: AdjustmentParams): AdjustmentParams = AdjustmentParams(
        exposure = exposure + other.exposure,
        contrast = contrast + other.contrast,
        brightness = brightness + other.brightness,
        highlights = highlights + other.highlights,
        shadows = shadows + other.shadows,
        blacks = blacks + other.blacks,
        saturation = saturation + other.saturation,
        vibrance = vibrance + other.vibrance,
        temperature = temperature + other.temperature,
        tint = tint + other.tint,
        warmth = warmth + other.warmth,
        clarity = clarity + other.clarity,
        sharpness = sharpness + other.sharpness,
        noiseReduction = noiseReduction + other.noiseReduction,
        vignette = vignette + other.vignette,
        hueShift = hueShift + other.hueShift,
        colorGrading = colorGrading + other.colorGrading,
        highlightTint = highlightTint + other.highlightTint,
        shadowTint = shadowTint + other.shadowTint,
        lightShift = lightShift + other.lightShift
    )
}

data class FilterPreset(
    val id: String,
    val name: String,
    val params: AdjustmentParams = AdjustmentParams(),
    val themeColor: Color = Color(0xFFFFA500)
)

private val STANDARD_FILTERS = listOf(
    FilterPreset(
        id = "roseGold",
        name = "玫瑰金",
        params = AdjustmentParams(
            warmth = 22f, saturation = 14f, brightness = 8f,
            highlights = -8f, shadows = 12f, highlightTint = 22f,
            shadowTint = 10f, clarity = 10f, vibrance = 8f
        ),
        themeColor = Color(0xFFE8B4B8)
    ),
    FilterPreset(
        id = "amber",
        name = "琥珀色",
        params = AdjustmentParams(
            warmth = 34f, saturation = 2f, contrast = 10f,
            highlights = -14f, shadows = 18f, blacks = 6f,
            highlightTint = 20f, clarity = 12f
        ),
        themeColor = Color(0xFFFFB347)
    ),
    FilterPreset(
        id = "gold",
        name = "金色",
        params = AdjustmentParams(
            warmth = 30f, saturation = 12f, brightness = 8f,
            highlights = -10f, shadows = 14f, highlightTint = 18f,
            clarity = 8f, vibrance = 6f
        ),
        themeColor = Color(0xFFD4A853)
    ),
    FilterPreset(
        id = "coolRose",
        name = "冷调玫瑰",
        params = AdjustmentParams(
            temperature = -18f, tint = 14f, saturation = 10f,
            highlights = 10f, shadowTint = 18f, clarity = 10f,
            vibrance = 8f
        ),
        themeColor = Color(0xFFC9A9B8)
    ),
    FilterPreset(
        id = "neutral",
        name = "中性",
        params = AdjustmentParams(
            contrast = 10f, clarity = 14f, sharpness = 8f,
            saturation = -6f, shadows = 8f, highlights = -4f
        ),
        themeColor = Color(0xFFB0B0B0)
    )
)

private val MOOD_FILTERS = listOf(
    FilterPreset(
        id = "vivid",
        name = "鲜明",
        params = AdjustmentParams(
            contrast = 10f, saturation = 30f, vibrance = 20f,
            clarity = 10f, brightness = 5f, highlights = -6f
        ),
        themeColor = Color(0xFFFF7F50)
    ),
    FilterPreset(
        id = "fujiContrast",
        name = "反差色",
        params = AdjustmentParams(
            contrast = 28f, saturation = 10f, shadows = 20f,
            highlights = -14f, blacks = 8f, colorGrading = 6f,
            tint = 8f, clarity = 12f
        ),
        themeColor = Color(0xFF4A90A4)
    ),
    FilterPreset(
        id = "ethereal",
        name = "飘渺",
        params = AdjustmentParams(
            brightness = 12f, highlights = 16f, shadows = 18f,
            clarity = -8f, saturation = -10f, colorGrading = 10f,
            lightShift = 8f
        ),
        themeColor = Color(0xFFB0C4DE)
    ),
    FilterPreset(
        id = "warm",
        name = "温馨",
        params = AdjustmentParams(
            warmth = 35f, brightness = 8f, saturation = -5f,
            highlights = -10f, shadows = 12f, highlightTint = 18f
        ),
        themeColor = Color(0xFFFFD27F)
    ),
    FilterPreset(
        id = "passionate",
        name = "热烈",
        params = AdjustmentParams(
            warmth = 20f, saturation = 30f, contrast = 16f,
            vibrance = 20f, colorGrading = 8f, clarity = 10f
        ),
        themeColor = Color(0xFFE74C3C)
    ),
    FilterPreset(
        id = "romantic",
        name = "浪漫",
        params = AdjustmentParams(
            tint = 22f, saturation = 18f, brightness = 10f,
            highlights = 8f, shadowTint = 20f, warmth = 12f,
            clarity = 6f
        ),
        themeColor = Color(0xFFF4A7B9)
    ),
    FilterPreset(
        id = "softContrast",
        name = "柔和反差",
        params = AdjustmentParams(
            contrast = 12f, saturation = -10f, clarity = 10f,
            brightness = 8f, shadows = 14f, highlights = -8f,
            noiseReduction = 8f
        ),
        themeColor = Color(0xFF9B9B9B)
    ),
    FilterPreset(
        id = "highContrastMono",
        name = "高对比黑白",
        params = AdjustmentParams(
            saturation = -100f, contrast = 34f, blacks = 16f,
            clarity = 20f, sharpness = 14f, highlights = -12f,
            shadows = 22f
        ),
        themeColor = Color(0xFF3A3A3A)
    ),
    FilterPreset(
        id = "pearly",
        name = "珠光色",
        params = AdjustmentParams(
            brightness = 12f, highlightTint = 26f, clarity = 12f,
            saturation = -10f, shadows = 12f, colorGrading = 10f,
            lightShift = 8f
        ),
        themeColor = Color(0xFFFFF0E0)
    )
)

val STANDARD_ORIGINAL = FilterPreset(
    id = "standard",
    name = "标准",
    params = AdjustmentParams(),
    themeColor = Color(0xFFE0E0E0)
)

val IOS_FILTERS = STANDARD_FILTERS + STANDARD_ORIGINAL + MOOD_FILTERS

fun AdjustmentParams.withFilter(filter: FilterPreset, intensity: Int): AdjustmentParams {
    if (intensity <= 0) return this
    val factor = intensity / 100f
    return AdjustmentParams(
        exposure = exposure + filter.params.exposure * factor,
        contrast = contrast + filter.params.contrast * factor,
        brightness = brightness + filter.params.brightness * factor,
        highlights = highlights + filter.params.highlights * factor,
        shadows = shadows + filter.params.shadows * factor,
        blacks = blacks + filter.params.blacks * factor,
        saturation = saturation + filter.params.saturation * factor,
        vibrance = vibrance + filter.params.vibrance * factor,
        temperature = temperature + filter.params.temperature * factor,
        tint = tint + filter.params.tint * factor,
        warmth = warmth + filter.params.warmth * factor,
        clarity = clarity + filter.params.clarity * factor,
        sharpness = sharpness + filter.params.sharpness * factor,
        noiseReduction = noiseReduction + filter.params.noiseReduction * factor,
        vignette = vignette + filter.params.vignette * factor,
        hueShift = hueShift + filter.params.hueShift * factor,
        colorGrading = colorGrading + filter.params.colorGrading * factor,
        highlightTint = highlightTint + filter.params.highlightTint * factor,
        shadowTint = shadowTint + filter.params.shadowTint * factor,
        lightShift = lightShift + filter.params.lightShift * factor
    )
}
