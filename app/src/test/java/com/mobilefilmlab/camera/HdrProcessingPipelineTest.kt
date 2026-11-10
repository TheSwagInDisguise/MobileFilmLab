package com.mobilefilmlab.camera

import android.graphics.*
import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.camera.*
import com.mobilefilmlab.camera.data.model.RecipeCatalog
import com.mobilefilmlab.camera.pipeline.ImageProcessor
import com.mobilefilmlab.camera.processing.BitmapIO
import com.mobilefilmlab.camera.processing.hdr.*
import com.mobilefilmlab.camera.settings.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.*
import java.util.Random
import kotlin.math.*

/** Deterministic synthetic JPEG fixtures, not photographs or evidence of handset image quality. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],shadows=[FixtureJpegRegionDecoderShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HdrProcessingPipelineTest {
    private val context: Context get()=RuntimeEnvironment.getApplication()
    private fun jpeg(w: Int,h: Int,exposure: Double,seed: Int=1,noise: Int=0,subjectX: Int?=null): ByteArray {
        val random=Random(seed.toLong());val p=IntArray(w*h)
        for(y in 0 until h) for(x in 0 until w) {
            val radiance=if(x>w*.62) .68+(x-w*.62)/(w*.38)*1.05 else .075+.11*x/w+.025*sin(y*.08)
            fun code(value: Double): Int {
                val linear=(value*exposure).coerceIn(0.0,1.0)
                val gamma=if(linear<=.0031308) linear*12.92 else 1.055*linear.pow(1/2.4)-.055
                return (gamma*255+(if(noise>0) random.nextInt(noise*2+1)-noise else 0)).roundToInt().coerceIn(0,255)
            }
            val green=subjectX!=null && x in subjectX until subjectX+25 && y in h/3 until h*2/3
            val r=code(if(green) .02 else radiance);val g=code(if(green) .5 else radiance);val b=code(if(green) .02 else radiance)
            p[y*w+x]=(255 shl 24) or (r shl 16) or (g shl 8) or b
        }
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap.setPixels(p,0,w,0,0,w,h)
        return try {ByteArrayOutputStream().also {assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,100,it))}.toByteArray()} finally {bitmap.recycle()}
    }
    private fun bundle(w: Int=320,h: Int=240,noise: Int=0,quality: Boolean=false,subject: Boolean=false): CaptureBundle {
        val safety=CapturedJpeg(jpeg(w,h,1.0,noise=noise,subjectX=if(subject) 45 else null),PhotoExifMetadata(iso=1600,exposureTimeNs=20_000_000,sensorTimestampNs=1))
        val shortCount=if(quality) 7 else 5
        val frames=mutableListOf(safety)
        repeat(shortCount) {i -> frames+=CapturedJpeg(jpeg(w,h,.5,i+3,noise,if(subject) if(i==0) 45 else 90+i*10 else null),PhotoExifMetadata(iso=1600,exposureTimeNs=10_000_000,sensorTimestampNs=i+2L))}
        if(quality) repeat(2) {i -> frames+=CapturedJpeg(jpeg(w,h,2.0,20+i,noise),PhotoExifMetadata(iso=1600,exposureTimeNs=40_000_000,sensorTimestampNs=30+i.toLong()))}
        return CaptureBundle(frames,1f,"Synthetic HDR test",HdrCaptureInfo(quality,(1..shortCount).toList(),if(quality) listOf(8,9) else emptyList()))
    }
    private fun process(b: CaptureBundle,settings: CaptureSettings=CaptureSettings(pipeline=CapturePipeline.SMART_HDR))=ImageProcessor.processCapture(context,b,RecipeCatalog.getDefaultRecipe(),settings)
    private fun bitmap(bytes: ByteArray)=BitmapFactory.decodeByteArray(bytes,0,bytes.size)!!
    private fun pixels(b: Bitmap)=IntArray(b.width*b.height).also {b.getPixels(it,0,b.width,0,0,b.width,b.height)}
    private fun variance(b: Bitmap,x0: Int,x1: Int): Double {
        val values=mutableListOf<Double>()
        for(y in 40 until b.height-40) for(x in x0 until x1) {
            val p=b.getPixel(x,y);values+=Color.red(p)-Color.green(p).toDouble()
        }
        val mean=values.average();return values.sumOf {(it-mean)*(it-mean)}/values.size
    }
    @Test fun nativeJpegFixturesAndStripMergeRestoreSyntheticHighlightGradationWithoutUpscaling() {
        val b=bundle();val result=process(b)
        assertTrue(result.fallbackReason ?: result.note,result.appliedHdr)
        assertEquals(320,result.width);assertEquals(240,result.height)
        val original=bitmap(b.frames[0].bytes);val hdr=bitmap(result.bytes)
        try {
            val evidence=File("build/outputs/hdr-fixtures").apply {mkdirs()}
            File(evidence,"SYNTHETIC-original.png").outputStream().use {original.compress(Bitmap.CompressFormat.PNG,100,it)}
            File(evidence,"SYNTHETIC-hdr.png").outputStream().use {hdr.compress(Bitmap.CompressFormat.PNG,100,it)}
            assertTrue(Color.red(original.getPixel(285,100))>=253)
            assertTrue("Clipped safety-frame highlights should be differentiated by the short burst",Color.red(hdr.getPixel(310,100))-Color.red(hdr.getPixel(270,100))>=3)
            assertTrue(result.note.contains("JPEG-domain"));assertTrue(result.note.contains("native source grid"))
        } finally {original.recycle();hdr.recycle()}
    }
    @Test fun highIsoSyntheticChromaNoiseIsReducedAgainstSingleShortExposure() {
        val b=bundle(noise=10);val result=process(b);assertTrue(result.fallbackReason ?: result.note,result.appliedHdr)
        val reference=bitmap(b.frames[1].bytes);val hdr=bitmap(result.bytes)
        try {assertTrue("Chroma variance ${variance(reference,50,100)} -> ${variance(hdr,50,100)}",variance(hdr,50,100)<variance(reference,50,100)*.9)}
        finally {reference.recycle();hdr.recycle()}
    }
    @Test fun movingColouredSubjectIsNotAveragedIntoTheReferenceBackground() {
        val b=bundle(subject=true);val result=process(b);assertTrue(result.fallbackReason ?: result.note,result.appliedHdr)
        val hdr=bitmap(result.bytes)
        try {
            val referenceFrame=Regex("short reference (\\d+)").find(result.note)!!.groupValues[1].toInt()-1
            val subject=if(referenceFrame==1) 45 else 90+(referenceFrame-1)*10
            for(x in listOf(110,125,140).filter {it !in subject until subject+25}) {val p=hdr.getPixel(x,120);assertTrue("Moving helper colour must be rejected at x=$x",abs(Color.green(p)-Color.red(p))<12)}
        } finally {hdr.recycle()}
    }
    @Test fun qualityModeAcceptsTenCapturedFramesAndIncludesRealExposureNotes() {
        val result=process(bundle(quality=true),CaptureSettings(pipeline=CapturePipeline.QUALITY_HDR))
        assertTrue(result.fallbackReason ?: result.note,result.appliedHdr)
        assertTrue(result.note.contains("Quality HDR"));assertTrue(result.note.contains("10 captured"))
        val e=ExifInterface(ByteArrayInputStream(result.bytes))
        assertEquals(1600,e.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,0))
        assertEquals(.01,e.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME,0.0),.00001)
        assertTrue(e.getAttribute(ExifInterface.TAG_USER_COMMENT)!!.contains("short reference"))
    }
    @Test fun missingActualHelperExposureFallsBackToSafetyOriginalDecodedPixelsExactly() {
        val b=bundle();val invalid=b.copy(frames=b.frames.mapIndexed {i,f -> if(i==0) f else f.copy(metadata=PhotoExifMetadata())})
        val result=process(invalid);assertFalse(result.appliedHdr);assertNotNull(result.fallbackReason)
        val a=bitmap(b.frames[0].bytes);val z=bitmap(result.bytes)
        try {assertArrayEquals(pixels(a),pixels(z))} finally {a.recycle();z.recycle()}
    }
    @Test fun interruptedOrPartlyCapturedBurstNeverUsesTheDarkHelperAsOriginal() {
        val b=bundle();val partial=b.copy(frames=b.frames.take(3),hdr=HdrCaptureInfo(false,listOf(1,2),fallbackReason="Synthetic rejected helper"))
        val result=process(partial);assertFalse(result.appliedHdr);assertEquals("Synthetic rejected helper",result.fallbackReason)
        val a=bitmap(b.frames[0].bytes);val z=bitmap(result.bytes)
        try {assertArrayEquals(pixels(a),pixels(z))} finally {a.recycle();z.recycle()}
    }
    @Test fun duplicatedSensorTimestampsCannotMasqueradeAsAnIndependentBurst() {
        val b=bundle();val result=process(b.copy(frames=b.frames.mapIndexed {i,f -> if(i==0) f else f.copy(metadata=f.metadata.copy(sensorTimestampNs=77))}))
        assertFalse(result.appliedHdr);assertTrue(result.fallbackReason!!.contains("duplicated"))
    }
    @Test fun oddDimensionsCrossStripBoundariesWithoutChangingNativeShape() {
        val b=bundle(257,773);val result=process(b)
        assertTrue(result.fallbackReason ?: result.note,result.appliedHdr);assertEquals(257,result.width);assertEquals(773,result.height)
        val out=bitmap(result.bytes)
        try {for(y in 383..385) assertTrue(Color.red(out.getPixel(100,y)) in 50..190)} finally {out.recycle()}
    }
    @Test fun exifOrientationIsAppliedAfterRegistrationAndTwoTimesCropOnlyOnce() {
        val b=bundle(320,240)
        val rotated=b.frames.map {f ->
            val tmp=File.createTempFile("hdr-orientation",".jpg",context.cacheDir)
            try {tmp.writeBytes(f.bytes);ExifInterface(tmp).apply {setAttribute(ExifInterface.TAG_ORIENTATION,"6");saveAttributes()};f.copy(bytes=tmp.readBytes())} finally {tmp.delete()}
        }
        val result=process(b.copy(frames=rotated,cropFactor=2f))
        assertTrue(result.fallbackReason ?: result.note,result.appliedHdr);assertEquals(120,result.width);assertEquals(160,result.height)
        assertEquals(1,ExifInterface(ByteArrayInputStream(result.bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,0))
    }
    @Test fun actualPostRawSensitivityBoostIsIncludedRatherThanAssumingTimeOnly() {
        val b=bundle();val reference=process(b)
        val boosted=b.copy(frames=b.frames.mapIndexed {i,f -> if(i==0) f else f.copy(metadata=f.metadata.copy(exposureTimeNs=5_000_000,postRawSensitivityBoost=200))})
        val result=process(boosted);assertTrue(result.fallbackReason ?: result.note,result.appliedHdr)
        val a=bitmap(reference.bytes);val z=bitmap(result.bytes)
        try {assertArrayEquals(pixels(a),pixels(z))} finally {a.recycle();z.recycle()}
    }
    @Test fun changedOpticalRouteCannotBeMergedJustBecauseImageSizesMatch() {
        val b=bundle().let {it.copy(frames=it.frames.mapIndexed {i,f -> f.copy(metadata=f.metadata.copy(focalLengthMm=if(i==0) 6f else 10f))})}
        val result=process(b);assertFalse(result.appliedHdr);assertTrue(result.fallbackReason!!.contains("focal length"))
    }
    @Test fun rawFormatRejectsAnImpossibleHdrBundleInsteadOfSynthesizingRaw() {
        try {process(bundle(),CaptureSettings(format=CaptureFormat.RAW_JPEG));fail("RAW must reject HDR bundles")}
        catch(expected: IllegalArgumentException) {assertTrue(expected.message!!.contains("RAW"))}
    }
    @Test fun aSuccessfulHdrMergeDoesNotGetASecondBalancedToneOrDenoisePass() {
        val b=bundle();val a=process(b);val z=process(b,CaptureSettings(pipeline=CapturePipeline.SMART_HDR,jpegRendering=JpegRendering.BALANCED))
        assertTrue(a.appliedHdr && z.appliedHdr)
        val x=bitmap(a.bytes);val y=bitmap(z.bytes)
        try {assertArrayEquals(pixels(x),pixels(y))} finally {x.recycle();y.recycle()}
    }
}
