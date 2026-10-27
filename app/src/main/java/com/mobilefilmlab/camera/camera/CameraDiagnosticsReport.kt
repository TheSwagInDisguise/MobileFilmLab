package com.mobilefilmlab.camera.camera

import android.os.Build
import com.mobilefilmlab.camera.BuildConfig
import com.mobilefilmlab.camera.data.model.CameraSpec

object CameraDiagnosticsReport {

    fun generateReport(specs: List<CameraSpec>): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("MobileFilmLab Camera Hardware Diagnostics\n")
        sb.append("Target Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
        sb.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        sb.append("Total Exposed Camera2 IDs/routes: ${specs.size} (logical routes are not extra physical lenses)\n")
        sb.append("=========================================\n\n")

        val rearCameras = specs.filter { it.isBackFacing }
        val frontCameras = specs.filter { !it.isBackFacing }

        sb.append("--- Rear Camera2 routes (${rearCameras.size} exposed, including logical routes) ---\n")
        rearCameras.forEachIndexed { index, spec ->
            sb.append("\n[Camera ${index + 1}: ${spec.displayName}]\n")
            sb.append("  • Camera ID: ${spec.cameraId}\n")
            sb.append("  • Detected focal lengths: ${spec.availableFocalLengthsMm} mm; JPEG output: ${spec.hasJpegSupport}\n")
            sb.append("  • Classification: ${spec.lensType.name} (${spec.lensType.label})\n")
            sb.append("  • 35mm Equivalent: ~${spec.equivalentFocalLength35mm} mm\n")
            sb.append("  • Physical Focal Length: ${spec.focalLengthMm} mm\n")
            sb.append("  • Aperture: f/${spec.apertureFNumber}\n")
            sb.append("  • Sensor Dimensions: ${spec.sensorWidthMm} mm × ${spec.sensorHeightMm} mm\n")
            sb.append("  • Active sensor array: ${spec.activeArrayWidth} × ${spec.activeArrayHeight}; preferred exposed JPEG ~${String.format("%.1f", spec.maxResolutionMp)} MP\n")
            sb.append("  • Logical Multi-Camera: ${if (spec.isLogicalMultiCamera) "Yes (Physical IDs: ${spec.physicalCameraIds.joinToString()})" else "No (Direct Physical Access)"}\n")
            sb.append("  • Open ID: ${spec.openCameraId}; physical output ID: ${spec.physicalOutputId ?: "direct"}\n")
            sb.append("  • OEM extension IDs: ${spec.extensionModes}; PRIVATE reprocessing: ${spec.privateReprocessing}\n")
            sb.append("  • Manual exposure: ${spec.manualSensor}; manual WB: ${spec.manualWb}; flash: ${spec.hasFlash}\n")
            sb.append("  • RAW / DNG advertised: ${spec.hasRawSupport}; safe native sizes: ${spec.rawSizes}; ${spec.rawUnavailableReason}\n")
            sb.append("    Actual preview/RAW/JPEG combination still needs a successful session and DNG capture on this firmware.\n")
            sb.append("  • Sensitivity Range: ISO ${spec.minIso} .. ISO ${spec.maxIso}\n")
            sb.append("  • Shutter Range: 1/${(1_000_000_000L / spec.minExposureNs.coerceAtLeast(1L))}s .. ${(spec.maxExposureNs / 1_000_000_000L)}s\n")
            sb.append("  • EV Bias: ${spec.minEv * spec.evStep} EV .. +${spec.maxEv * spec.evStep} EV (step: ${spec.evStep})\n")
        }

        if (frontCameras.isNotEmpty()) {
            sb.append("\n--- Front Camera Array (${frontCameras.size} detected) ---\n")
            frontCameras.forEach { spec ->
                sb.append("  • ID: ${spec.cameraId} - ${spec.displayName} (${String.format("%.1f", spec.maxResolutionMp)} MP)\n")
            }
        }

        sb.append("\n=========================================\n")
        sb.append("Report generated for device testing (no unique identifiers)\n")
        sb.append("MobileFilmLab ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        return sb.toString()
    }
}
