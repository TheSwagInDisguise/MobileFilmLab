package com.mobilefilmlab.camera.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.settings.LensCalibration

@Composable
fun RecipeLiveThumbnail(bitmap: Bitmap?,recipe: Recipe,calibration: LensCalibration,modifier: Modifier=Modifier) {
    val matrix=remember(recipe,calibration) { ColorMatrix(ImageProcessor.previewMatrix(recipe,calibration).array) }
    Box(modifier.background(Color(0xFF222630)),contentAlignment=Alignment.Center) {
        if(bitmap!=null && !bitmap.isRecycled) Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,colorFilter=ColorFilter.colorMatrix(matrix))
        else Text(if(recipe.isMonochrome) "B&W" else "LIVE",fontSize=9.sp,color=Color.LightGray)
    }
}
