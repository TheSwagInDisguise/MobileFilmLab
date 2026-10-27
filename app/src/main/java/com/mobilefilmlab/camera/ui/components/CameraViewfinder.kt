package com.mobilefilmlab.camera.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.ColorMatrixColorFilter
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mobilefilmlab.camera.camera.CameraEngine
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.settings.CaptureSettings
import com.mobilefilmlab.camera.settings.GridMode
import com.mobilefilmlab.camera.ui.theme.AccentGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

private data class Tilt(val degrees: Float, val valid: Boolean)

@Composable
private fun rememberTilt(enabled: Boolean): Tilt {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var tilt by remember { mutableStateOf(Tilt(0f,false)) }
    DisposableEffect(enabled,lifecycle) {
        val manager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor=manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var filtered=0f; var first=true
        val listener=object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x=event.values[0]; val y=event.values[1]
                val valid=hypot(x,y)>3f
                val angle=Math.toDegrees(atan2(-x.toDouble(),y.toDouble())).toFloat()
                filtered=if(first || abs(angle-filtered)>90f) angle else filtered*0.84f+angle*0.16f
                first=false
                tilt=Tilt(filtered,valid)
            }
            override fun onAccuracyChanged(sensor: Sensor?,accuracy: Int) {}
        }
        fun register() { if(enabled && sensor!=null) manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI) }
        val observer=LifecycleEventObserver { _,event ->
            when(event) { Lifecycle.Event.ON_RESUME -> register(); Lifecycle.Event.ON_PAUSE -> manager.unregisterListener(listener); else -> Unit }
        }
        lifecycle.addObserver(observer)
        if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) register()
        onDispose { manager.unregisterListener(listener); lifecycle.removeObserver(observer) }
    }
    return tilt
}

