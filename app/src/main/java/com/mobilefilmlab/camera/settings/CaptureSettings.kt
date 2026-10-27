package com.mobilefilmlab.camera.settings

import android.content.Context
import com.mobilefilmlab.camera.data.model.LensType

enum class CapturePipeline(val label: String) {
    STANDARD("Camera2 natural"), BURST("3-frame fusion · experimental"),
    OEM_HDR("OEM HDR · if available"), OEM_NIGHT("OEM Night · if available")
}
enum class GridMode(val label: String) {
    OFF("Off"), THIRDS("Rule of thirds"), GOLDEN("Golden ratio"), SQUARE("Square framing guide")
}
enum class OutputSize(val label: String) {
    NATIVE("Native pixels (recommended)"), UPSCALE_28("~28.1MP interpolated export (4:3)")
}
data class LensCalibration(val red: Float = 1f, val green: Float = 1f, val blue: Float = 1f) {
    val isIdentity: Boolean get() = red == 1f && green == 1f && blue == 1f
}
data class CaptureSettings(
    val pipeline: CapturePipeline = CapturePipeline.STANDARD,
    val grid: GridMode = GridMode.THIRDS,
    val showLevel: Boolean = true,
    val grain: Int = 0,
    val haptics: Boolean = true,
    val zsl: Boolean = false,
    val outputSize: OutputSize = OutputSize.NATIVE,
    val compare: Boolean = false,
    val calibration: LensCalibration = LensCalibration()
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("capture_settings", Context.MODE_PRIVATE)
    fun load(lens: LensType): CaptureSettings = CaptureSettings(
        pipeline = enumValue(prefs.getString("pipeline", null), CapturePipeline.STANDARD),
        grid = enumValue(prefs.getString("grid", null), GridMode.THIRDS),
        showLevel = prefs.getBoolean("level", true), grain = prefs.getInt("grain", 0).coerceIn(0, 3),
        haptics = prefs.getBoolean("haptics", true), zsl = prefs.getBoolean("zsl", false),
        outputSize = enumValue(prefs.getString("output", null), OutputSize.NATIVE),
        calibration = LensCalibration(
            prefs.getFloat("${lens.name}_r", 1f).coerceIn(0.8f, 1.2f),
            prefs.getFloat("${lens.name}_g", 1f).coerceIn(0.8f, 1.2f),
            prefs.getFloat("${lens.name}_b", 1f).coerceIn(0.8f, 1.2f)
        )
    )
    fun save(s: CaptureSettings, lens: LensType) {
        prefs.edit().putString("pipeline", s.pipeline.name).putString("grid", s.grid.name)
            .putBoolean("level", s.showLevel).putInt("grain", s.grain).putBoolean("haptics", s.haptics)
            .putBoolean("zsl", s.zsl).putString("output", s.outputSize.name)
            .putFloat("${lens.name}_r", s.calibration.red).putFloat("${lens.name}_g", s.calibration.green)
            .putFloat("${lens.name}_b", s.calibration.blue).apply()
    }
    private inline fun <reified T : Enum<T>> enumValue(s: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == s } ?: default
}
