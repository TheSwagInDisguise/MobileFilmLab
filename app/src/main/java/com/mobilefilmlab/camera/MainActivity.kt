package com.mobilefilmlab.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.mobilefilmlab.camera.ui.components.MainCameraScreen
import com.mobilefilmlab.camera.ui.theme.MobileFilmLabTheme
import com.mobilefilmlab.camera.ui.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.userNotice.value = "Camera access granted"
        } else {
            viewModel.userNotice.value = "Camera permission is required to take photos"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCameraPermission()

        setContent {
            MobileFilmLabTheme {
                MainCameraScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cameraEngine.startBackgroundThread()
    }

    override fun onPause() {
        viewModel.cameraEngine.stopBackgroundThread()
        viewModel.cameraEngine.closeCamera()
        super.onPause()
    }
}
