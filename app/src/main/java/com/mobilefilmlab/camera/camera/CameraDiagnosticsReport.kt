package com.mobilefilmlab.camera.camera

import android.os.Build
import com.mobilefilmlab.camera.data.model.CameraSpec

object CameraDiagnosticsReport {

    fun generateReport(specs: List<CameraSpec>): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("MobileFilmLab Camera Hardware Diagnostics\n")
        sb.append("Target Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
        sb.append("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        sb.append("Total Exposed Cameras: ${specs.size}\n")
        sb.append("=========================================\n\n")

        val rearCameras = specs.filter { it.isBackFacing }
        val frontCameras = specs.filter { !it.isBackFacing }

        sb.append("--- Rear Camera Array (${rearCameras.size} detected) ---\n")
        rearCameras.forEachIndexed { index, spec ->
            sb.append("\n[Camera ${index + 1}: ${spec.displayName}]\n")
            sb.append("  • Camera ID: ${spec.cameraId}\n")
            sb.append("  • Classification: ${spec.lensType.name} (${spec.lensType.label})\n")
            sb.append("  • 35mm Equivalent: ~${spec.equivalentFocalLength35mm} mm\n")
            sb.append("  • Physical Focal Length: ${spec.focalLengthMm} mm\n")
            sb.append("  • Aperture: f/${spec.apertureFNumber}\n")
            sb.append("  • Sensor Dimensions: ${spec.sensorWidthMm} mm × ${spec.sensorHeightMm} mm\n")
            sb.append("  • Active Sensor Array: ${spec.activeArrayWidth} × ${spec.activeArrayHeight} (${String.format("%.1f", spec.maxResolutionMp)} MP)\n")
            sb.append("  • Logical Multi-Camera: ${if (spec.isLogicalMultiCamera) "Yes (Physical IDs: ${spec.physicalCameraIds.joinToString()})" else "No (Direct Physical Access)"}\n")
            sb.append("  • RAW / DNG Support: ${if (spec.hasRawSupport) "Available (RAW_SENSOR)" else "Not Exposed to 3rd-Party"}\n")
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
        sb.append("Report generated for GPL-3.0 compliance\n")
        sb.append("MobileFilmLab v0.1.0-alpha.1\n")
        return sb.toString()
    }
}
