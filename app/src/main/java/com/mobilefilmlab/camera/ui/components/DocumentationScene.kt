package com.mobilefilmlab.camera.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Explicit inspection-only input for rendering the production UI in documentation tests.
 * Never loaded by the app: CameraViewfinder also requires DEBUG and LocalInspectionMode.
 * The demo image lives in docs/, not Android assets, and is not included in either APK.
 */
internal val LocalDocumentationScene = staticCompositionLocalOf<Bitmap?> { null }
