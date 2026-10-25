package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.ui.theme.AccentGold
import com.mobilefilmlab.camera.ui.theme.BorderDark
import com.mobilefilmlab.camera.ui.theme.SurfaceDark

@Composable
fun ManualControlStrip(
    evCompensation: Float,
    iso: String,
    shutter: String,
    kelvin: Int,
    focusMode: String,
    onEvClick: () -> Unit,
    onIsoClick: () -> Unit,
    onShutterClick: () -> Unit,
    onWbClick: () -> Unit,
    onFocusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.85f))
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ManualParamChip(
            label = "EV",
            value = String.format("%+.1f", evCompensation),
            isHighlighted = evCompensation != 0.0f,
            onClick = onEvClick
        )
        ManualParamChip(
            label = "ISO",
            value = iso,
            isHighlighted = iso != "AUTO",
            onClick = onIsoClick
        )
        ManualParamChip(
            label = "SEC",
            value = shutter,
            isHighlighted = shutter != "AUTO",
            onClick = onShutterClick
        )
        ManualParamChip(
            label = "WB",
            value = "${kelvin}K",
            isHighlighted = kelvin != 5600,
            onClick = onWbClick
        )
        ManualParamChip(
            label = "FOCUS",
            value = focusMode,
            isHighlighted = focusMode != "AF-C",
            onClick = onFocusClick
        )
    }
}

@Composable
private fun ManualParamChip(
    label: String,
    value: String,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlighted) AccentGold else Color.White.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label $value",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlighted) Color.Black else Color.White
        )
    }
}
