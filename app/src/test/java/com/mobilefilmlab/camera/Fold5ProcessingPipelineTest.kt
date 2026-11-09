package com.mobilefilmlab.camera

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.camera.*
import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.data.repository.RecipeRepository
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.processing.*
import com.mobilefilmlab.camera.settings.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random
import kotlin.math.abs

/** Synthetic JPEG fixtures exercise shipped native code, NOT a Fold5 camera or quality calibration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Fold5ProcessingPipelineTest {
    private val w=320;private val h=240
    private fun bytes(pixels: IntArray): ByteArray {
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap.setPixels(pixels,0,w,0,0,w,h)
        return ByteArrayOutputStream().use {out ->check(bitmap.compress(Bitmap.CompressFormat.JPEG,100,out));bitmap.recycle();out.toByteArray()}
    }
    private fun texture(): IntArray {val r=Random(15);return IntArray(w*h) {val v=60+r.nextInt(140);Color.rgb(v,v,v)}}
    private fun frame(data: ByteArray,time: Long?=10_000_000)=CapturedJpeg(data,PhotoExifMetadata(iso=100,exposureTimeNs=time))
    @Test fun optionalBalancedRendererKeepsNativeDimensionsAndRecordsItsProcessing() {
        val context=RuntimeEnvironment.getApplication();val source=bytes(texture());val settings=CaptureSettings(jpegRendering=JpegRendering.BALANCED)
        assertFalse(ImageProcessor.canPreserveOriginal(RecipeCatalog.getDefaultRecipe(),settings,1f))
        val out=ImageProcessor.processCapture(context,CaptureBundle(listOf(frame(source)),1f,"Native fixture"),RecipeCatalog.getDefaultRecipe(),settings)
        assertEquals(w,out.width);assertEquals(h,out.height);assertTrue(out.note.contains("balanced JPEG"))
        val exif=ExifInterface(ByteArrayInputStream(out.bytes))
        assertEquals(100,exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,0))
        assertTrue(exif.getAttribute(ExifInterface.TAG_USER_COMMENT).orEmpty().contains("balanced JPEG"))
        val cropped=ImageProcessor.processCapture(context,CaptureBundle(listOf(frame(source)),2f,"Native fixture"),RecipeCatalog.getDefaultRecipe(),settings)
        assertEquals(w/2,cropped.width);assertEquals(h/2,cropped.height)
    }
    @Test fun balancedRowsReuseTheInputBitmapAndKeepFlatColour() {
        val b=Bitmap.createBitmap(400,300,Bitmap.Config.ARGB_8888);b.eraseColor(Color.rgb(30,110,190))
        assertSame(b,ImageProcessor.applyBalancedJpeg(b,3200));assertEquals(Color.rgb(30,110,190),b.getPixel(200,150));b.recycle()
    }
    @Test fun absentIsoUsesConservativeRenderingWithoutFabricatingAnExifSensitivity() {
        val context=RuntimeEnvironment.getApplication();val source=bytes(texture());val settings=CaptureSettings(jpegRendering=JpegRendering.BALANCED)
        fun render(iso: Int?)=ImageProcessor.processCapture(context,CaptureBundle(listOf(CapturedJpeg(source,PhotoExifMetadata(iso=iso))),1f,"Native fixture"),RecipeCatalog.getDefaultRecipe(),settings)
        val unknown=render(null);val conservative=render(800)
        val a=BitmapIO.decodeUpright(unknown.bytes);val b=BitmapIO.decodeUpright(conservative.bytes)
        for(y in 0 until h step 11) for(x in 0 until w step 13) assertEquals(a.getPixel(x,y),b.getPixel(x,y))
        assertEquals(-1,ExifInterface(ByteArrayInputStream(unknown.bytes)).getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,-1))
        a.recycle();b.recycle()
    }
    @Test fun rawOnlyDisablesTheJpegRendererWhileRawJpegRetainsItForTheJpegOnly() {
        assertEquals(JpegRendering.CAMERA_ORIGINAL,CaptureFormatPolicy.normalize(CaptureSettings(format=CaptureFormat.RAW,jpegRendering=JpegRendering.BALANCED)).jpegRendering)
        assertEquals(JpegRendering.BALANCED,CaptureFormatPolicy.normalize(CaptureSettings(format=CaptureFormat.RAW_JPEG,jpegRendering=JpegRendering.BALANCED)).jpegRendering)
    }
    @Test fun fold5DisplayAliasKeepsFactoryIdsCountAndCustomNames() {
        val context=RuntimeEnvironment.getApplication();context.deleteDatabase("mobilefilmlab.db")
        val repo=RecipeRepository(context,DeviceProfile.GALAXY_Z_FOLD5.naturalLabel)
        assertEquals(80,repo.recipes.value.size);assertEquals("stock_oppo_natural",repo.activeRecipe.value.id)
        assertEquals("Fold5 Natural (Camera2)",repo.activeRecipe.value.name)
        val custom=repo.importRecipes(listOf(RecipeCatalog.getDefaultRecipe().copy(name="My unchanged custom name"))).first()
        assertEquals("My unchanged custom name",repo.recipes.value.first {it.id==custom.id}.name)
        repo.toggleFavourite(repo.activeRecipe.value);repo.refresh()
        assertTrue(repo.recipes.value.first {it.id=="stock_oppo_natural"}.isFavourite)
        assertEquals("Oppo Natural (Camera2)",RecipeCatalog.getDefaultRecipe().name);repo.close()
    }
    @Test fun missingExposureMetadataUsesUnmodifiedMiddlePixelsInsteadOfGuessingAnEv() {
        val data=bytes(texture());val output=FusionProcessor.fuse(listOf(frame(data),frame(data,null),frame(data)))
        assertEquals(1,output.acceptedFrames);assertTrue(output.note.contains("metadata"))
        val original=BitmapIO.decodeUpright(data)
        for(y in 0 until h step 17) for(x in 0 until w step 19) assertEquals(original.getPixel(x,y),output.bitmap.getPixel(x,y))
        original.recycle();output.bitmap.recycle()
    }
    @Test fun sameLumaDifferentColourMotionCannotTurnTheReferenceSubjectPurple() {
        val middle=texture();val other=middle.copyOf()
        for(y in 90..145) for(x in 140..195) {middle[y*w+x]=Color.rgb(180,20,20);other[y*w+x]=Color.rgb(20,50,180)}
        val output=FusionProcessor.fuse(listOf(frame(bytes(other)),frame(bytes(middle)),frame(bytes(other))))
        assertEquals(3,output.acceptedFrames)
        val p=output.bitmap.getPixel(165,115)
        assertTrue("Moving hue must be rejected: ${Color.red(p)},${Color.blue(p)}",Color.red(p)>170 && Color.blue(p)<40)
        output.bitmap.recycle()
    }
    @Test fun realNativeFusionRetainsDarkerExposureHighlightDetailUntilToneMapping() {
        val random=Random(32)
        val radiance=DoubleArray(w*h) { .05+random.nextDouble()*.42 }
        for(y in 60..180) for(x in 200..285) radiance[y*w+x]=1.05+(x-200)*.7/85
        fun exposed(scale: Double)=bytes(IntArray(w*h) {val v=FusionMath.toSrgb(radiance[it]*scale);Color.rgb(v,v,v)})
        val middleBytes=exposed(1.0)
        val output=FusionProcessor.fuse(listOf(frame(exposed(.5),5_000_000),frame(middleBytes),frame(exposed(2.0),20_000_000)))
        assertTrue(output.acceptedFrames>=2);assertTrue("Expected recovered headroom: ${output.note}",output.note.contains("recovered-highlight"))
        val early=Color.red(output.bitmap.getPixel(213,120));val late=Color.red(output.bitmap.getPixel(270,120))
        assertTrue("Clipped reference highlights should become distinct: $early, $late",late>early+8)
        val original=BitmapIO.decodeUpright(middleBytes)
        assertTrue(abs(Color.red(original.getPixel(213,120))-Color.red(original.getPixel(270,120)))<=1)
        original.recycle();output.bitmap.recycle()
    }
}
