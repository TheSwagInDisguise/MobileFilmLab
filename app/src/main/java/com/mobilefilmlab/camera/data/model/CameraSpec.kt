package com.mobilefilmlab.camera.data.model

enum class LensType(val label: String, val approxFocalLength35mm: Int) {
    ULTRA_WIDE("UW", 15), WIDE("W", 23), TELEPHOTO("3x", 70), PERISCOPE("10x", 230), FRONT("Selfie", 21)
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
    val extensionModes: List<Int> = emptyList()
)
