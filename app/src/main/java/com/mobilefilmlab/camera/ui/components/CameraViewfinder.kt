package com.mobilefilmlab.camera.ui.components

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mobilefilmlab.camera.camera.CameraEngine
import com.mobilefilmlab.camera.ui.theme.AccentGreen

@Composable
fun CameraViewfinder(
    cameraEngine: CameraEngine,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(20.dp))
    ) {
        // Native Camera2 Preview TextureView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                TextureView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            val previewSize = cameraEngine.getOptimalPreviewSize(width, height)
                            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
                            configureTransform(this@apply, width, height, previewSize, cameraEngine.getSensorOrientation())
                            
                            val surface = Surface(surfaceTexture)
                            cameraEngine.bindPreviewSurface(surface, previewSize.width, previewSize.height)
                        }

                        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            val previewSize = cameraEngine.getOptimalPreviewSize(width, height)
                            configureTransform(this@apply, width, height, previewSize, cameraEngine.getSensorOrientation())
                        }

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            cameraEngine.unbindPreviewSurface()
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                            // Frame updated
                        }
                    }

                    val st = surfaceTexture
                    if (isAvailable && st != null) {
                        val previewSize = cameraEngine.getOptimalPreviewSize(width, height)
                        st.setDefaultBufferSize(previewSize.width, previewSize.height)
                        configureTransform(this, width, height, previewSize, cameraEngine.getSensorOrientation())
                        val surface = Surface(st)
                        cameraEngine.bindPreviewSurface(surface, previewSize.width, previewSize.height)
                    }
                }
            },
            update = { textureView ->
                if (textureView.isAvailable && textureView.surfaceTexture != null) {
                    val previewSize = cameraEngine.getOptimalPreviewSize(textureView.width, textureView.height)
                    configureTransform(textureView, textureView.width, textureView.height, previewSize, cameraEngine.getSensorOrientation())
                }
            }
        )

        // Rule of Thirds Grid & Electronic Horizon Level Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Rule of Thirds Grid
            val gridColor = Color.White.copy(alpha = 0.16f)
            drawLine(color = gridColor, start = Offset(w / 3f, 0f), end = Offset(w / 3f, h), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(2f * w / 3f, 0f), end = Offset(2f * w / 3f, h), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(0f, h / 3f), end = Offset(w, h / 3f), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(0f, 2f * h / 3f), end = Offset(w, 2f * h / 3f), strokeWidth = 1f)

            // Center Electronic Horizon Level
            val levelY = h / 2f
            val centerX = w / 2f
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
            drawLine(
                color = AccentGreen,
                start = Offset(centerX - 35f, levelY),
                end = Offset(centerX + 35f, levelY),
                strokeWidth = 2f,
                pathEffect = dash
            )
            drawCircle(color = AccentGreen, radius = 3f, center = Offset(centerX, levelY))
        }
    }
}

private fun configureTransform(
    view: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    previewSize: Size,
    sensorOrientation: Int
) {
    if (viewWidth == 0 || viewHeight == 0) return
    val matrix = Matrix()
    val displayRotation = view.display?.rotation ?: Surface.ROTATION_0

    val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
    val bufferRect: RectF
    if (sensorOrientation == 0 || sensorOrientation == 180) {
        bufferRect = RectF(0f, 0f, previewSize.width.toFloat(), previewSize.height.toFloat())
    } else {
        bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
    }
    val centerX = viewRect.centerX()
    val centerY = viewRect.centerY()

    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)

    val scale = if (sensorOrientation == 0 || sensorOrientation == 180) {
        maxOf(
            viewWidth.toFloat() / previewSize.width.toFloat(),
            viewHeight.toFloat() / previewSize.height.toFloat()
        )
    } else {
        maxOf(
            viewWidth.toFloat() / previewSize.height.toFloat(),
            viewHeight.toFloat() / previewSize.width.toFloat()
        )
    }
    matrix.postScale(scale, scale, centerX, centerY)

    if (Surface.ROTATION_90 == displayRotation || Surface.ROTATION_270 == displayRotation) {
        matrix.postRotate((90 * (displayRotation - 2)).toFloat(), centerX, centerY)
    } else if (Surface.ROTATION_180 == displayRotation) {
        matrix.postRotate(180f, centerX, centerY)
    }

    view.setTransform(matrix)
}
