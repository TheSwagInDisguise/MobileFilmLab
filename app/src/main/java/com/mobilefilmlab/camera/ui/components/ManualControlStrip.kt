package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.camera.ProControlMath
import com.mobilefilmlab.camera.data.model.CameraSpec
import com.mobilefilmlab.camera.ui.theme.*

private enum class ProControl(val label: String, val title: String) {
    EV("EV", "Exposure compensation"), ISO("ISO", "Sensor sensitivity"),
    SHUTTER("SEC", "Shutter speed"), WB("WB", "White balance"), FOCUS("FOCUS", "Autofocus mode")
}

/** Selecting a tab only opens its editor. Values change by slider or explicit Auto/mode choice. */
@Composable
fun ManualControlStrip(
    evCompensation: Float, iso: Int?, exposureTimeNs: Long?, kelvin: Int, focusMode: String,
    camera: CameraSpec?, enabled: Boolean,
    onEvChange: (Float)->Unit, onIsoChange: (Int?)->Unit, onShutterChange: (Long?)->Unit,
    onWbChange: (Int)->Unit, onFocusChange: (String)->Unit, onAdjustmentFinished: ()->Unit,
    modifier: Modifier=Modifier
) {
    var active by rememberSaveable(camera?.cameraId) { mutableStateOf(if(iso!=null) ProControl.ISO else ProControl.EV) }
    val manualExposure=iso!=null || exposureTimeNs!=null
    val values=mapOf(
        ProControl.EV to if(manualExposure) "OFF" else ProControlMath.evLabel(evCompensation),
        ProControl.ISO to (iso?.toString() ?: if(exposureTimeNs!=null) "METER" else "AUTO"),
        ProControl.SHUTTER to (exposureTimeNs?.let(ProControlMath::exposureLabel) ?: if(iso!=null) "METER" else "AUTO"),
        ProControl.WB to if(kelvin==0) "AUTO" else "${kelvin}K",
        ProControl.FOCUS to if(camera?.focusModes.isNullOrEmpty()) "N/A" else focusMode
    )
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(SurfaceDark).border(1.dp,BorderDark,RoundedCornerShape(14.dp)).padding(8.dp)) {
        Row(Modifier.fillMaxWidth().selectableGroup(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            ProControl.values().forEach { control ->
                val selected=active==control
                Column(Modifier.weight(1f).heightIn(min=48.dp).clip(RoundedCornerShape(8.dp))
                    .background(if(selected) AccentGold else SurfaceCard)
                    .selectable(selected=selected,enabled=enabled,role=Role.Tab,onClick={ active=control })
                    .semantics(mergeDescendants=true) { contentDescription="${control.title} control" }
                    .padding(horizontal=2.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Text(control.label,fontSize=10.sp,lineHeight=14.sp,fontWeight=FontWeight.Bold,color=if(selected) Color.Black else TextSecondary)
                    Text(values.getValue(control),fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.Bold,color=if(selected) Color.Black else TextPrimary)
                }
            }
        }
        when(active) {
            ProControl.EV -> {
                val supported=ProControlMath.hasEv(camera)
                ProHeader(active.title,if(manualExposure) "Inactive" else "${ProControlMath.evLabel(evCompensation)} EV") {
                    TextButton(onClick={ onEvChange(0f); onAdjustmentFinished() },enabled=enabled && supported && !manualExposure,contentPadding=PaddingValues(horizontal=8.dp)) { Text("Reset") }
                }
                if(supported && camera!=null) {
                    val steps=(camera.maxEv.toLong()-camera.minEv-1).coerceIn(0,200).toInt()
                    Slider(value=ProControlMath.evSteps(evCompensation,camera).toFloat(),
                        onValueChange={ onEvChange(it*camera.evStep) },onValueChangeFinished=onAdjustmentFinished,
                        valueRange=camera.minEv.toFloat()..camera.maxEv.toFloat(),steps=steps,
                        enabled=enabled && !manualExposure,
                        modifier=Modifier.fillMaxWidth().semantics { contentDescription="Exposure compensation"; stateDescription="${ProControlMath.evLabel(evCompensation)} EV" })
                    RangeLabels("${ProControlMath.evLabel(camera.minEv*camera.evStep)} EV","${ProControlMath.evLabel(camera.maxEv*camera.evStep)} EV")
                }
                ProNote(when { !supported -> "EV compensation is not exposed on this camera."
                    manualExposure -> "Set ISO and SEC to Auto to use EV compensation."
                    else -> "Drag to brighten or darken · snaps to Camera2 EV steps." })
            }
            ProControl.ISO -> {
                val range=camera?.let(ProControlMath::isoRange) ?: 100..100
                val available=camera?.manualSensor==true
                val value=(iso ?: 100).coerceIn(range)
                ProHeader(active.title,iso?.toString() ?: if(exposureTimeNs!=null) "Metered" else "Auto") {
                    AutoControl(iso==null,enabled && available,if(exposureTimeNs!=null) "Metered" else "Auto") { onIsoChange(if(it) null else value); onAdjustmentFinished() }
                }
                LogSlider("ISO",value.toLong(),range.first.toLong(),range.last.toLong(),enabled && available && iso!=null,onAdjustmentFinished) { onIsoChange(it.toInt()) }
                RangeLabels("ISO ${range.first}","ISO ${range.last}")
                ProNote(when { !available -> "Manual ISO is not exposed on this camera."
                    iso==null && exposureTimeNs==null -> "Turn Auto off to adjust ISO."
                    exposureTimeNs==null -> "Shutter is metered then held · not ISO-priority auto."
                    iso==null -> "ISO is metered then held · not shutter-priority auto."
                    else -> "Drag to adjust ISO · logarithmic sensitivity scale." })
            }
            ProControl.SHUTTER -> {
                val range=camera?.let(ProControlMath::exposureRange) ?: 8_000_000L..8_000_000L
                val available=camera?.manualSensor==true
                val value=(exposureTimeNs ?: 8_000_000L).coerceIn(range)
                ProHeader(active.title,exposureTimeNs?.let(ProControlMath::exposureLabel) ?: if(iso!=null) "Metered" else "Auto") {
                    AutoControl(exposureTimeNs==null,enabled && available,if(iso!=null) "Metered" else "Auto") { onShutterChange(if(it) null else value); onAdjustmentFinished() }
                }
                LogSlider("Shutter speed",value,range.first,range.last,enabled && available && exposureTimeNs!=null,onAdjustmentFinished,onShutterChange)
                RangeLabels(ProControlMath.exposureLabel(range.first),ProControlMath.exposureLabel(range.last))
                ProNote(when { !available -> "Manual shutter is not exposed on this camera."
                    exposureTimeNs==null && iso==null -> "Turn Auto off to adjust shutter speed."
                    iso==null -> "ISO is metered then held · not shutter-priority auto."
                    exposureTimeNs==null -> "Shutter is metered then held · not ISO-priority auto."
                    else -> "Logarithmic time scale · sensor/frame limits, up to 30 s." })
            }
            ProControl.WB -> {
                val available=camera?.manualWb==true
                ProHeader(active.title,if(kelvin==0) "Auto" else "$kelvin K") {
                    AutoControl(kelvin==0,enabled && available,"Auto") { onWbChange(if(it) 0 else 5600); onAdjustmentFinished() }
                }
                Slider(value=(if(kelvin==0) 5600 else kelvin).toFloat().coerceIn(2500f,9900f),onValueChange={ onWbChange(ProControlMath.kelvin(it.toInt())) },
                    onValueChangeFinished=onAdjustmentFinished,valueRange=2500f..9900f,steps=73,enabled=enabled && available && kelvin!=0,
                    modifier=Modifier.fillMaxWidth().semantics { contentDescription="White balance temperature"; stateDescription=if(kelvin==0) "Auto" else "$kelvin kelvin" })
                RangeLabels("2500 K","9900 K")
                ProNote(when { !available -> "Manual white balance is not exposed on this camera."
                    kelvin==0 -> "Turn Auto off to adjust colour temperature."
                    else -> "Approximate Camera2 RGB-gain temperature · not calibrated Kelvin." })
            }
            ProControl.FOCUS -> {
                ProHeader(active.title,values.getValue(ProControl.FOCUS))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    camera?.focusModes.orEmpty().forEach { focus ->
                        FilterChip(selected=focusMode==focus,onClick={ onFocusChange(focus); onAdjustmentFinished() },enabled=enabled,label={ Text(focus) })
                    }
                }
                ProNote(if(camera?.focusModes.isNullOrEmpty()) "No selectable autofocus modes are advertised on this camera."
                    else "Choose a supported focus mode directly · no value cycling.")
            }
        }
    }
}

