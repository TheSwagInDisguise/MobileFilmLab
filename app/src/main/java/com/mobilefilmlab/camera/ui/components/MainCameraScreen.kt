package com.mobilefilmlab.camera.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.camera.FlashMode
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.ui.theme.*
import com.mobilefilmlab.camera.ui.viewmodel.CameraMode
import com.mobilefilmlab.camera.ui.viewmodel.CameraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCameraScreen(viewModel: CameraViewModel,modifier: Modifier=Modifier) {
    val context=LocalContext.current
    val mode by viewModel.mode.collectAsState(); val flash by viewModel.flashMode.collectAsState()
    val lens by viewModel.activeLens.collectAsState(); val crop by viewModel.cropFactor.collectAsState()
    val framing by viewModel.framingConfig.collectAsState(); val recipe by viewModel.activeRecipe.collectAsState()
    val recipes by viewModel.allRecipes.collectAsState(); val tone by viewModel.puckTone.collectAsState(); val color by viewModel.puckColor.collectAsState()
    val ev by viewModel.evCompensation.collectAsState(); val iso by viewModel.iso.collectAsState(); val shutter by viewModel.shutterSpeed.collectAsState()
    val kelvin by viewModel.whiteBalanceKelvin.collectAsState(); val focus by viewModel.focusMode.collectAsState()
    val recipeOpen by viewModel.isRecipeSheetOpen.collectAsState(); val settingsOpen by viewModel.isSettingsOpen.collectAsState()
    val diagnosticsOpen by viewModel.isDiagnosticsOpen.collectAsState(); val aboutOpen by viewModel.isAboutOpen.collectAsState()
    val galleryOpen by viewModel.isGalleryOpen.collectAsState(); val busy by viewModel.isCapturing.collectAsState()
    val flashing by viewModel.isShutterFlashing.collectAsState(); val thumbnail by viewModel.lastCapturedThumbnail.collectAsState()
    val notice by viewModel.userNotice.collectAsState(); val settings by viewModel.settings.collectAsState()
    val specs by viewModel.cameraEngine.cameraSpecs.collectAsState(); val streaming by viewModel.cameraEngine.isCameraStreaming.collectAsState()
    val cameraError by viewModel.cameraEngine.cameraError.collectAsState(); val pipeline by viewModel.cameraEngine.pipelineStatus.collectAsState()
    val captureProgress by viewModel.cameraEngine.progress.collectAsState(); val processing by viewModel.processingProgress.collectAsState()
    val photos by viewModel.photos.collectAsState(); val thermal by viewModel.thermalStatus.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState(); val update by viewModel.availableUpdate.collectAsState()
    var previewFrame by remember { mutableStateOf<Bitmap?>(null) }
    var qrRecipe by remember { mutableStateOf<Recipe?>(null) }
    val favourites=remember(recipes) { recipes.filter { it.isFavourite }.take(6) }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(viewModel::exportRecipe) }
    val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> viewModel.importRecipes(uri) } }
    val importQr=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> viewModel.importRecipes(uri,true) } }
    LaunchedEffect(notice) { notice?.let { Toast.makeText(context,it,Toast.LENGTH_LONG).show(); viewModel.userNotice.value=null } }
    Box(modifier.fillMaxSize().background(BgDark).systemBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal=14.dp,vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=viewModel::toggleFlash,enabled=!busy) {
                    val icon=when(flash) { FlashMode.OFF->Icons.Default.FlashOff; FlashMode.AUTO->Icons.Default.FlashAuto; FlashMode.ON->Icons.Default.FlashOn; FlashMode.TORCH->Icons.Default.Bolt }
                    Icon(icon,"Flash: ${flash.name}",tint=if(flash==FlashMode.OFF) TextSecondary else AccentGold)
                }
                AutoProModeToggle(mode,viewModel::setMode)
                IconButton(onClick={ viewModel.isAboutOpen.value=true },enabled=!busy) { Icon(Icons.Default.Info,"About MobileFilmLab",tint=Color.White) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                val width=minOf(maxWidth,maxHeight*.75f)
                Box(Modifier.width(width).height(width/.75f)) {
                    CameraViewfinder(viewModel.cameraEngine,recipe,tone,color,flashing,settings,
                        samplePreview=recipeOpen || favourites.size>1,controlsEnabled=!busy,
                        onPreviewFrame={ previewFrame=it },modifier=Modifier.fillMaxSize())
                    if(!streaming && cameraError!=null) {
                        Column(Modifier.align(Alignment.Center).background(SurfaceDark.copy(alpha=.95f),RoundedCornerShape(14.dp)).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(cameraError!!,color=Color.White,fontSize=12.sp)
                            Row {
                                TextButton(onClick=viewModel::onPermissionGranted) { Text("Retry") }
                                TextButton(onClick={ runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}"))) } }) { Text("App settings") }
                            }
                        }
                    }
                    Column(Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(favourites.size>1) LazyRow(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            items(favourites,key={ it.id }) { fav ->
                                Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha=.55f)).clickable(enabled=!busy) { viewModel.selectRecipe(fav) }.padding(5.dp),verticalAlignment=Alignment.CenterVertically) {
                                    RecipeLiveThumbnail(previewFrame,fav,settings.calibration,Modifier.size(24.dp,30.dp).clip(RoundedCornerShape(5.dp)))
                                    Text(fav.name,fontSize=9.sp,color=if(fav.id==recipe.id) AccentGold else Color.White,modifier=Modifier.padding(horizontal=6.dp))
                                }
                            }
                        }
                        LensSelectorBar(lens,crop,specs.filter { it.isBackFacing }.map { it.lensType }.toSet(),!busy,viewModel::selectLens,viewModel::toggle2xCrop,viewModel::setCrop)
                    }
                }
            }
            HonestCropBadge(framing)
            if(mode==CameraMode.PRO) ManualControlStrip(ev,iso,shutter,kelvin,focus,viewModel::cycleEvCompensation,viewModel::cycleIso,viewModel::cycleShutter,viewModel::cycleWhiteBalance,viewModel::cycleFocus)
            FilmLookPill(recipe,onClick={ if(!busy) viewModel.isRecipeSheetOpen.value=true })
            if(busy) Text(processing.ifBlank { captureProgress.ifBlank { "Capturing…" } },fontSize=11.sp,color=AccentGold)
            Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(SurfaceDark).clickable(enabled=!busy) { viewModel.openGallery() },contentAlignment=Alignment.Center) {
                    if(thumbnail!=null) Image(thumbnail!!.asImageBitmap(),"Open photo gallery",Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    else Icon(Icons.Default.Photo,"Open photo gallery",tint=TextSecondary)
                }
                ShutterButton(onClick=viewModel::triggerShutter,enabled=streaming && !busy,busy=busy)
                IconButton(onClick={ viewModel.isSettingsOpen.value=true },enabled=!busy,modifier=Modifier.size(52.dp).background(SurfaceDark,RoundedCornerShape(15.dp))) { Icon(Icons.Default.Settings,"Camera settings",tint=Color.White) }
            }
        }
        if(recipeOpen) ModalBottomSheet(onDismissRequest={ viewModel.isRecipeSheetOpen.value=false },sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=SurfaceDark,dragHandle=null) {
            RecipeBottomSheet(recipes,recipe,tone,color,previewFrame,settings.calibration,viewModel::selectRecipe,viewModel::toggleFavourite,viewModel::updatePuck,viewModel::resetPuck,
                viewModel::saveAsCustomRecipe,viewModel::deleteRecipe,
                onExport={ export.launch("MobileFilmLab-${recipe.name.replace("[^a-zA-Z0-9]".toRegex(),"").take(40)}.filmrecipe") },
                onImport={ import.launch(arrayOf("application/json","text/plain","application/octet-stream","*/*")) },
                onQr={ qrRecipe=viewModel.tunedRecipe() },onImportQr={ importQr.launch(arrayOf("image/*")) },onDismiss={ viewModel.isRecipeSheetOpen.value=false })
        }
        if(settingsOpen) SettingsDialog(settings,lens,pipeline,thermal,updateStatus,update,viewModel::updateSettings,viewModel::checkUpdates,
            onDiagnostics={ viewModel.isSettingsOpen.value=false; viewModel.isDiagnosticsOpen.value=true },onDismiss={ viewModel.isSettingsOpen.value=false })
        if(diagnosticsOpen) DiagnosticsDialog(viewModel.getDiagnosticsReport()) { viewModel.isDiagnosticsOpen.value=false }
        if(aboutOpen) AboutDialog { viewModel.isAboutOpen.value=false }
        if(galleryOpen) InAppGalleryDialog(photos,viewModel.photoRepository,viewModel::deletePhoto,viewModel::closeGallery)
        qrRecipe?.let { r -> RecipeQrDialog(r,{ viewModel.userNotice.value=it }) { qrRecipe=null } }
    }
}
