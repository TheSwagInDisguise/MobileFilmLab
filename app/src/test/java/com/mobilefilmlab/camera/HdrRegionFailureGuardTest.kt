package com.mobilefilmlab.camera

import android.graphics.*
import com.mobilefilmlab.camera.camera.*
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.processing.hdr.HdrCaptureInfo
import com.mobilefilmlab.camera.settings.CaptureSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*

/** Uses the real Robolectric EMPTY region stub intentionally; no claimed device-codec validation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HdrRegionFailureGuardTest {
    @Test fun anEmptyRegionDecoderCannotSaveABlackHdrInsteadOfTheSafetyOriginal() {
        val bitmap=Bitmap.createBitmap(160,120,Bitmap.Config.ARGB_8888);bitmap.eraseColor(Color.rgb(120,140,160))
        val source=ByteArrayOutputStream().also {bitmap.compress(Bitmap.CompressFormat.JPEG,100,it)}.toByteArray();bitmap.recycle()
        val frames=(0..3).map {CapturedJpeg(source,PhotoExifMetadata(iso=100,exposureTimeNs=10_000_000,sensorTimestampNs=it+1L))}
        val bundle=CaptureBundle(frames,1f,"Empty codec fixture",HdrCaptureInfo(false,listOf(1,2,3)))
        val result=ImageProcessor.processCapture(RuntimeEnvironment.getApplication(),bundle,RecipeCatalog.getDefaultRecipe(),CaptureSettings())
        assertFalse(result.appliedHdr);assertTrue(result.fallbackReason!!.contains("Empty/transparent"))
        val a=BitmapFactory.decodeByteArray(source,0,source.size);val z=BitmapFactory.decodeByteArray(result.bytes,0,result.bytes.size)
        try {assertEquals(a.getPixel(80,60),z.getPixel(80,60))} finally {a.recycle();z.recycle()}
    }
}
