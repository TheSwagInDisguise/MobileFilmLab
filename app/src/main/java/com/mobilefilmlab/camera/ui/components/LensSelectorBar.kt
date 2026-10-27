package com.mobilefilmlab.camera.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.CameraOptions
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.settings.CaptureFormat
import com.mobilefilmlab.camera.settings.CaptureFormatPolicy
import com.mobilefilmlab.camera.ui.theme.AccentGold

/** No fixed slots: every chip is an actual reported photo-capable rear Camera2 route. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LensSelectorBar(activeCameraId: String?, cropFactor: Float, cameras: List<CameraSpec>, format: CaptureFormat, enabled: Boolean,
                    onCameraSelected: (String) -> Unit, onToggle2xCrop: () -> Unit, onCrop: (Float) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val desiredWidth = (cameras.map { if (it.isLogicalMultiCamera) 92 else 62 }.sum() + 64).dp
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Row(Modifier.width(minOf(maxWidth, desiredWidth)).background(Color.Black.copy(alpha = .58f), RoundedCornerShape(27.dp)).padding(5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(cameras, key = { it.cameraId }) { spec ->
                    val selected = spec.cameraId == activeCameraId
                    val supported = CaptureFormatPolicy.supported(format, spec)
                    val duplicate = cameras.count { CameraOptions.label(it) == CameraOptions.label(spec) } > 1
                    Column(Modifier.width(if (spec.isLogicalMultiCamera) 86.dp else 57.dp).height(44.dp)
                        .background(if (selected) Color.White else Color.White.copy(alpha = .10f), RoundedCornerShape(20.dp))
                        .semantics { contentDescription = CameraOptions.description(spec) }
                        .combinedClickable(enabled = enabled, onClick = { onCameraSelected(spec.cameraId) },
                            onLongClick = { Toast.makeText(context, CameraOptions.description(spec), Toast.LENGTH_LONG).show() }),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(CameraOptions.label(spec), fontSize = 10.sp, maxLines = 1,
                            color = if (selected) Color.Black else if (supported) Color.White else Color.Gray)
                        if (duplicate) Text("ID ${spec.cameraId}", fontSize = 8.sp, maxLines = 1, color = if (selected) Color.DarkGray else Color.LightGray)
                    }
                }
            }
            Box {
                Box(Modifier.height(44.dp).width(48.dp)
                    .background(if (cropFactor > 1f) AccentGold else Color.White.copy(alpha = .10f), RoundedCornerShape(15.dp))
                    .combinedClickable(enabled = enabled, onClick = {
                        if (CaptureFormatPolicy.allowsCrop(format)) onToggle2xCrop()
                        else Toast.makeText(context, "RAW modes keep 1× uncropped sensor framing", Toast.LENGTH_SHORT).show()
                    }, onLongClick = { if (CaptureFormatPolicy.allowsCrop(format)) menu = true })
                    .semantics { contentDescription = if (format.writesRaw) "RAW framing: 1×, uncropped" else "Digital crop; hold for all framing levels" },
                    contentAlignment = Alignment.Center) {
                    Text(if (format.writesRaw) "1×" else if (cropFactor == 1f || cropFactor == 2f) "2×" else "${cropFactor}×",
                        fontSize = 12.sp, color = if (cropFactor > 1f) Color.Black else Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf(1f, 1.5f, 2f, 3f).forEach { factor ->
                        DropdownMenuItem(text = { Text("${factor}× crop${if (factor == 1f) " · full frame" else " · native pixels"}") },
                            onClick = { onCrop(factor); menu = false })
                    }
                }
            }
        }
    }
}
