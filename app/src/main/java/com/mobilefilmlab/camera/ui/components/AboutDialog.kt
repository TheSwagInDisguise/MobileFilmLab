package com.mobilefilmlab.camera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mobilefilmlab.camera.ui.theme.AccentGold
import com.mobilefilmlab.camera.ui.theme.BorderDark
import com.mobilefilmlab.camera.ui.theme.SurfaceDark
import com.mobilefilmlab.camera.ui.theme.TextMuted
import com.mobilefilmlab.camera.ui.theme.TextSecondary

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "MobileFilmLab",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Version 0.1.0-alpha.5 • Target: Oppo Find X9 Ultra",
                    fontSize = 11.sp,
                    color = AccentGold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Features & Capabilities",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "• 77 Recipe Lab presets ported to native Camera2.\n• Quad rear camera support (14mm UW, 23mm W, 70mm 3x, 135mm 10x) with honest 2x crop.\n• 2D Photographic Styles square pad for tone & warmth tuning.\n• Pro manual exposure (ISO, Shutter, Kelvin, EV bias, Focus).\n• Scoped storage MediaStore output with complete EXIF tags.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Upstream Attribution",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ported from Recipe Lab (Sony PMCA) by voxivoid (commit aab4b57).\nLicensed under GNU General Public License v3.0 or later (GPL-3.0-or-later). Complete source code is available publicly on GitHub.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Independence Disclaimer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "Independently authored and maintained. Not affiliated with or endorsed by Sony, Fujifilm, Kodak, Leica, Ricoh, or Hasselblad.",
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
