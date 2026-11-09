package com.mobilefilmlab.camera.data.model

import java.util.Locale

/** Compatibility policy, not measured sensor calibration or proprietary stock-camera tuning. */
enum class DeviceProfile(val label: String, val naturalLabel: String?) {
    GENERIC("Standard Camera2", null),
    GALAXY_Z_FOLD5("Galaxy Z Fold5 · balanced native streams", "Fold5 Natural (Camera2)");

    companion object {
        fun detect(manufacturer: String?, model: String?): DeviceProfile {
            val maker=manufacturer.orEmpty().trim().lowercase(Locale.ROOT)
            val name=model.orEmpty().trim().uppercase(Locale.ROOT)
            return if(maker=="samsung" && (name=="SM-F946" || name.startsWith("SM-F946"))) GALAXY_Z_FOLD5 else GENERIC
        }
    }
}
