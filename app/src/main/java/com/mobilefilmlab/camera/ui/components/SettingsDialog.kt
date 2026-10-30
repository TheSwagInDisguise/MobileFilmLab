package com.mobilefilmlab.camera.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobilefilmlab.camera.BuildConfig
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.data.model.CameraOptions
import com.mobilefilmlab.camera.settings.*
import com.mobilefilmlab.camera.updates.ReleaseChecker
import com.mobilefilmlab.camera.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    settings: CaptureSettings,camera: CameraSpec?,cameraCount: Int,status: String,thermal: String,
    updateStatus: String,update: ReleaseChecker.Release?,
    onChange: (CaptureSettings)->Unit,onCheckUpdates: ()->Unit,onDiagnostics: ()->Unit,onDismiss: ()->Unit
) {
    val context=LocalContext.current
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(
            modifier=Modifier.padding(16.dp).widthIn(max=560.dp).fillMaxWidth()
                .heightIn(max=(LocalConfiguration.current.screenHeightDp*0.88f).dp),
            shape=RoundedCornerShape(22.dp),color=SurfaceDark,contentColor=TextPrimary
        ) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Camera settings",fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Bold)
                Text("${BuildConfig.VERSION_NAME} · PUBLIC BETA",color=AccentGold,fontSize=12.sp)
                HorizontalDivider(color=BorderDark)
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    SettingsNote("Default: Oppo Natural (Camera2), without an added film grade. This is not the proprietary stock camera pipeline.")
                    SectionTitle("Photo file format")
                    CaptureFormatChoices(settings.format,camera) { onChange(settings.copy(format=it)) }
                    SettingsNote("RAW is a real DNG from RAW_SENSOR, ungraded and uncropped. RAW + JPEG uses the same exposure; looks affect only its JPEG. RAW-only does not request a still JPEG. RAW modes lock native 1× framing and standard Camera2; no fusion, OEM HDR/Night, ZSL or enlargement.")
                    SectionTitle("Detected cameras · $cameraCount photo routes")
                    SettingsNote("${camera?.let { CameraOptions.description(it) } ?: "No rear camera available"}\nLabels come from Camera2 focal/sensor metadata, not fixed lens slots. Logical Auto routes are not extra physical lenses. Hold a lens chip for details. Firmware can hide physical cameras.")
                    SectionTitle("Capture processing")
                    CapturePipeline.values().forEach { p ->
                        val enabled=CaptureFormatPolicy.allowsPipeline(settings.format,p)
                        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=settings.pipeline==p,enabled=enabled,role=Role.RadioButton) { onChange(settings.copy(pipeline=p)) },verticalAlignment=Alignment.CenterVertically) {
                            RadioButton(selected=settings.pipeline==p,onClick=null,enabled=enabled)
                            Text(p.label,color=if(enabled) TextPrimary else TextSecondary,fontSize=14.sp,lineHeight=20.sp,modifier=Modifier.padding(start=12.dp,top=10.dp,bottom=10.dp).weight(1f))
                        }
                    }
                    Text("Active: $status\nThermal: $thermal",fontSize=13.sp,lineHeight=18.sp,color=AccentGold)
                    SettingsNote("OEM modes require firmware support and control their own exposure/flash. Fusion is JPEG-domain, translation-only, capped at ~12MP and can fall back for motion. It is not Oppo AI processing.")
                    SettingToggle("Reprocessing ZSL (experimental)",settings.zsl,{ onChange(settings.copy(zsl=it)) },"PRIVATE input ring, where the device supports a reprocessable session. No guaranteed zero-lag fallback.",enabled=!settings.format.writesRaw)
                    SectionTitle("Output pixels")
                    OutputSize.values().forEach { o ->
                        val enabled=!settings.format.writesRaw || o==OutputSize.NATIVE
                        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).selectable(selected=settings.outputSize==o,enabled=enabled,role=Role.RadioButton) { onChange(settings.copy(outputSize=o)) },verticalAlignment=Alignment.CenterVertically) {
                            RadioButton(settings.outputSize==o,onClick=null,enabled=enabled)
                            Text(o.label,fontSize=14.sp,lineHeight=20.sp,color=if(enabled) TextPrimary else TextSecondary,modifier=Modifier.padding(start=12.dp,top=10.dp,bottom=10.dp).weight(1f))
                        }
                    }
                    SettingsNote("Native keeps the actual capture/crop dimensions. Enlarged export is interpolation, not additional captured detail. Square is a guide only; it does not crop the saved photo.")
                    SectionTitle("Composition")
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        GridMode.values().forEach { g -> FilterChip(settings.grid==g,{ onChange(settings.copy(grid=g)) },label={ Text(g.label,fontSize=13.sp) }) }
                    }
                    SettingToggle("Sensor-driven horizon",settings.showLevel,{ onChange(settings.copy(showLevel=it)) },"Gold when within 1°. Hidden when the phone is flat or no sensor is available.")
                    SettingToggle("Live before / after comparison",settings.compare,{ onChange(settings.copy(compare=it)) },"Drag the divider. Colour preview only; final grain, sharpening and OEM processing can differ.",enabled=settings.format.writesJpeg)
                    SectionTitle("Optional JPEG film grain")
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf("Off","Fine","Medium","Coarse").forEachIndexed { i,label -> FilterChip(settings.grain==i,{ onChange(settings.copy(grain=i)) },label={ Text(label) },enabled=settings.format.writesJpeg) }
                    }
                    SettingsNote("Procedural luminance grain; no grain is added to any profile unless you enable it.")
                    SettingToggle("Haptic feedback",settings.haptics,{ onChange(settings.copy(haptics=it)) })
                    SectionTitle("${camera?.let { CameraOptions.label(it) } ?: "Camera"} · JPEG lens matching")
                    SettingsNote("User calibration controls, not a measured factory profile. Leave all gains at 1.00 unless matching a neutral chart in controlled light. Saved independently for each camera ID; existing category gains are migrated when first used. DNG sensor data is never changed by these sliders.")
                    GainSlider("Red",settings.calibration.red) { onChange(settings.copy(calibration=settings.calibration.copy(red=it))) }
                    GainSlider("Green",settings.calibration.green) { onChange(settings.copy(calibration=settings.calibration.copy(green=it))) }
                    GainSlider("Blue",settings.calibration.blue) { onChange(settings.copy(calibration=settings.calibration.copy(blue=it))) }
                    OutlinedButton(onClick={ onChange(settings.copy(calibration=LensCalibration())) }) { Text("Reset this lens") }
                    SectionTitle("Diagnostics & releases")
                    OutlinedButton(onClick=onDiagnostics,modifier=Modifier.fillMaxWidth()) { Text("Camera capability report") }
                    SettingsNote(updateStatus)
                    Button(onClick=onCheckUpdates,enabled=updateStatus!="Checking GitHub releases…",modifier=Modifier.fillMaxWidth()) { Text("Check ${ReleaseChecker.channelLabel} updates") }
                    if(update!=null) OutlinedButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(update.page))) } },modifier=Modifier.fillMaxWidth()) { Text("View ${update.tag} release") }
                    OutlinedButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(ReleaseChecker.RELEASES))) } },modifier=Modifier.fillMaxWidth()) { Text("Open releases page") }
                    SettingsNote("Update checks contact GitHub only when requested. No photo uploads or analytics. Downloads and installation stay under your control. This beta channel offers newer beta and stable releases, not alpha or release-candidate builds.")
                }
                HorizontalDivider(color=BorderDark)
                Button(onClick=onDismiss,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Done") }
            }
        }
    }
}

@Composable private fun SectionTitle(s: String) {
    Text(s,fontWeight=FontWeight.Bold,color=TextPrimary,fontSize=16.sp,lineHeight=22.sp,modifier=Modifier.padding(top=8.dp))
}
@Composable private fun SettingsNote(s: String) { Text(s,color=TextSecondary,fontSize=13.sp,lineHeight=18.sp) }
@Composable private fun SettingToggle(label: String,checked: Boolean,onChecked: (Boolean)->Unit,note: String="",enabled: Boolean=true) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(label,Modifier.weight(1f).padding(end=12.dp),fontSize=14.sp,lineHeight=20.sp)
            Switch(checked,onChecked,enabled=enabled,modifier=Modifier.semantics { contentDescription=label })
        }
        if(note.isNotBlank()) SettingsNote(note)
    }
}
@Composable private fun GainSlider(label: String,value: Float,onChange: (Float)->Unit) {
    Column {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(label,fontSize=14.sp)
            Text("%.2f".format(value),fontSize=14.sp,color=AccentGold)
        }
        Slider(value,onChange,modifier=Modifier.fillMaxWidth().semantics { contentDescription="$label JPEG lens matching gain" },valueRange=.8f..1.2f,steps=39)
    }
}
