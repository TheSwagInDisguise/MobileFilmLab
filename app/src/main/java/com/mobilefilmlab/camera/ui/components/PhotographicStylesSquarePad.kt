package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.ui.theme.AccentBlue
import com.mobilefilmlab.camera.ui.theme.AccentGold
import com.mobilefilmlab.camera.ui.theme.AccentOrange
import com.mobilefilmlab.camera.ui.theme.BorderDark
import com.mobilefilmlab.camera.ui.theme.TextMuted

@Composable
fun PhotographicStylesSquarePad(
    puckTone: Float,   // -100f (muted) .. +100f (contrast)
    puckColor: Float,  // -100f (cool) .. +100f (warm)
    onPuckMoved: (tone: Float, color: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val padSize = 220.dp

    Box(
        modifier = modifier
            .size(padSize)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF161A24),
                        Color(0xFF090A0E)
                    )
                )
            )
            .border(1.5.dp, BorderDark, RoundedCornerShape(20.dp))
    ) {
        // Axis Labels
        Text(
            text = "CONTRAST (+)",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Text(
            text = "MUTED (-)",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Text(
            text = "COOL",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = "WARM",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Canvas with Crosshairs and Draggable Puck
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val halfW = size.width / 2f
                        val halfH = size.height / 2f

                        val currentPxX = (puckColor / 100f) * halfW + halfW
                        val currentPxY = (-puckTone / 100f) * halfH + halfH

                        val newPxX = (currentPxX + dragAmount.x).coerceIn(10f, size.width - 10f)
                        val newPxY = (currentPxY + dragAmount.y).coerceIn(10f, size.height - 10f)

                        val newColor = ((newPxX - halfW) / halfW * 100f).coerceIn(-100f, 100f)
                        val newTone = -((newPxY - halfH) / halfH * 100f).coerceIn(-100f, 100f)

                        onPuckMoved(newTone, newColor)
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Dashed crosshair lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(
                color = Color(0xFF2A2E3D),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )
            drawLine(
                color = Color(0xFF2A2E3D),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )

            // Center origin mark
            drawCircle(color = Color(0xFF4A5164), radius = 3f, center = center)

            // Puck position
            val puckPxX = (puckColor / 100f) * (size.width / 2f) + (size.width / 2f)
            val puckPxY = (-puckTone / 100f) * (size.height / 2f) + (size.height / 2f)
            val puckPos = Offset(puckPxX, puckPxY)

            // Puck Outer Glow Halo
            drawCircle(color = AccentGold.copy(alpha = 0.25f), radius = 20f, center = puckPos)
            // Puck Outer White Ring
            drawCircle(color = Color.White, radius = 12f, center = puckPos)
            // Puck Center Gold Dot
            drawCircle(color = AccentGold, radius = 5f, center = puckPos)
        }
    }
}
