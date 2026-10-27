package com.mobilefilmlab.camera.ui.components

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobilefilmlab.camera.gallery.PhotoRepository
import com.mobilefilmlab.camera.ui.theme.AccentGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InAppGalleryDialog(photos: List<PhotoRepository.Photo>,repository: PhotoRepository,onDelete: (PhotoRepository.Photo)->Unit,onDismiss: ()->Unit) {
    val context=LocalContext.current
    val pager=rememberPagerState(pageCount={ photos.size })
    var zoomed by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PhotoRepository.Photo?>(null) }
    LaunchedEffect(pager.currentPage) { zoomed=false }
    val current=photos.getOrNull(pager.currentPage)
    val details by produceState<PhotoRepository.Details?>(null,current?.uri) {
        value=null
        if(current!=null) value=withContext(Dispatchers.IO) { runCatching { repository.details(current) }.getOrNull() }
    }
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                IconButton(onDismiss) { Icon(Icons.Default.Close,"Close gallery",tint=Color.White) }
                Text(if(photos.isEmpty()) "No photos yet" else "${pager.currentPage+1} / ${photos.size}",color=Color.White)
                Row {
                    IconButton(onClick={ current?.let { p ->
                        runCatching {
                            val intent=Intent(Intent.ACTION_SEND).apply { type="image/jpeg"; putExtra(Intent.EXTRA_STREAM,p.uri); clipData=ClipData.newRawUri("Photo",p.uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                            context.startActivity(Intent.createChooser(intent,"Share photo"))
                        }.onFailure { Toast.makeText(context,"No sharing app available",Toast.LENGTH_SHORT).show() }
                    } },enabled=current!=null) { Icon(Icons.Default.Share,"Share photo",tint=Color.White) }
                    IconButton(onClick={ pendingDelete=current },enabled=current!=null) { Icon(Icons.Default.Delete,"Delete photo",tint=Color.White) }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
                if(photos.isEmpty()) Text("Your MobileFilmLab photos appear here after capture.",color=Color.LightGray,modifier=Modifier.padding(30.dp))
                else HorizontalPager(pager,Modifier.fillMaxSize(),userScrollEnabled=!zoomed,key={ photos[it].uri.toString() }) { page ->
                    ZoomablePhoto(photos[page],repository,page==pager.currentPage) { zoomed=it }
                }
            }
            Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(details?.title.orEmpty(),color=AccentGold,fontSize=14.sp)
                Text(details?.size.orEmpty(),color=Color.White,fontSize=12.sp)
                Text(details?.exposure.orEmpty(),color=Color.LightGray,fontSize=11.sp)
                if(details?.processing?.isNotBlank()==true) Text(details!!.processing,color=Color.Gray,fontSize=9.sp,maxLines=3)
                Text("Swipe photos · pinch / double-tap to zoom",color=Color.Gray,fontSize=10.sp,modifier=Modifier.padding(top=8.dp))
            }
        }
    }
    if(pendingDelete!=null) AlertDialog(onDismissRequest={ pendingDelete=null },title={ Text("Delete this photo?") },text={ Text("This removes the saved image from your phone.") },
        confirmButton={ TextButton(onClick={ pendingDelete?.let(onDelete); pendingDelete=null }) { Text("Delete") } },dismissButton={ TextButton(onClick={ pendingDelete=null }) { Text("Cancel") } })
}

@Composable
private fun ZoomablePhoto(photo: PhotoRepository.Photo,repository: PhotoRepository,active: Boolean,onZoom: (Boolean)->Unit) {
    var scale by remember(photo.uri) { mutableFloatStateOf(1f) }
    var offset by remember(photo.uri) { mutableStateOf(Offset.Zero) }
    var error by remember(photo.uri) { mutableStateOf<String?>(null) }
    val full=active && scale>1.6f
    val bitmap by produceState<Bitmap?>(null,photo.uri,full) {
        value=withContext(Dispatchers.IO) { runCatching { repository.load(photo.uri,full) }.onFailure { error="Could not load photo. Try opening it in your system gallery." }.getOrNull() }
    }
    LaunchedEffect(active) { if(!active) { scale=1f; offset=Offset.Zero } else onZoom(scale>1f) }
    LaunchedEffect(scale,active) { if(active) onZoom(scale>1f) }
    BoxWithConstraints(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
        val density=androidx.compose.ui.platform.LocalDensity.current
        val bw=with(density) { maxWidth.toPx() }; val bh=with(density) { maxHeight.toPx() }
        val state=rememberTransformableState { zoom,pan,_ ->
            scale=(scale*zoom).coerceIn(1f,8f)
            val mx=bw*(scale-1)/2; val my=bh*(scale-1)/2
            offset=if(scale==1f) Offset.Zero else Offset((offset.x+pan.x).coerceIn(-mx,mx),(offset.y+pan.y).coerceIn(-my,my))
        }
        Box(Modifier.fillMaxSize().transformable(state).pointerInput(photo.uri) { detectTapGestures(onDoubleTap={ scale=if(scale>1f) 1f else 3f; offset=Offset.Zero }) },contentAlignment=Alignment.Center) {
            if(bitmap!=null) Image(bitmap!!.asImageBitmap(),"Saved photo",Modifier.fillMaxSize().graphicsLayer { scaleX=scale; scaleY=scale; translationX=offset.x; translationY=offset.y },contentScale=ContentScale.Fit)
            else if(error!=null) Text(error!!,color=Color.White,modifier=Modifier.padding(24.dp))
            else CircularProgressIndicator(color=AccentGold)
            if(scale>1f && bitmap!=null) Text("${"%.1f".format(scale)}× · ${bitmap!!.width} × ${bitmap!!.height} decoded",fontSize=10.sp,color=Color.LightGray,modifier=Modifier.align(Alignment.TopCenter).padding(10.dp))
        }
    }
}
