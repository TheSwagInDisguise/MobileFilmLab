package com.mobilefilmlab.camera.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics as CC
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.data.model.StreamSize
import com.mobilefilmlab.camera.data.model.RawStreamSelector
import kotlin.math.hypot
import kotlin.math.roundToInt

class CameraDiscovery(context: Context) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    fun discoverCameras(): List<CameraSpec> {
        val direct = runCatching { manager.cameraIdList.toList() }.getOrDefault(emptyList())
        val routes = linkedMapOf<String, Pair<String, String?>>()
        direct.forEach { routes[it] = it to null }
        direct.forEach { parent ->
            runCatching { manager.getCameraCharacteristics(parent).physicalCameraIds }.getOrDefault(emptySet()).forEach { id ->
                // IDs not independently openable are routed through a logical parent's OutputConfiguration.
                if (id !in routes) routes[id] = parent to id
            }
        }
        return routes.mapNotNull { (id, route) ->
            runCatching { describe(id, route.first, route.second) }.onFailure {
                Log.w("CameraDiscovery", "Camera $id characteristics unavailable: ${it.javaClass.simpleName}")
            }.getOrNull()
        }
    }
    private fun describe(id: String, openId: String, physicalId: String?): CameraSpec {
        val c = manager.getCameraCharacteristics(id)
        val parent = manager.getCameraCharacteristics(openId)
        val back = c.get(CC.LENS_FACING) == CC.LENS_FACING_BACK
        val f = c.get(CC.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 0f
        val sensor = c.get(CC.SENSOR_INFO_PHYSICAL_SIZE)
        val sw = sensor?.width ?: 0f; val sh = sensor?.height ?: 0f
        val eq = if (sw > 0f && sh > 0f && f > 0f) (f * hypot(36.0,24.0) / hypot(sw.toDouble(),sh.toDouble())).roundToInt() else 0
        val aperture = c.get(CC.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull() ?: 0f
        val active = c.get(CC.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val map = c.get(CC.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = (map?.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty() + map?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()).toMutableList()
        if (Build.VERSION.SDK_INT >= 31) c.get(CC.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)?.let {
            sizes += it.getOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()
            sizes += it.getHighResolutionOutputSizes(ImageFormat.JPEG)?.toList().orEmpty()
        }
        val max = ResolutionSelector.select(sizes.map { ResolutionSelector.Candidate(it.width,it.height) })
        val caps = (c.get(CC.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf())
        val parentCaps = (parent.get(CC.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf())
        val advertisedRaw=if(caps.contains(CC.REQUEST_AVAILABLE_CAPABILITIES_RAW))
            runCatching { map?.getOutputSizes(ImageFormat.RAW_SENSOR)?.map { StreamSize(it.width,it.height) }.orEmpty() }.getOrDefault(emptyList()) else emptyList()
        val rawSizes=RawStreamSelector.candidates(advertisedRaw)
        val rawReason=when {
            !caps.contains(CC.REQUEST_AVAILABLE_CAPABILITIES_RAW) -> "RAW capability is not advertised"
            advertisedRaw.isEmpty() -> "No RAW_SENSOR output sizes are advertised"
            rawSizes.isEmpty() -> "Only RAW streams above the 55MP memory-safety limit are advertised"
            else -> ""
        }
        val iso = c.get(CC.SENSOR_INFO_SENSITIVITY_RANGE)
        val exp = c.get(CC.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val ev = parent.get(CC.CONTROL_AE_COMPENSATION_RANGE)
        val type = when {
            !back -> LensType.FRONT
            eq in 1..18 -> LensType.ULTRA_WIDE
            eq in 19..35 || eq == 0 -> LensType.WIDE
            eq <= 95 -> LensType.TELEPHOTO
            else -> LensType.PERISCOPE
        }
        val modes = if (Build.VERSION.SDK_INT >= 31 && physicalId == null) runCatching {
            manager.getCameraExtensionCharacteristics(openId).supportedExtensions
        }.getOrDefault(emptyList()) else emptyList()
        return CameraSpec(
            cameraId=id, isBackFacing=back, isLogicalMultiCamera=c.physicalCameraIds.isNotEmpty(),
            physicalCameraIds=c.physicalCameraIds.toList(), focalLengthMm=f, equivalentFocalLength35mm=eq,
            sensorWidthMm=sw, sensorHeightMm=sh, apertureFNumber=aperture,
            activeArrayWidth=active?.width() ?: 0, activeArrayHeight=active?.height() ?: 0,
            maxResolutionMp=(max?.area ?: 0L)/1_000_000f,
            hasRawSupport=rawSizes.isNotEmpty(),
            minIso=iso?.lower ?: 50, maxIso=iso?.upper ?: 6400,
            minExposureNs=exp?.lower ?: 100_000L, maxExposureNs=exp?.upper ?: 1_000_000_000L,
            minEv=ev?.lower ?: 0, maxEv=ev?.upper ?: 0, evStep=parent.get(CC.CONTROL_AE_COMPENSATION_STEP)?.toFloat() ?: 0f,
            lensType=type, displayName="${if(c.physicalCameraIds.isNotEmpty()) "Logical auto" else type.label}: ${if(eq>0) "~$eq mm eq." else "$f mm physical"} f/$aperture (ID $id${if (physicalId != null) " via $openId" else ""})",
            openCameraId=openId, physicalOutputId=physicalId, hasFlash=parent.get(CC.FLASH_INFO_AVAILABLE) == true,
            manualSensor=caps.contains(CC.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            manualWb=caps.contains(CC.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING),
            privateReprocessing=parentCaps.contains(CC.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING), extensionModes=modes,
            rawSizes=rawSizes, hasJpegSupport=sizes.isNotEmpty(), rawUnavailableReason=rawReason,
            availableFocalLengthsMm=c.get(CC.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList().orEmpty()
        )
    }
}
