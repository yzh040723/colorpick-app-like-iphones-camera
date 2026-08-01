package com.example.colorpick.ui.camera

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import com.example.colorpick.ui.editor.AdjustmentParams
import com.example.colorpick.ui.editor.FilterPreset
import com.example.colorpick.ui.editor.IOS_FILTERS
import com.example.colorpick.ui.editor.STANDARD_ORIGINAL
import com.example.colorpick.ui.editor.withFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _params = MutableStateFlow(AdjustmentParams())
    val params: StateFlow<AdjustmentParams> = _params

    private val _squarePanelOffset = MutableStateFlow(Offset.Zero)
    val squarePanelOffset: StateFlow<Offset> = _squarePanelOffset

    private val _selectedFilter = MutableStateFlow(STANDARD_ORIGINAL)
    val selectedFilter: StateFlow<FilterPreset> = _selectedFilter

    private val _filterIntensity = MutableStateFlow(100)
    val filterIntensity: StateFlow<Int> = _filterIntensity

    private val _showAdjustPanel = MutableStateFlow(false)
    val showAdjustPanel: StateFlow<Boolean> = _showAdjustPanel

    private val PARAM_SCALE = 100f
    // The palette still shows ±100, but the actual image adjustments are scaled down
    // to keep the effect subtle and avoid clipping/colour artifacts.
    private val SATURATION_EFFECT_SCALE = 0.5f
    private val BRIGHTNESS_EFFECT_SCALE = 0.35f

    fun updateSquarePanel(offset: Offset) {
        val clamped = Offset(
            x = offset.x.coerceIn(-1f, 1f),
            y = offset.y.coerceIn(-1f, 1f)
        )
        // Keep the raw offset continuous so values like +99 are possible.
        // The 11x11 dot matrix only provides visual snapping.
        _squarePanelOffset.value = clamped

        // UI labels remain ±100; the actual effect is scaled to a more usable range.
        val saturation = clamped.x * PARAM_SCALE * SATURATION_EFFECT_SCALE
        val brightness = clamped.y * PARAM_SCALE * BRIGHTNESS_EFFECT_SCALE

        _params.update { current ->
            current.copy(
                saturation = saturation.coerceIn(-100f, 100f),
                brightness = brightness.coerceIn(-100f, 100f)
            )
        }
    }

    fun resetSquarePanel() {
        _squarePanelOffset.value = Offset.Zero
        _params.update { it.copy(brightness = 0f, saturation = 0f) }
    }

    fun resetToDefault() {
        _squarePanelOffset.value = Offset.Zero
        _selectedFilter.value = STANDARD_ORIGINAL
        _filterIntensity.value = 100
        _params.value = AdjustmentParams()
    }

    fun selectFilter(filter: FilterPreset) {
        if (_selectedFilter.value.id == filter.id) return
        _selectedFilter.value = filter
        _filterIntensity.value = 100
        if (filter.id == "standard") {
            resetSquarePanel()
        }
    }

    fun setFilterIntensity(intensity: Int) {
        _filterIntensity.value = intensity.coerceIn(0, 100)
    }

    fun toggleAdjustPanel() {
        _showAdjustPanel.value = !_showAdjustPanel.value
    }

    fun showAdjustPanel() {
        _showAdjustPanel.value = true
    }

    fun hideAdjustPanel() {
        _showAdjustPanel.value = false
    }

    fun updateTemperature(value: Float) {
        _params.update { it.copy(temperature = value.coerceIn(-100f, 100f)) }
    }

    fun saturationLabel(): String {
        val value = (_squarePanelOffset.value.x * PARAM_SCALE).roundToInt().coerceIn(-100, 100)
        val sign = if (value >= 0) "+" else ""
        return "饱和度 $sign$value"
    }

    fun brightnessLabel(): String {
        val value = (_squarePanelOffset.value.y * PARAM_SCALE).roundToInt().coerceIn(-100, 100)
        val sign = if (value >= 0) "+" else ""
        return "亮度 $sign$value"
    }

    fun intensityLabel(): String {
        return "强度 ${_filterIntensity.value}"
    }

    fun resolvedParams(): AdjustmentParams {
        // The filter's color character is applied in the shader; these are only panel offsets.
        return _params.value
    }
}
