package com.mobilefilmlab.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.OrientationEventListener
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
    private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) viewModel.onPermissionGranted()
        else viewModel.userNotice.value="Camera permission is required. Enable it in Android app settings to take photos."
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val needed=mutableListOf(Manifest.permission.CAMERA)
        if(Build.VERSION.SDK_INT==28) needed+=listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE)
        val missing=needed.filter { ContextCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED }
        if(missing.isNotEmpty()) permissions.launch(missing.toTypedArray()) else viewModel.onPermissionGranted()
        setContent { MobileFilmLabTheme { MainCameraScreen(viewModel) } }
    }
    private val orientation by lazy { object : OrientationEventListener(this) {
        override fun onOrientationChanged(value: Int) { viewModel.cameraEngine.updateDeviceOrientation(value) }
    } }
    override fun onResume() { super.onResume(); if(orientation.canDetectOrientation()) orientation.enable(); viewModel.onResume() }
    override fun onPause() { orientation.disable(); viewModel.onPause(); super.onPause() }
    private val shutterKeys=setOf(KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_VOLUME_DOWN,KeyEvent.KEYCODE_CAMERA)
    private val consumedKeys=mutableSetOf<Int>()
    override fun onKeyDown(keyCode: Int,event: KeyEvent): Boolean {
        if(keyCode in shutterKeys && viewModel.hardwareShutterAllowed()) {
            consumedKeys.add(keyCode)
            if(event.repeatCount==0) viewModel.triggerShutter()
            return true
        }
        return super.onKeyDown(keyCode,event)
    }
    override fun onKeyUp(keyCode: Int,event: KeyEvent): Boolean {
        if(consumedKeys.remove(keyCode)) return true
        return super.onKeyUp(keyCode,event)
    }
}
