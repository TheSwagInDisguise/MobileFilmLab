package com.mobilefilmlab.camera.pipeline

import android.content.Context
import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.BuildConfig
import com.mobilefilmlab.camera.camera.CaptureBundle
import com.mobilefilmlab.camera.camera.PhotoExifMetadata
import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.processing.*
import com.mobilefilmlab.camera.settings.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

object ImageProcessor {
    fun buildRecipeColorMatrix(recipe: Recipe): ColorMatrix {
        val matrix = ColorMatrix()

        // 1. Monochrome conversion
        if (recipe.isMonochrome) {
            val monoMatrix = ColorMatrix()
            when (recipe.name) {
                "Acros +R (red filter)", "GR Hi-Contrast B&W" -> {
                    monoMatrix.set(floatArrayOf(
                        0.70f, 0.20f, 0.10f, 0f, 0f,
                        0.70f, 0.20f, 0.10f, 0f, 0f,
                        0.70f, 0.20f, 0.10f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                "Acros +Ye (yellow filter)" -> {
                    monoMatrix.set(floatArrayOf(
                        0.50f, 0.40f, 0.10f, 0f, 0f,
                        0.50f, 0.40f, 0.10f, 0f, 0f,
                        0.50f, 0.40f, 0.10f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                "Acros +G (green filter)" -> {
                    monoMatrix.set(floatArrayOf(
                        0.25f, 0.65f, 0.10f, 0f, 0f,
                        0.25f, 0.65f, 0.10f, 0f, 0f,
                        0.25f, 0.65f, 0.10f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                else -> {
                    monoMatrix.setSaturation(0.0f)
                }
            }
            matrix.postConcat(monoMatrix)
        } else {
            // 2. Saturation (-3 .. +3) + 2D Puck Color offset
            val styleSat = when(recipe.style) { CreativeStyle.VIVID -> 1.16f; CreativeStyle.NEUTRAL -> 0.94f; CreativeStyle.PORTRAIT -> 0.96f; CreativeStyle.LANDSCAPE -> 1.1f; CreativeStyle.DEEP -> 1.08f; else -> 1f }
            val satBase = styleSat + (recipe.sat * 0.12f)
            val satPuck = 1.0f + (recipe.puckColor * 0.003f)
            val finalSat = (satBase * satPuck).coerceIn(0.1f, 2.0f)
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(finalSat)
            matrix.postConcat(satMatrix)
        }

        // 3. Contrast (-3 .. +3) + 2D Puck Tone offset
        val styleCon = when(recipe.style) { CreativeStyle.NEUTRAL -> 0.96f; CreativeStyle.LIGHT -> 0.92f; CreativeStyle.DEEP -> 1.1f; else -> 1f }
        val conBase = styleCon + (recipe.con * 0.10f)
        val conPuck = 1.0f + (recipe.puckTone * 0.003f)
        val finalCon = (conBase * conPuck).coerceIn(0.5f, 1.8f)
        val conOffset = (1.0f - finalCon) * 128f
        val conMatrix = ColorMatrix(floatArrayOf(
            finalCon, 0f, 0f, 0f, conOffset,
            0f, finalCon, 0f, 0f, conOffset,
            0f, 0f, finalCon, 0f, conOffset,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(conMatrix)

        // 4. White Balance Kelvin + Amber-Blue / Green-Magenta Tint
        var rScale = 1.0f
        var gScale = 1.0f
        var bScale = 1.0f

        if (recipe.wbMode == WbMode.KELVIN && recipe.kelvin > 0) {
            val kDiff = (recipe.kelvin - 5600) / 1000f
            if (kDiff < 0) {
                rScale += (-kDiff * 0.08f)
                bScale -= (-kDiff * 0.08f)
            } else {
                bScale += (kDiff * 0.07f)
                rScale -= (kDiff * 0.07f)
            }
        }

        val abTotal = recipe.ab + (recipe.puckColor * 0.05f)
        rScale += (abTotal * 0.025f)
        bScale -= (abTotal * 0.025f)

        gScale += (recipe.gm * 0.025f)
        rScale -= (recipe.gm * 0.012f)
        bScale -= (recipe.gm * 0.012f)

        val wbMatrix = ColorMatrix(floatArrayOf(
            rScale.coerceIn(0.4f, 1.8f), 0f, 0f, 0f, 0f,
            0f, gScale.coerceIn(0.4f, 1.8f), 0f, 0f, 0f,
            0f, 0f, bScale.coerceIn(0.4f, 1.8f), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(wbMatrix)

        // 5. Exposure Bias EV (-15 .. +15)
        if (recipe.ev != 0) {
            val evVal = (recipe.ev / 3.0f) * 16f
            val evMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, evVal,
                0f, 1f, 0f, 0f, evVal,
                0f, 0f, 1f, 0f, evVal,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(evMatrix)
        }

        if (recipe.style == CreativeStyle.SEPIA) {
            matrix.postConcat(ColorMatrix(floatArrayOf(
                .393f,.769f,.189f,0f,0f, .349f,.686f,.168f,0f,0f,
                .272f,.534f,.131f,0f,0f, 0f,0f,0f,1f,0f)))
        }
        if (recipe.pe == PictureEffect.RETRO_PHOTO) {
            matrix.postConcat(ColorMatrix(floatArrayOf(
                .95f,0f,0f,0f,12f, 0f,.91f,0f,0f,10f,
                0f,0f,.82f,0f,8f, 0f,0f,0f,1f,0f)))
        } else if (recipe.pe == PictureEffect.SOFT_HIGH_KEY) {
            matrix.postConcat(ColorMatrix(floatArrayOf(
                .82f,0f,0f,0f,38f, 0f,.84f,0f,0f,38f,
                0f,0f,.88f,0f,40f, 0f,0f,0f,1f,0f)))
        }
        return matrix
    }

    fun previewMatrix(recipe: Recipe, calibration: LensCalibration = LensCalibration()): ColorMatrix = buildRecipeColorMatrix(recipe).apply {
        postConcat(ColorMatrix(floatArrayOf(calibration.red,0f,0f,0f,0f, 0f,calibration.green,0f,0f,0f, 0f,0f,calibration.blue,0f,0f, 0f,0f,0f,1f,0f)))
    }
    fun canPreserveOriginal(recipe: Recipe, settings: CaptureSettings, crop: Float): Boolean =
        recipe.id == "stock_oppo_natural" && recipe.puckTone==0f && recipe.puckColor==0f &&
            settings.grain==0 && settings.outputSize==OutputSize.NATIVE && settings.calibration.isIdentity && crop==1f

    data class Output(val bytes: ByteArray, val width: Int, val height: Int, val note: String)

    /** Runs on a worker dispatcher. Ownership of all temporary bitmaps stays here. */
    fun processCapture(context: Context, capture: CaptureBundle, recipe: Recipe, settings: CaptureSettings): Output {
        val baseFrame=capture.frames[if(capture.frames.size==3) 1 else 0]
        val (rawW,rawH)=BitmapIO.dimensions(baseFrame.bytes)
        val originalMeta=baseFrame.metadata.copy(
            sourceWidth=baseFrame.metadata.sourceWidth.takeIf { it>0 } ?: rawW,
            sourceHeight=baseFrame.metadata.sourceHeight.takeIf { it>0 } ?: rawH,
            pipeline=capture.label
        )
        if(capture.frames.size==1 && canPreserveOriginal(recipe,settings,capture.cropFactor)) {
            val tagged=writeExif(context,baseFrame.bytes,recipe,originalMeta,null,"Original Camera2/OEM JPEG pixel data preserved")
            val e=ExifInterface(java.io.ByteArrayInputStream(tagged))
            val rotate=e.getAttributeInt(ExifInterface.TAG_ORIENTATION,1) in listOf(5,6,7,8)
            return Output(tagged,if(rotate) rawH else rawW,if(rotate) rawW else rawH,"Original JPEG · ${capture.label}")
        }
        var bitmap: Bitmap?=null
        try {
            var note=capture.label
            val heap=Runtime.getRuntime().maxMemory()
            val maxPixels=(heap/14L).coerceIn(4_000_000L,55_000_000L)
            bitmap=if(capture.frames.size==3) {
                val fused=FusionProcessor.fuse(capture.frames); note=fused.note; fused.bitmap
            } else BitmapIO.decodeUpright(baseFrame.bytes,maxPixels)
            if(bitmap.width.toLong()*bitmap.height < rawW.toLong()*rawH && capture.frames.size==1) note+="; downsampled for memory safety"
            if(capture.cropFactor>1f) {
                val b=CropEngine.computeCropBounds(bitmap.width,bitmap.height,capture.cropFactor)
                val cropped=Bitmap.createBitmap(bitmap,b.left,b.top,b.width,b.height)
                if(cropped !== bitmap) bitmap.recycle()
                bitmap=cropped; note+="; ${capture.cropFactor}× native-pixel crop (once)"
            }
            val graded=Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888)
            Canvas(graded).drawBitmap(bitmap,0f,0f,Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter=ColorMatrixColorFilter(previewMatrix(recipe,settings.calibration)) })
            bitmap.recycle(); bitmap=graded
            applyPhotographicSharpening(bitmap,recipe.sharp.coerceIn(0,3),originalMeta.iso ?: 100)
            if(settings.grain>0) applyFilmGrain(bitmap,settings.grain,originalMeta.captureTimestampMs.toInt())
            if(settings.outputSize==OutputSize.UPSCALE_28) {
                // Preserve aspect ratio; explicitly interpolation, never labelled super-resolution.
                val targetLong=if(capture.cropFactor>1f) (6120/capture.cropFactor).roundToInt() else 6120
                val long=maxOf(bitmap.width,bitmap.height)
                if(long<targetLong) {
                    val ratio=targetLong.toFloat()/long
                    val scaled=Bitmap.createScaledBitmap(bitmap,(bitmap.width*ratio).roundToInt(),(bitmap.height*ratio).roundToInt(),true)
                    if(scaled !== bitmap) bitmap.recycle()
                    bitmap=scaled; note+="; interpolated enlargement (not native detail)"
                }
            }
            val stream=ByteArrayOutputStream()
            check(bitmap.compress(Bitmap.CompressFormat.JPEG,98,stream)) { "JPEG encoder failed" }
            val bytes=writeExif(context,stream.toByteArray(),recipe,originalMeta,bitmap.width to bitmap.height,note,settings.grain)
            return Output(bytes,bitmap.width,bitmap.height,note)
        } finally { bitmap?.takeUnless { it.isRecycled }?.recycle() }
    }
    /** In-place rows avoid a second 112MB pixel array. Threshold adapts to the reported ISO. */
    fun applyPhotographicSharpening(bitmap: Bitmap, strength: Int, iso: Int=100): Bitmap {
        if(strength<=0 || bitmap.width<3 || bitmap.height<3) return bitmap
        val w=bitmap.width
        var top=IntArray(w); var row=IntArray(w); var bottom=IntArray(w); val out=IntArray(w)
        bitmap.getPixels(top,0,w,0,0,w,1); bitmap.getPixels(row,0,w,0,1,w,1)
        for(y in 1 until bitmap.height-1) {
            bitmap.getPixels(bottom,0,w,0,y+1,w,1)
            PixelFilters.sharpenRow(top,row,bottom,out,strength,iso)
            bitmap.setPixels(out,0,w,0,y,w,1)
            val old=top; top=row; row=bottom; bottom=old
        }
        return bitmap
    }
    fun applyFilmGrain(bitmap: Bitmap, level: Int, seed: Int=1337): Bitmap {
        if(level<=0) return bitmap
        val row=IntArray(bitmap.width); var state=seed
        for(y in 0 until bitmap.height) {
            bitmap.getPixels(row,0,row.size,0,y,row.size,1)
            state=PixelFilters.grainRow(row,level,state)
            bitmap.setPixels(row,0,row.size,0,y,row.size,1)
        }
        return bitmap
    }
    /** Actual source metadata only. EXIF failures are surfaced; a metadata-less success is not silently reported. */
    fun writeExif(context: Context, bytes: ByteArray, recipe: Recipe, m: PhotoExifMetadata,
                  dimensions: Pair<Int,Int>?, note: String, grain: Int=0): ByteArray {
        val tmp=File.createTempFile("mfl_exif_",".jpg",context.cacheDir)
        try {
            tmp.writeBytes(bytes)
            val e=ExifInterface(tmp)
            fun set(tag: String, value: String?) { if(value!=null) e.setAttribute(tag,value) }
            set(ExifInterface.TAG_MAKE,m.cameraMake)
            set(ExifInterface.TAG_MODEL,m.cameraModel)
            set(ExifInterface.TAG_LENS_MAKE,m.cameraMake)
            set(ExifInterface.TAG_LENS_MODEL,m.lensModel)
            set(ExifInterface.TAG_SOFTWARE,"MobileFilmLab ${BuildConfig.VERSION_NAME}")
            if(m.focalLengthMm != null && m.fNumber != null) {
                val f=(m.focalLengthMm*1000).roundToInt(); val a=(m.fNumber*100).roundToInt()
                // LensSpecification is PHYSICAL focal length, not the 35mm equivalent.
                set(ExifInterface.TAG_LENS_SPECIFICATION,"$f/1000 $f/1000 $a/100 $a/100")
            }
            m.iso?.let { set(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,it.toString()); set("SensitivityType","3") }
            m.exposureTimeNs?.let { set(ExifInterface.TAG_EXPOSURE_TIME,"${(it/1000).coerceAtLeast(1)}/1000000") }
            m.fNumber?.let {
                set(ExifInterface.TAG_F_NUMBER,"${(it*1000).roundToInt()}/1000")
                set(ExifInterface.TAG_APERTURE_VALUE,"${(2.0*ln(it.toDouble())/ln(2.0)*1_000_000).roundToInt()}/1000000")
            }
            m.focalLengthMm?.let { set(ExifInterface.TAG_FOCAL_LENGTH,"${(it*1000).roundToInt()}/1000") }
            m.equivalent35mm?.let { set(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,it.toString()) }
            m.evBias?.let { set(ExifInterface.TAG_EXPOSURE_BIAS_VALUE,"${(it*1000).roundToInt()}/1000") }
            m.flashExif?.let { set(ExifInterface.TAG_FLASH,it.toString()) }
            set(ExifInterface.TAG_EXPOSURE_MODE,if(m.manualExposure) "1" else "0")
            set(ExifInterface.TAG_WHITE_BALANCE,if(m.manualWhiteBalance) "1" else "0")
            val timestamp=SimpleDateFormat("yyyy:MM:dd HH:mm:ss",Locale.US).format(Date(m.captureTimestampMs))
            set(ExifInterface.TAG_DATETIME,timestamp); set(ExifInterface.TAG_DATETIME_ORIGINAL,timestamp)
            if(dimensions!=null) {
                set(ExifInterface.TAG_IMAGE_WIDTH,dimensions.first.toString()); set(ExifInterface.TAG_IMAGE_LENGTH,dimensions.second.toString())
                set(ExifInterface.TAG_PIXEL_X_DIMENSION,dimensions.first.toString()); set(ExifInterface.TAG_PIXEL_Y_DIMENSION,dimensions.second.toString())
                set(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL.toString())
            }
            set(ExifInterface.TAG_IMAGE_DESCRIPTION,"${recipe.name} · MobileFilmLab")
            set(ExifInterface.TAG_USER_COMMENT,"Recipe: ${recipe.name} [${recipe.group.displayName}] | ${recipe.summary()} | Source: ${m.sourceWidth}x${m.sourceHeight} | Pipeline=${m.pipeline} | ${m.note} | $note | Grain=$grain")
            e.saveAttributes()
            return tmp.readBytes()
        } finally { tmp.delete() }
    }
}
