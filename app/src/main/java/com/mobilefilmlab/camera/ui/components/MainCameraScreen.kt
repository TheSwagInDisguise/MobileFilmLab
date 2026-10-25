package com.mobilefilmlab.camera.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.camera.FlashMode
import com.mobilefilmlab.camera.ui.theme.AccentGold
import com.mobilefilmlab.camera.ui.theme.BgDark
import com.mobilefilmlab.camera.ui.theme.BorderDark
import com.mobilefilmlab.camera.ui.theme.SurfaceDark
import com.mobilefilmlab.camera.ui.theme.TextMuted
import com.mobilefilmlab.camera.ui.viewmodel.CameraMode
import com.mobilefilmlab.camera.ui.viewmodel.CameraViewModel

@Composable
fun MainCameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mode by viewModel.mode.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val activeLens by viewModel.activeLens.collectAsState()
    val is2xActive by viewModel.is2xActive.collectAsState()
    val framingConfig by viewModel.framingConfig.collectAsState()
    val activeRecipe by viewModel.activeRecipe.collectAsState()
    val allRecipes by viewModel.allRecipes.collectAsState()
    val puckTone by viewModel.puckTone.collectAsState()
    val puckColor by viewModel.puckColor.collectAsState()
    val evCompensation by viewModel.evCompensation.collectAsState()
    val iso by viewModel.iso.collectAsState()
    val shutterSpeed by viewModel.shutterSpeed.collectAsState()
    val whiteBalanceKelvin by viewModel.whiteBalanceKelvin.collectAsState()
    val focusMode by viewModel.focusMode.collectAsState()

    val isRecipeSheetOpen by viewModel.isRecipeSheetOpen.collectAsState()
    val isDiagnosticsOpen by viewModel.isDiagnosticsOpen.collectAsState()
    val isAboutOpen by viewModel.isAboutOpen.collectAsState()
    val isShutterFlashing by viewModel.isShutterFlashing.collectAsState()
    val lastCapturedUri by viewModel.lastCapturedUri.collectAsState()
    val lastCapturedThumbnail by viewModel.lastCapturedThumbnail.collectAsState()
    val userNotice by viewModel.userNotice.collectAsState()

    LaunchedEffect(userNotice) {
        userNotice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.userNotice.value = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ==================== TOP ACTION BAR ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Flash / Torch Button
                val (flashIcon, flashTint) = when (flashMode) {
                    FlashMode.OFF -> Pair(Icons.Default.FlashOff, TextMuted)
                    FlashMode.AUTO -> Pair(Icons.Default.FlashAuto, AccentGold)
                    FlashMode.ON -> Pair(Icons.Default.FlashOn, AccentGold)
                    FlashMode.TORCH -> Pair(Icons.Default.Bolt, AccentGold)
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, CircleShape)
                        .clickable { viewModel.toggleFlash() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = "Flash Toggle ($flashMode)",
                        tint = flashTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Center AUTO vs PRO Mode Switcher
                AutoProModeToggle(
                    currentMode = mode,
                    onModeSelected = { viewModel.setMode(it) }
                )

                // Info / About Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, CircleShape)
                        .clickable { viewModel.isAboutOpen.value = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==================== VIEWFINDER AREA ====================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                CameraViewfinder(
                    cameraEngine = viewModel.cameraEngine,
                    activeRecipe = activeRecipe,
                    puckTone = puckTone,
                    puckColor = puckColor,
                    isShutterFlashing = isShutterFlashing,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Lens Selection Bar inside lower part of viewfinder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    LensSelectorBar(
                        activeLens = activeLens,
                        is2xCropActive = is2xActive,
                        onLensSelected = { viewModel.selectLens(it) },
                        onToggle2xCrop = { viewModel.toggle2xCrop() }
                    )
                }
            }

            // Honest Crop Telemetry Badge
            HonestCropBadge(config = framingConfig)

            // Pro Mode Manual Control Strip
            if (mode == CameraMode.PRO) {
                ManualControlStrip(
                    evCompensation = evCompensation,
                    iso = iso,
                    shutter = shutterSpeed,
                    kelvin = whiteBalanceKelvin,
                    focusMode = focusMode,
                    onEvClick = { viewModel.cycleEvCompensation() },
                    onIsoClick = { viewModel.cycleIso() },
                    onShutterClick = { viewModel.cycleShutter() },
                    onWbClick = { viewModel.cycleWhiteBalance() },
                    onFocusClick = { viewModel.cycleFocus() },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Film Look Pill
            FilmLookPill(
                recipe = activeRecipe,
                onClick = { viewModel.isRecipeSheetOpen.value = true },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // ==================== BOTTOM CAPTURE DECK ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Gallery Thumbnail Shortcut
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .border(1.5.dp, BorderDark, RoundedCornerShape(14.dp))
                        .clickable {
                            lastCapturedUri?.let { uri ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "image/*")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open gallery viewer", Toast.LENGTH_SHORT).show()
                                }
                            } ?: run {
                                Toast.makeText(context, "No photos captured yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (lastCapturedThumbnail != null) {
                        Image(
                            bitmap = lastCapturedThumbnail!!.asImageBitmap(),
                            contentDescription = "Last photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Gallery",
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Large Shutter Button (Photos only!)
                ShutterButton(onClick = { viewModel.triggerShutter() })

                // Camera Diagnostics Shortcut
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.5.dp, BorderDark, CircleShape)
                        .clickable { viewModel.isDiagnosticsOpen.value = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Diagnostics",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ==================== BOTTOM SHEET: RECIPE & 2D PAD ====================
        if (isRecipeSheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.isRecipeSheetOpen.value = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                RecipeBottomSheet(
                    recipes = allRecipes,
                    activeRecipe = activeRecipe,
                    puckTone = puckTone,
                    puckColor = puckColor,
                    onRecipeSelected = { viewModel.selectRecipe(it) },
                    onToggleFavourite = { viewModel.toggleFavourite(it) },
                    onPuckMoved = { tone, color -> viewModel.updatePuck(tone, color) },
                    onResetPuck = { viewModel.resetPuck() },
                    onSaveCustomRecipe = { name -> viewModel.saveAsCustomRecipe(name) },
                    onDismiss = { viewModel.isRecipeSheetOpen.value = false },
                    modifier = Modifier.clickable(enabled = false) {}
                )
            }
        }

        // ==================== DIALOGS ====================
        if (isDiagnosticsOpen) {
            DiagnosticsDialog(
                reportText = viewModel.getDiagnosticsReport(),
                onDismiss = { viewModel.isDiagnosticsOpen.value = false }
            )
        }

        if (isAboutOpen) {
            AboutDialog(onDismiss = { viewModel.isAboutOpen.value = false })
        }
    }
}
