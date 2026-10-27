package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.CameraOptions
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.settings.CaptureFormat
import com.mobilefilmlab.camera.settings.CaptureFormatPolicy
import com.mobilefilmlab.camera.ui.theme.TextSecondary

@Composable
fun CaptureFormatChoices(selected: CaptureFormat, camera: CameraSpec?, onSelect: (CaptureFormat) -> Unit) {
    Column {
        CaptureFormat.values().forEach { format ->
            val supported = if(camera==null) format==CaptureFormat.JPEG else CaptureFormatPolicy.supported(format,camera)
            Row(Modifier.fillMaxWidth().clickable(enabled=supported) { onSelect(format) }, verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected==format,onClick={ onSelect(format) },enabled=supported)
                Column(Modifier.weight(1f).padding(vertical=7.dp)) {
                    Text(format.label,fontSize=13.sp,color=if(supported) MaterialTheme.colorScheme.onSurface else TextSecondary)
                    val explanation=when(format) {
                        CaptureFormat.JPEG -> "Camera JPEG, with optional recipe processing"
                        CaptureFormat.RAW_JPEG -> "One exposure · ungraded DNG + optional-look JPEG"
                        CaptureFormat.RAW -> "Sensor DNG only · no still-JPEG stream or saved JPEG"
                    }
                    Text(explanation,fontSize=10.sp,color=TextSecondary)
                    if(!supported) Text(if(format.writesRaw && camera?.hasRawSupport!=true) camera?.rawUnavailableReason ?: "Camera capability unavailable" else "JPEG output not exposed on this route",fontSize=10.sp,color=TextSecondary)
                }
            }
        }
    }
}

@Composable
fun CaptureFormatDialog(selected: CaptureFormat,camera: CameraSpec?,onSelect: (CaptureFormat)->Unit,onDismiss: ()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={ Text("Photo file format") },text={ Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(camera?.let { "${CameraOptions.label(it)} · ID ${it.cameraId}" } ?: "Camera capability unavailable",fontSize=11.sp,color=TextSecondary)
        CaptureFormatChoices(selected,camera,onSelect)
        Text("RAW availability is lens/firmware-dependent. RAW uses standard Camera2, native pixels and uncropped 1× framing; no fusion, OEM HDR/Night or ZSL. Looks, grain and RGB gains apply to JPEG only. A RAW-capable editor may be needed to view DNG files.",fontSize=11.sp,color=TextSecondary)
    } },confirmButton={ TextButton(onDismiss) { Text("Done") } })
}
