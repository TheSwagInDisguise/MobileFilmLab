package com.mobilefilmlab.camera

import android.content.Context
import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.camera.*
import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.processing.BitmapIO
import com.mobilefilmlab.camera.settings.*
import com.mobilefilmlab.camera.data.repository.RecipeRepository
import com.mobilefilmlab.camera.data.importer.RecipeQrCodec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImagePipelineTest {
    private val context: Context get()=RuntimeEnvironment.getApplication()
    private fun jpeg(w: Int=160,h: Int=120): ByteArray {
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap); canvas.drawColor(Color.rgb(120,140,180))
        canvas.drawRect(20f,20f,100f,80f,Paint().apply { color=Color.RED })
        val bytes=ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes); bitmap.recycle()
        return bytes.toByteArray()
    }
    @Test fun neutralDefaultPreservesOriginalDecodedPixelsAndNativeDimensions() {
        val source=jpeg(); val meta=PhotoExifMetadata(iso=200,exposureTimeNs=10_000_000,fNumber=1.6f,focalLengthMm=8.5f,equivalent35mm=24,evBias=0f,lensModel="Test wide camera",flashExif=16)
        val out=ImageProcessor.processCapture(context,CaptureBundle(listOf(CapturedJpeg(source,meta)),1f,"Camera2 native JPEG"),RecipeCatalog.getDefaultRecipe(),CaptureSettings())
        assertEquals(160,out.width); assertEquals(120,out.height)
        val a=BitmapFactory.decodeByteArray(source,0,source.size); val b=BitmapFactory.decodeByteArray(out.bytes,0,out.bytes.size)
        val pa=IntArray(160*120); val pb=IntArray(pa.size); a.getPixels(pa,0,160,0,0,160,120); b.getPixels(pb,0,160,0,0,160,120)
        assertArrayEquals("Neutral mode must not recompress pixel data",pa,pb)
        val e=ExifInterface(ByteArrayInputStream(out.bytes))
        assertEquals(200,e.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,0))
        assertEquals(0.0,e.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE,99.0),.0001)
        assertEquals(16,e.getAttributeInt(ExifInterface.TAG_FLASH,99))
        assertEquals("Test wide camera",e.getAttribute(ExifInterface.TAG_LENS_MODEL))
        assertEquals(2.0*kotlin.math.ln(1.6)/kotlin.math.ln(2.0),e.getAttributeDouble(ExifInterface.TAG_APERTURE_VALUE,0.0),.001)
    }
    @Test fun nativeCropIsAppliedOnceAndNeverUpscaledByDefault() {
        val bytes=jpeg(400,300)
        val out=ImageProcessor.processCapture(context,CaptureBundle(listOf(CapturedJpeg(bytes,PhotoExifMetadata())),2f,"Camera2"),RecipeCatalog.getDefaultRecipe(),CaptureSettings())
        assertEquals(200,out.width); assertEquals(150,out.height)
    }
    @Test fun monochromePreviewUsesTheSameMatrixAsSavedOutput() {
        val recipe=RecipeCatalog.ALL_RECIPES.first { it.isMonochrome }
        val out=ImageProcessor.processCapture(context,CaptureBundle(listOf(CapturedJpeg(jpeg(),PhotoExifMetadata())),1f,"Camera2"),recipe,CaptureSettings())
        val bitmap=BitmapFactory.decodeByteArray(out.bytes,0,out.bytes.size)
        val c=bitmap.getPixel(50,50)
        assertTrue(abs(Color.red(c)-Color.green(c))<=1 && abs(Color.green(c)-Color.blue(c))<=1)
    }
    @Test fun qrImageCanActuallyBeScanned() {
        val recipe=RecipeCatalog.getDefaultRecipe().copy(puckTone=-25f,puckColor=40f)
        val image=RecipeQrCodec.bitmap(recipe)
        assertEquals(listOf(recipe),RecipeQrCodec.scan(image).getOrThrow())
        image.recycle()
    }
    @Test fun repositoryStartsNaturalAndImportsDoNotReplaceBuiltins() {
        context.deleteDatabase("mobilefilmlab.db")
        val repo=RecipeRepository(context)
        assertEquals("stock_oppo_natural",repo.activeRecipe.value.id)
        val original=RecipeCatalog.getDefaultRecipe()
        val imported=repo.importRecipes(listOf(original)).first()
        assertTrue(imported.id.startsWith("custom_")); assertTrue(imported.isCustom)
        assertEquals(81,repo.recipes.value.size)
        assertEquals(original,RecipeCatalog.getDefaultRecipe())
        repo.close()
    }
    @Test fun exifRotationIsHonouredInsteadOfGuessingFromAspectRatio() {
        val b=ImageProcessor.writeExif(context,jpeg(160,120),RecipeCatalog.getDefaultRecipe(),PhotoExifMetadata(),null,"Test")
        val decoded=BitmapIO.decodeUpright(b)
        assertEquals(160,decoded.width); assertEquals(120,decoded.height)
        decoded.recycle()
    }
    @Test fun settingsAndPerLensCalibrationSurviveReopen() {
        val store=SettingsStore(context)
        val wide=CaptureSettings(grain=2,zsl=false,calibration=LensCalibration(1.1f,.9f,1f))
        store.save(wide,LensType.WIDE)
        assertEquals(wide.calibration,SettingsStore(context).load(LensType.WIDE).calibration)
        assertEquals(LensCalibration(),SettingsStore(context).load(LensType.PERISCOPE).calibration)
        assertEquals(2,SettingsStore(context).load(LensType.PERISCOPE).grain)
    }
    @Test fun threeFrameFusionExecutesRealRegistrationAndProducesNativePixels() {
        val source=Bitmap.createBitmap(320,240,Bitmap.Config.ARGB_8888)
        val pixels=IntArray(320*240) { index ->
            val x=index%320; val y=index/320
            val v=(40+((x/8+y/8)%2)*130+(x+y)%25).coerceIn(0,255)
            Color.rgb(v,v,v)
        }
        source.setPixels(pixels,0,320,0,0,320,240)
        val stream=ByteArrayOutputStream(); source.compress(Bitmap.CompressFormat.JPEG,100,stream); source.recycle()
        val frame=CapturedJpeg(stream.toByteArray(),PhotoExifMetadata(iso=100,exposureTimeNs=10000000))
        val output=com.mobilefilmlab.camera.processing.FusionProcessor.fuse(listOf(frame,frame,frame))
        assertEquals(320,output.bitmap.width); assertEquals(240,output.bitmap.height)
        assertEquals(3,output.acceptedFrames)
        assertTrue(output.note.contains("3/3")); output.bitmap.recycle()
    }
    @Test fun importedFavouritesPersistWithoutChangingFactoryDefault() {
        context.deleteDatabase("mobilefilmlab.db")
        val first=RecipeRepository(context)
        val custom=first.importRecipes(listOf(RecipeCatalog.getDefaultRecipe())).first()
        first.toggleFavourite(custom); first.close()
        val second=RecipeRepository(context)
        assertEquals("stock_oppo_natural",second.activeRecipe.value.id)
        assertTrue(second.recipes.value.first { it.id==custom.id }.isFavourite)
        second.close()
    }
    @Test fun editingDefaultsDisablesJpegPassthrough() {
        val natural=RecipeCatalog.getDefaultRecipe()
        assertTrue(ImageProcessor.canPreserveOriginal(natural,CaptureSettings(),1f))
        assertFalse(ImageProcessor.canPreserveOriginal(natural.copy(puckColor=1f),CaptureSettings(),1f))
        assertFalse(ImageProcessor.canPreserveOriginal(natural,CaptureSettings(grain=1),1f))
        assertFalse(ImageProcessor.canPreserveOriginal(natural,CaptureSettings(),2f))
        assertFalse(ImageProcessor.canPreserveOriginal(natural,CaptureSettings(calibration=LensCalibration(1.01f)),1f))
    }
}
