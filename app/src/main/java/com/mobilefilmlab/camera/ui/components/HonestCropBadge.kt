package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.FramingConfig
import com.mobilefilmlab.camera.ui.theme.AccentGreen
import com.mobilefilmlab.camera.ui.theme.TextSecondary

@Composable
fun HonestCropBadge(
    config: FramingConfig,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentGreen)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${if(config.effectiveFocalLength35mm>0) "${config.effectiveFocalLength35mm}mm eq." else "Focal length unavailable"} • ${config.cropStatus.label} • ~${String.format("%.1f", config.estimatedResolutionMp)}MP",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}
