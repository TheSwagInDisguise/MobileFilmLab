package com.mobilefilmlab.camera.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.SizeF
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.LensType

class CameraDiscovery(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun discoverCameras(): List<CameraSpec> {
        val list = mutableListOf<CameraSpec>()
        val cameraIds = try {
            cameraManager.cameraIdList
        } catch (e: Exception) {
            emptyArray<String>()
        }

        for (id in cameraIds) {
            try {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
                val isBack = facing == CameraCharacteristics.LENS_FACING_BACK

                // Physical vs Logical
                val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    chars.physicalCameraIds.toList()
                } else {
                    emptyList()
                }
                val isLogical = physicalIds.isNotEmpty()

                // Focal length and 35mm equivalent
                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf(4.5f)
                val focalLength = focalLengths.firstOrNull() ?: 4.5f
                val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(6.4f, 4.8f)

                // 35mm equivalent calculation: 36.0 / sensorWidth * focalLength
                val cropFactor35 = 36.0f / sensorSize.width.coerceAtLeast(1.0f)
                val eq35 = (focalLength * cropFactor35).toInt().coerceAtLeast(12)

                val apertureArray = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES) ?: floatArrayOf(1.8f)
                val aperture = apertureArray.firstOrNull() ?: 1.8f

                val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val activeW = activeArray?.width() ?: 4000
                val activeH = activeArray?.height() ?: 3000

                // Formats and resolution discovery
                val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val outputFormats = streamMap?.outputFormats ?: intArrayOf()
                val hasRaw = outputFormats.contains(ImageFormat.RAW_SENSOR)

                val hiResSizes = streamMap?.getHighResolutionOutputSizes(ImageFormat.JPEG) ?: emptyArray()
                val regularSizes = streamMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
                val maxSizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val maxMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                    (maxMap?.getHighResolutionOutputSizes(ImageFormat.JPEG) ?: emptyArray()) +
                    (maxMap?.getOutputSizes(ImageFormat.JPEG) ?: emptyArray())
                } else {
                    emptyArray()
                }

                val allDiscoveredSizes = (hiResSizes.toList() + regularSizes.toList() + maxSizes.toList())
                val stock28Mp = allDiscoveredSizes.firstOrNull { s ->
                    (s.width == 6120 && s.height == 4596) ||
                    (s.width == 4596 && s.height == 6120) ||
                    (s.width * s.height in 27_000_000..29_500_000)
                }
                val maxOutputSize = stock28Mp ?: allDiscoveredSizes
                    .filter { it.width * it.height <= 55_000_000 }
                    .maxByOrNull { it.width * it.height }

                val maxW = maxOutputSize?.width ?: activeW
                val maxH = maxOutputSize?.height ?: activeH
                val mp = (maxW * maxH).toFloat() / 1_000_000.0f

                // ISO and Exposure
                val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                val minIso = isoRange?.lower ?: 50
                val maxIso = isoRange?.upper ?: 6400

                val expRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                val minExp = expRange?.lower ?: 100_000L
                val maxExp = expRange?.upper ?: 30_000_000_000L

                val evRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                val minEv = evRange?.lower ?: -6
                val maxEv = evRange?.upper ?: 6
                val evStepRational = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                val evStep = evStepRational?.toFloat() ?: 0.333f

                val lensType = classifyLens(isBack, eq35, isLogical)
                val displayName = buildDisplayName(lensType, eq35, aperture, isLogical, id)

                list.add(
                    CameraSpec(
                        cameraId = id,
                        isBackFacing = isBack,
                        isLogicalMultiCamera = isLogical,
                        physicalCameraIds = physicalIds,
                        focalLengthMm = focalLength,
                        equivalentFocalLength35mm = eq35,
                        sensorWidthMm = sensorSize.width,
                        sensorHeightMm = sensorSize.height,
                        apertureFNumber = aperture,
                        activeArrayWidth = activeW,
                        activeArrayHeight = activeH,
                        maxResolutionMp = mp,
                        hasRawSupport = hasRaw,
                        minIso = minIso,
                        maxIso = maxIso,
                        minExposureNs = minExp,
                        maxExposureNs = maxExp,
                        minEv = minEv,
                        maxEv = maxEv,
                        evStep = evStep,
                        lensType = lensType,
                        displayName = displayName
                    )
                )
            } catch (e: Exception) {
                // Skip camera if characteristics fail
            }
        }

        return list
    }

    private fun classifyLens(isBack: Boolean, eq35: Int, isLogical: Boolean): LensType {
        if (!isBack) return LensType.FRONT
        return when {
            eq35 <= 18 -> LensType.ULTRA_WIDE
            eq35 in 19..35 -> LensType.WIDE
            eq35 in 36..90 -> LensType.TELEPHOTO
            else -> LensType.PERISCOPE
        }
    }

    private fun buildDisplayName(lens: LensType, eq35: Int, aperture: Float, isLogical: Boolean, id: String): String {
        val label = when (lens) {
            LensType.ULTRA_WIDE -> "Ultra-wide"
            LensType.WIDE -> "Primary Wide"
            LensType.TELEPHOTO -> "Telephoto 3x"
            LensType.PERISCOPE -> "Periscope 10x"
            LensType.FRONT -> "Front Camera"
        }
        val logTag = if (isLogical) " [Logical Multi-Cam]" else ""
        return "${lens.label} ($eq35 mm f/$aperture)$logTag (ID: $id)"
    }
}
