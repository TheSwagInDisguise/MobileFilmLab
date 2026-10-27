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
    val format: CaptureFormat = CaptureFormat.JPEG,
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
    fun load(lens: LensType, cameraId: String? = null): CaptureSettings = CaptureSettings(
        pipeline = enumValue(prefs.getString("pipeline", null), CapturePipeline.STANDARD),
        format = enumValue(prefs.getString("format", null), CaptureFormat.JPEG),
        grid = enumValue(prefs.getString("grid", null), GridMode.THIRDS),
        showLevel = prefs.getBoolean("level", true), grain = prefs.getInt("grain", 0).coerceIn(0, 3),
        haptics = prefs.getBoolean("haptics", true), zsl = prefs.getBoolean("zsl", false),
        outputSize = enumValue(prefs.getString("output", null), OutputSize.NATIVE),
        calibration = LensCalibration(
            gain(lens,cameraId,"r").coerceIn(0.8f, 1.2f),
            gain(lens,cameraId,"g").coerceIn(0.8f, 1.2f),
            gain(lens,cameraId,"b").coerceIn(0.8f, 1.2f)
        )
    )
    private fun prefix(lens: LensType, cameraId: String?): String = if(cameraId==null) lens.name else
        "camera_"+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(cameraId.toByteArray(Charsets.UTF_8))
    private fun gain(lens: LensType, cameraId: String?, channel: String): Float {
        val key="${prefix(lens,cameraId)}_$channel"
        return if(prefs.contains(key)) prefs.getFloat(key,1f) else prefs.getFloat("${lens.name}_$channel",1f)
    }
    fun selectedCameraId(): String? = prefs.getString("selected_camera_id",null)
    fun save(s: CaptureSettings, lens: LensType, cameraId: String? = null) {
        prefs.edit().putString("pipeline", s.pipeline.name).putString("format",s.format.name)
            .putString("selected_camera_id",cameraId ?: prefs.getString("selected_camera_id",null)).putString("grid", s.grid.name)
            .putBoolean("level", s.showLevel).putInt("grain", s.grain).putBoolean("haptics", s.haptics)
            .putBoolean("zsl", s.zsl).putString("output", s.outputSize.name)
            .putFloat("${prefix(lens,cameraId)}_r", s.calibration.red).putFloat("${prefix(lens,cameraId)}_g", s.calibration.green)
            .putFloat("${prefix(lens,cameraId)}_b", s.calibration.blue).apply()
    }
    private inline fun <reified T : Enum<T>> enumValue(s: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == s } ?: default
}
