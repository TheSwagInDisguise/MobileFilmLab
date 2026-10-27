package com.mobilefilmlab.camera.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.importer.RecipeQrCodec
import com.mobilefilmlab.camera.data.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RecipeQrDialog(recipe: Recipe,onNotice: (String)->Unit,onDismiss: ()->Unit) {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val bitmap by produceState<Bitmap?>(null,recipe) { value=withContext(Dispatchers.Default) { runCatching { RecipeQrCodec.bitmap(recipe) }.onFailure { error=it.message }.getOrNull() } }
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if(uri!=null && bitmap!=null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri,"w")?.use { check(bitmap!!.compress(Bitmap.CompressFormat.PNG,100,it)) } ?: error("Could not write image") } }
                .onSuccess { onNotice("QR image exported") }.onFailure { onNotice("Could not export QR image") }
        }
    }
    AlertDialog(onDismissRequest=onDismiss,title={ Text(recipe.name) },text={ Column {
        if(bitmap!=null) Image(bitmap!!.asImageBitmap(),"Shareable recipe QR code",Modifier.fillMaxWidth().aspectRatio(1f))
        else if(error!=null) Text(error!!)
        else CircularProgressIndicator()
        Text("Import this image using Recipes → Import QR image. Includes the active Tone/Warmth settings.",fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
    } },confirmButton={ TextButton(onClick={ save.launch("MobileFilmLab-recipe-QR.png") },enabled=bitmap!=null) { Text("Save PNG") } },dismissButton={ TextButton(onDismiss) { Text("Close") } })
}
