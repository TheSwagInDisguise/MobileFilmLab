package com.mobilefilmlab.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.ui.theme.AccentGold
import com.mobilefilmlab.camera.ui.theme.BorderDark
import com.mobilefilmlab.camera.ui.theme.SurfaceDark
import com.mobilefilmlab.camera.ui.theme.TextMuted
import com.mobilefilmlab.camera.ui.viewmodel.CameraMode

@Composable
fun AutoProModeToggle(
    currentMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark.copy(alpha = 0.85f))
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isAuto = currentMode == CameraMode.AUTO
        val autoBg by animateColorAsState(if (isAuto) AccentGold else Color.Transparent, label = "autoBg")
        val proBg by animateColorAsState(if (!isAuto) AccentGold else Color.Transparent, label = "proBg")

        Box(
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(autoBg)
                .clickable { onModeSelected(CameraMode.AUTO) }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AUTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAuto) Color.White else TextMuted,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(proBg)
                .clickable { onModeSelected(CameraMode.PRO) }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PRO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isAuto) Color.White else TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
