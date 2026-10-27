package com.mobilefilmlab.camera.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobilefilmlab.camera.BuildConfig
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.settings.*
import com.mobilefilmlab.camera.updates.ReleaseChecker
import com.mobilefilmlab.camera.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    settings: CaptureSettings,lens: LensType,status: String,thermal: String,
    updateStatus: String,update: ReleaseChecker.Release?,
    onChange: (CaptureSettings)->Unit,onCheckUpdates: ()->Unit,onDiagnostics: ()->Unit,onDismiss: ()->Unit
) {
    val context=LocalContext.current
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxWidth().widthIn(max=540.dp).padding(16.dp)
            .heightIn(max=(LocalConfiguration.current.screenHeightDp*0.88f).dp)
            .background(SurfaceDark,RoundedCornerShape(22.dp)).padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Camera settings",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White)
            Text("${BuildConfig.VERSION_NAME} · PUBLIC BETA",color=AccentGold,fontSize=11.sp)
            Text("Default: Oppo Natural (Camera2), without an added film grade. This is not the proprietary stock camera pipeline.",fontSize=12.sp,color=TextSecondary)
            SectionTitle("Capture processing")
            CapturePipeline.values().forEach { p ->
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(selected=settings.pipeline==p,onClick={ onChange(settings.copy(pipeline=p)) })
                    Text(p.label,color=Color.White,fontSize=12.sp)
                }
            }
            Text("Active: $status\nThermal: $thermal",fontSize=11.sp,color=AccentGold)
            Text("OEM modes require firmware support and control their own exposure/flash. Fusion is JPEG-domain, translation-only, capped at ~12MP and can fall back for motion. It is not Oppo AI processing.",fontSize=11.sp,color=TextSecondary)
            SettingToggle("Reprocessing ZSL (experimental)",settings.zsl,{ onChange(settings.copy(zsl=it)) },"PRIVATE input ring, where the device supports a reprocessable session. No guaranteed zero-lag fallback.")
            SectionTitle("Output pixels")
            OutputSize.values().forEach { o ->
                Row(verticalAlignment=Alignment.CenterVertically) { RadioButton(settings.outputSize==o,{ onChange(settings.copy(outputSize=o)) }); Text(o.label,fontSize=12.sp) }
            }
            Text("Native keeps the actual capture/crop dimensions. Enlarged export is interpolation, not additional captured detail. Square is a guide only; it does not crop the saved photo.",fontSize=11.sp,color=TextSecondary)
            SectionTitle("Composition")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                GridMode.values().forEach { g -> FilterChip(settings.grid==g,{ onChange(settings.copy(grid=g)) },label={ Text(g.label,fontSize=10.sp) }) }
            }
            SettingToggle("Sensor-driven horizon",settings.showLevel,{ onChange(settings.copy(showLevel=it)) },"Gold when within 1°. Hidden when the phone is flat or no sensor is available.")
            SettingToggle("Live before / after comparison",settings.compare,{ onChange(settings.copy(compare=it)) },"Drag the divider. Colour preview only; final grain, sharpening and OEM processing can differ.")
            SectionTitle("Optional film grain")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Off","Fine","Medium","Coarse").forEachIndexed { i,label -> FilterChip(settings.grain==i,{ onChange(settings.copy(grain=i)) },label={ Text(label) }) }
            }
            Text("Procedural luminance grain; no grain is added to any profile unless you enable it.",fontSize=11.sp,color=TextSecondary)
            SettingToggle("Haptic feedback",settings.haptics,{ onChange(settings.copy(haptics=it)) })
            SectionTitle("${lens.label} lens matching")
            Text("User calibration controls, not a measured factory profile. Leave all gains at 1.00 unless matching a neutral chart in controlled light. Saved independently for each lens.",fontSize=11.sp,color=TextSecondary)
            GainSlider("Red",settings.calibration.red) { onChange(settings.copy(calibration=settings.calibration.copy(red=it))) }
            GainSlider("Green",settings.calibration.green) { onChange(settings.copy(calibration=settings.calibration.copy(green=it))) }
            GainSlider("Blue",settings.calibration.blue) { onChange(settings.copy(calibration=settings.calibration.copy(blue=it))) }
            OutlinedButton(onClick={ onChange(settings.copy(calibration=LensCalibration())) }) { Text("Reset this lens") }
            SectionTitle("Diagnostics & releases")
            OutlinedButton(onClick=onDiagnostics,modifier=Modifier.fillMaxWidth()) { Text("Camera capability report") }
            Text(updateStatus,fontSize=11.sp,color=TextSecondary)
            Button(onClick=onCheckUpdates,enabled=updateStatus!="Checking GitHub releases…",modifier=Modifier.fillMaxWidth()) { Text("Check ${ReleaseChecker.channelLabel} updates") }
            if(update!=null) OutlinedButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(update.page))) } },modifier=Modifier.fillMaxWidth()) { Text("View ${update.tag} release") }
            OutlinedButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(ReleaseChecker.RELEASES))) } },modifier=Modifier.fillMaxWidth()) { Text("Open releases page") }
            Text("Update checks contact GitHub only when requested. No photo uploads or analytics. Downloads and installation stay under your control. This beta channel offers newer beta and stable releases, not alpha or release-candidate builds.",fontSize=10.sp,color=TextSecondary)
            Button(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}
@Composable private fun SectionTitle(s: String) { Text(s,fontWeight=FontWeight.Bold,color=Color.White,fontSize=14.sp,modifier=Modifier.padding(top=8.dp)) }
@Composable private fun SettingToggle(label: String,checked: Boolean,onChecked: (Boolean)->Unit,note: String="") {
    Column {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(label,Modifier.weight(1f),fontSize=12.sp); Switch(checked,onChecked) }
        if(note.isNotBlank()) Text(note,color=TextSecondary,fontSize=10.sp)
    }
}
@Composable private fun GainSlider(label: String,value: Float,onChange: (Float)->Unit) {
    Row(verticalAlignment=Alignment.CenterVertically) { Text("$label ${"%.2f".format(value)}",fontSize=11.sp,modifier=Modifier.width(75.dp)); Slider(value,onChange,modifier=Modifier.weight(1f),valueRange=.8f..1.2f,steps=39) }
}