@Composable
fun CameraViewfinder(
    cameraEngine: CameraEngine,
    activeRecipe: Recipe,
    puckTone: Float,
    puckColor: Float,
    isShutterFlashing: Boolean,
    settings: CaptureSettings,
    samplePreview: Boolean,
    controlsEnabled: Boolean,
    onPreviewFrame: (android.graphics.Bitmap) -> Unit,
    modifier: Modifier=Modifier
) {
    val spec by cameraEngine.activeCameraSpec.collectAsState()
    val streaming by cameraEngine.isCameraStreaming.collectAsState()
    var texture by remember { mutableStateOf<TextureView?>(null) }
    var rawFrame by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var split by remember { mutableFloatStateOf(0.5f) }
    val tilt=rememberTilt(settings.showLevel && streaming)
    val onFrame by rememberUpdatedState(onPreviewFrame)
    val currentRecipe=activeRecipe.copy(puckTone=puckTone,puckColor=puckColor)
    val filter=remember(currentRecipe,settings.calibration) { ColorMatrixColorFilter(ImageProcessor.previewMatrix(currentRecipe,settings.calibration)) }
    LaunchedEffect(focusPoint) { if(focusPoint!=null) { delay(1800); focusPoint=null } }
    LaunchedEffect(texture,streaming,samplePreview,settings.compare) {
        while(isActive && streaming && (samplePreview || settings.compare)) {
            val t=texture
            if(t!=null && t.isAvailable && t.width>0 && t.height>0) {
                val long=if(settings.compare) 960 else 240
                val ratio=long.toFloat()/maxOf(t.width,t.height)
                runCatching { t.getBitmap((t.width*ratio).toInt().coerceAtLeast(1),(t.height*ratio).toInt().coerceAtLeast(1)) }.getOrNull()?.let { b ->
                    // Texture contents are sampled before the view's layer paint; tiles use the same matrix as the save path.
                    if(settings.compare) rawFrame=b
                    onFrame(b)
                }
            }
            delay(if(settings.compare) 180 else 850)
        }
        if(!settings.compare) rawFrame=null
    }
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color.Black).border(1.dp,Color(0xFF2A2E3D),RoundedCornerShape(18.dp))) {
        AndroidView(
            modifier=Modifier.fillMaxSize(),
            factory={ context ->
                object : TextureView(context) {
                    var surface: Surface?=null
                    var sizeInUse=Size(0,0)
                    fun bind(w: Int,h: Int) {
                        val st=surfaceTexture ?: return
                        val size=cameraEngine.getOptimalPreviewSize(w,h)
                        st.setDefaultBufferSize(size.width,size.height)
                        configureTransform(this,w,h,size,cameraEngine.getSensorOrientation())
                        if(surface==null) surface=Surface(st)
                        sizeInUse=size
                        cameraEngine.bindPreviewSurface(surface!!,size.width,size.height)
                    }
                    init {
                        layoutParams=ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
                        surfaceTextureListener=object : SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture,w: Int,h: Int) { bind(w,h) }
                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture,w: Int,h: Int) { bind(w,h) }
                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                cameraEngine.unbindPreviewSurface(); surface?.release(); surface=null; return true
                            }
                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                }.also { texture=it }
            },
            update={ view ->
                // Hardware TextureView layer paint implements a real colour transform, including true monochrome.
                view.setLayerPaint(Paint().apply { colorFilter=filter })
                val size=cameraEngine.getOptimalPreviewSize(view.width,view.height)
                if(view.isAvailable) {
                    view.surfaceTexture?.setDefaultBufferSize(size.width,size.height)
                    configureTransform(view,view.width,view.height,size,cameraEngine.getSensorOrientation())
                }
                // Reading spec causes this update to run for lens changes, retaining the proven native surface path.
                spec?.cameraId
            }
        )
        if(settings.compare && rawFrame!=null) {
            Image(rawFrame!!.asImageBitmap(),"Ungraded live colour comparison",contentScale=ContentScale.FillBounds,
                modifier=Modifier.fillMaxSize().drawWithContent { clipRect(right=size.width*split) { this@drawWithContent.drawContent() } })
            Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("BASELINE",fontSize=9.sp,color=Color.White)
                Text("RECIPE",fontSize=9.sp,color=AccentGold)
            }
        }
        Box(Modifier.fillMaxSize().pointerInput(settings.compare,controlsEnabled) {
            if(settings.compare) detectDragGestures(
                onDragStart={ split=(it.x/size.width).coerceIn(0.03f,0.97f) },
                onDrag={ change,_ -> change.consume(); split=(change.position.x/size.width).coerceIn(0.03f,0.97f) }
            ) else detectTapGestures { offset ->
                if(controlsEnabled) {
                    cameraEngine.triggerFocusAndMetering(offset.x/size.width,offset.y/size.height,size.width.toFloat()/size.height)
                    focusPoint=offset
                }
            }
        })
        Canvas(Modifier.fillMaxSize()) {
            val w=size.width; val h=size.height; val grid=Color.White.copy(alpha=.24f)
            when(settings.grid) {
                GridMode.THIRDS,GridMode.GOLDEN -> {
                    val fractions=if(settings.grid==GridMode.THIRDS) listOf(1f/3,2f/3) else listOf(.382f,.618f)
                    fractions.forEach { f ->
                        drawLine(grid,Offset(w*f,0f),Offset(w*f,h),1.dp.toPx())
                        drawLine(grid,Offset(0f,h*f),Offset(w,h*f),1.dp.toPx())
                    }
                }
                GridMode.SQUARE -> {
                    val side=minOf(w,h)
                    drawRect(grid,Offset((w-side)/2,(h-side)/2),ComposeSize(side,side),style=Stroke(1.dp.toPx()))
                }
                GridMode.OFF -> Unit
            }
            if(settings.showLevel && tilt.valid) {
                val centre=Offset(w/2,h/2)
                val r=30.dp.toPx()
                drawLine(Color.White.copy(alpha=.4f),centre-Offset(r+10.dp.toPx(),0f),centre+Offset(r+10.dp.toPx(),0f),1.dp.toPx())
                rotate(-tilt.degrees,centre) {
                    val color=if(abs(tilt.degrees)<1f) AccentGold else Color.White
                    drawLine(color,centre-Offset(r,0f),centre+Offset(r,0f),2.dp.toPx())
                    drawCircle(color,2.dp.toPx(),centre)
                }
            }
            if(settings.compare) drawLine(AccentGold,Offset(w*split,0f),Offset(w*split,h),2.dp.toPx())
            focusPoint?.let { p ->
                val d=52.dp.toPx()
                drawRect(AccentGold,p-Offset(d/2,d/2),ComposeSize(d,d),style=Stroke(1.5.dp.toPx()))
                drawCircle(AccentGold,2.dp.toPx(),p)
            }
        }
        if(!streaming) Text("Starting camera…",color=Color.White,modifier=Modifier.align(Alignment.Center),fontSize=12.sp)
        AnimatedVisibility(isShutterFlashing,enter=fadeIn(),exit=fadeOut(),modifier=Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha=.65f)))
        }
    }
}

private fun configureTransform(view: TextureView,w: Int,h: Int,size: Size,sensorOrientation: Int) {
    if(w==0 || h==0) return
    val matrix=Matrix()
    val rotation=view.display?.rotation ?: Surface.ROTATION_0
    // Android rotates SurfaceTexture preview into the natural display orientation. Do not reapply sensor rotation at ROTATION_0.
    if(rotation==Surface.ROTATION_90 || rotation==Surface.ROTATION_270) {
        val rect=android.graphics.RectF(0f,0f,w.toFloat(),h.toFloat())
        val buffer=android.graphics.RectF(0f,0f,size.height.toFloat(),size.width.toFloat())
        buffer.offset(rect.centerX()-buffer.centerX(),rect.centerY()-buffer.centerY())
        matrix.setRectToRect(rect,buffer,Matrix.ScaleToFit.FILL)
        val scale=maxOf(h.toFloat()/size.height,w.toFloat()/size.width)
        matrix.postScale(scale,scale,rect.centerX(),rect.centerY())
        matrix.postRotate((90*(rotation-2)).toFloat(),rect.centerX(),rect.centerY())
    } else if(rotation==Surface.ROTATION_180) matrix.postRotate(180f,w/2f,h/2f)
    view.setTransform(matrix)
}
