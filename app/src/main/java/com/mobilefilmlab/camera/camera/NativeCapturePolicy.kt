package com.mobilefilmlab.camera.camera

import com.mobilefilmlab.camera.data.model.DeviceProfile
import com.mobilefilmlab.camera.settings.CapturePipeline
import com.mobilefilmlab.camera.settings.CaptureResolution
import com.mobilefilmlab.camera.settings.CaptureSettings

/** Ranks only real HAL sizes. Bounded fallbacks never jump to a larger/more expensive buffer. */
object NativeCapturePolicy {
    const val BALANCED_PIXELS=12_600_000L
    const val MAX_PIXELS=55_000_000L

    fun attempts(candidates: List<ResolutionSelector.Candidate>,settings: CaptureSettings,profile: DeviceProfile): List<ResolutionSelector.Candidate> {
        val bounded=settings.pipeline==CapturePipeline.BURST || settings.pipeline.isSmartHdr || settings.zsl ||
            (profile==DeviceProfile.GALAXY_Z_FOLD5 && settings.captureResolution==CaptureResolution.DEVICE_BALANCED)
        val valid=candidates.filter { it.width>0 && it.height>0 && it.area<=MAX_PIXELS && (!bounded || !it.maximumMode) }
            .sortedBy { it.highResolution }
            .distinctBy { Triple(it.width,it.height,it.maximumMode) }
        if(valid.isEmpty()) return emptyList()
        if(bounded) {
            val fitting=valid.filter { it.area<=BALANCED_PIXELS }
            // If no small stream is advertised, use the smallest actual native one; invent no sizes.
            val pool=if(fitting.isEmpty()) listOf(valid.minBy { it.area }) else fitting
            return pool.sortedWith(compareBy<ResolutionSelector.Candidate> { it.highResolution }.thenByDescending { it.area })
        }
        val best=if(settings.captureResolution==CaptureResolution.MAXIMUM_NATIVE) valid.maxBy { it.area }
            else ResolutionSelector.select(valid,MAX_PIXELS) ?: return emptyList()
        return (listOf(best)+valid.filter { it!=best && !it.maximumMode && it.area<=best.area }.sortedByDescending { it.area })
            .distinctBy { Triple(it.width,it.height,it.maximumMode) }
    }
}
