package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
    val currentOnPuckMoved by rememberUpdatedState(onPuckMoved)

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
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val halfW = size.width / 2f
                    val halfH = size.height / 2f
                    val padRangeX = (halfW - 20f).coerceAtLeast(1f)
                    val padRangeY = (halfH - 20f).coerceAtLeast(1f)

                    val color = ((down.position.x - halfW) / padRangeX * 100f).coerceIn(-100f, 100f)
                    val tone = -((down.position.y - halfH) / padRangeY * 100f).coerceIn(-100f, 100f)
                    currentOnPuckMoved(tone, color)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()

                        val moveColor = ((change.position.x - halfW) / padRangeX * 100f).coerceIn(-100f, 100f)
                        val moveTone = -((change.position.y - halfH) / padRangeY * 100f).coerceIn(-100f, 100f)
                        currentOnPuckMoved(moveTone, moveColor)
                    }
                }
            }
    ) {
        // Axis Labels
        Text(
            text = "CONTRAST (+)",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
        Text(
            text = "MUTED (-)",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
        Text(
            text = "COOL",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )
        Text(
            text = "WARM",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        // Canvas with Crosshairs and Draggable Puck
        Canvas(modifier = Modifier.fillMaxSize()) {
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

            // Calculate exact puck position
            val halfW = size.width / 2f
            val halfH = size.height / 2f
            val padRangeX = (halfW - 20f).coerceAtLeast(1f)
            val padRangeY = (halfH - 20f).coerceAtLeast(1f)
            val puckPxX = (puckColor / 100f) * padRangeX + halfW
            val puckPxY = (-puckTone / 100f) * padRangeY + halfH
            val puckPos = Offset(puckPxX, puckPxY)

            // Puck Outer Glow Halo
            drawCircle(color = AccentGold.copy(alpha = 0.35f), radius = 22f, center = puckPos)
            // Puck Outer White Ring
            drawCircle(color = Color.White, radius = 13f, center = puckPos)
            // Puck Center Gold Dot
            drawCircle(color = AccentGold, radius = 6f, center = puckPos)
        }
    }
}
