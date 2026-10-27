package com.mobilefilmlab.camera.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.BuildConfig

@Composable
fun AboutDialog(onDismiss: ()->Unit) {
    val context=LocalContext.current
    AlertDialog(onDismissRequest=onDismiss,title={ Text("MobileFilmLab") },text={ Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Alpha test",fontSize=12.sp)
        Text("Photos only · AUTO & PRO · 77 original Recipe Lab presets + 3 independent Camera2 baselines.",fontSize=12.sp)
        Text("Oppo Natural (Camera2) is the default: it preserves the exposed ISP JPEG when no crop or edits are enabled. It is not the stock Oppo app's proprietary AI/Hasselblad pipeline, and no measured sensor calibration is claimed.",fontSize=12.sp)
        Text("Tap the preview to focus/meter. Hold the 2× crop button for 1× / 1.5× / 2× / 3×. The UW/W/3x/10x buttons are lens slots; the telemetry badge reports the detected equivalent focal length, not a guarantee of optical magnification.",fontSize=12.sp)
        Text("Star recipes to show up to six quick-switch favourites. The recipe drawer has live colour thumbnails, Tone/Warmth controls, custom saving, file interchange and QR image import/export.",fontSize=12.sp)
        Text("Experimental fusion, OEM extensions and PRIVATE reprocessing ZSL are capability-gated. Unsupported streams fall back visibly. The square grid is only a guide. Native export never invents pixels; optional enlargement is explicitly interpolation.",fontSize=12.sp)
        Text("Recipe definitions ported from voxivoid/recipe-lab-sony-pmca at aab4b57. GNU GPL-3.0-or-later. Rendering is an approximation, not an exact recreation of camera/film brands. Independently maintained; not endorsed by Oppo, Sony, Fujifilm, Kodak, Leica, Ricoh or Hasselblad.",fontSize=11.sp)
        Text("No analytics or cloud photo uploads. Internet permission is used only for explicit release checks. No beta promotion without device testing and approval.",fontSize=11.sp)
        TextButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/TheSwagInDisguise/MobileFilmLab"))) } }) { Text("Source code, licence & test checklist") }
    } },confirmButton={ TextButton(onDismiss) { Text("Close") } })
}