@Composable private fun ProHeader(title: String,value: String,action: @Composable RowScope.()->Unit={}) {
    Row(Modifier.fillMaxWidth().heightIn(min=40.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f).padding(end=6.dp),fontSize=13.sp,lineHeight=17.sp,color=TextPrimary)
        Text(value,fontSize=14.sp,lineHeight=18.sp,fontWeight=FontWeight.Bold,color=AccentGold,modifier=Modifier.padding(end=6.dp))
        action()
    }
}
@Composable private fun AutoControl(checked: Boolean,enabled: Boolean,label: String,onChange: (Boolean)->Unit) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Text(label,fontSize=12.sp,color=TextSecondary)
        Switch(checked=checked,onCheckedChange=onChange,enabled=enabled,modifier=Modifier.semantics { contentDescription="$label control" })
    }
}
@Composable private fun LogSlider(label: String,value: Long,min: Long,max: Long,enabled: Boolean,onFinished: ()->Unit,onChange: (Long)->Unit) {
    Slider(value=ProControlMath.logarithmicPosition(value,min,max),onValueChange={ onChange(ProControlMath.logarithmicValue(it,min,max)) },
        onValueChangeFinished=onFinished,enabled=enabled && min<max,
        modifier=Modifier.fillMaxWidth().semantics { contentDescription=label; stateDescription=if(label=="ISO") value.toString() else ProControlMath.exposureLabel(value) })
}
@Composable private fun RangeLabels(left: String,right: String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(left,color=TextSecondary,fontSize=11.sp,lineHeight=15.sp)
        Text(right,color=TextSecondary,fontSize=11.sp,lineHeight=15.sp)
    }
}
@Composable private fun ProNote(note: String) { Text(note,color=TextSecondary,fontSize=12.sp,lineHeight=16.sp,modifier=Modifier.padding(top=4.dp)) }
