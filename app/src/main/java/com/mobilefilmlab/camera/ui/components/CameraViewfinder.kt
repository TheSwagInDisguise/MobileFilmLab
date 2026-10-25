package com.mobilefilmlab.camera.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.mobilefilmlab.camera.ui.theme.AccentGreen

@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1F2B),
                        Color(0xFF0F1218),
                        Color(0xFF08090C)
                    )
                )
            )
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(20.dp))
    ) {
        // Grid & Level Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Rule of Thirds Grid
            val gridColor = Color.White.copy(alpha = 0.12f)
            drawLine(color = gridColor, start = Offset(w / 3f, 0f), end = Offset(w / 3f, h), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(2f * w / 3f, 0f), end = Offset(2f * w / 3f, h), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(0f, h / 3f), end = Offset(w, h / 3f), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(0f, 2f * h / 3f), end = Offset(w, 2f * h / 3f), strokeWidth = 1f)

            // Electronic Horizon Level in center
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
