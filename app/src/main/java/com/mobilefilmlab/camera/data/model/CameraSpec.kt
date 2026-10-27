package com.mobilefilmlab.camera.data.model

enum class LensType(val label: String, val approxFocalLength35mm: Int) {
    ULTRA_WIDE("Ultra-wide", 15), WIDE("Wide", 23), TELEPHOTO("Telephoto", 70), PERISCOPE("Long telephoto", 230), FRONT("Front", 21)
}

data class CameraSpec(
    val cameraId: String,
    val isBackFacing: Boolean,
    val isLogicalMultiCamera: Boolean,
    val physicalCameraIds: List<String> = emptyList(),
    val focalLengthMm: Float,
    val equivalentFocalLength35mm: Int,
    val sensorWidthMm: Float,
    val sensorHeightMm: Float,
    val apertureFNumber: Float,
    val activeArrayWidth: Int,
    val activeArrayHeight: Int,
    val maxResolutionMp: Float,
    val hasRawSupport: Boolean,
    val minIso: Int = 50,
    val maxIso: Int = 12800,
    val minExposureNs: Long = 100_000L,
    val maxExposureNs: Long = 30_000_000_000L,
    val minEv: Int = -6,
    val maxEv: Int = 6,
    val evStep: Float = 1f / 3f,
    val lensType: LensType,
    val displayName: String,
    val openCameraId: String = cameraId,
    val physicalOutputId: String? = null,
    val hasFlash: Boolean = false,
    val manualSensor: Boolean = false,
    val manualWb: Boolean = false,
    val privateReprocessing: Boolean = false,
    val extensionModes: List<Int> = emptyList(),
    val rawSizes: List<StreamSize> = emptyList(),
    val hasJpegSupport: Boolean = true,
    val rawUnavailableReason: String = "RAW_SENSOR is not advertised by this camera",
    val availableFocalLengthsMm: List<Float> = emptyList()
)
