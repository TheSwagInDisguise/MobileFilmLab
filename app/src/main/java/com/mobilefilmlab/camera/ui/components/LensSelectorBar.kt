package com.mobilefilmlab.camera.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.LensType
import com.mobilefilmlab.camera.ui.theme.AccentGold
import com.mobilefilmlab.camera.ui.theme.BorderDark

@Composable
fun LensSelectorBar(
    activeLens: LensType,
    is2xCropActive: Boolean,
    onLensSelected: (LensType) -> Unit,
    onToggle2xCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lenses = listOf(
        LensType.ULTRA_WIDE,
        LensType.WIDE,
        LensType.TELEPHOTO,
        LensType.PERISCOPE
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, BorderDark, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Stationary 4-lens selector buttons
            lenses.forEach { lens ->
                val isActive = lens == activeLens
                val bgColor by animateColorAsState(
                    if (isActive) Color.White else Color.White.copy(alpha = 0.12f),
                    label = "lensBg"
                )
                val textColor = if (isActive) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onLensSelected(lens) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lens.label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Dedicated 2x in-sensor crop toggle button
            val cropBg by animateColorAsState(
                if (is2xCropActive) AccentGold else Color.White.copy(alpha = 0.12f),
                label = "cropBg"
            )
            val cropTextColor = if (is2xCropActive) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(cropBg)
                    .clickable { onToggle2xCrop() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "2×",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cropTextColor
                )
            }
        }
    }
}
