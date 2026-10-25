package com.mobilefilmlab.camera.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.data.model.PictureEffect
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.WbMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageProcessor {

    /**
     * Builds the ColorMatrix that computationally approximates the recipe's look.
     */
    fun buildRecipeColorMatrix(recipe: Recipe): ColorMatrix {
        val matrix = ColorMatrix()

        // 1. Monochrome conversion
        if (recipe.isMonochrome) {
            val monoMatrix = ColorMatrix()
            when (recipe.name) {
                "Acros +R (red filter)", "GR Hi-Contrast B&W" -> {
                    // Dramatic red filter: 70% Red, 20% Green, 10% Blue
                    monoMatrix.set(floatArrayOf(
                        0.70f, 0.20f, 0.10f, 0f, 0f,
                        0.70f, 0.20f, 0.10f, 0f, 0f,
                        0.70f, 0.20f, 0.10f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                "Acros +Ye (yellow filter)" -> {
                    // Yellow filter: 50% Red, 40% Green, 10% Blue
                    monoMatrix.set(floatArrayOf(
                        0.50f, 0.40f, 0.10f, 0f, 0f,
                        0.50f, 0.40f, 0.10f, 0f, 0f,
                        0.50f, 0.40f, 0.10f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                "Acros +G (green filter)" -> {
                    // Green filter: 25% Red, 65% Green, 10% Blue
                    monoMatrix.set(floatArrayOf(
                        0.25f, 0.65f, 0.10f, 0f, 0f,
                        0.25f, 0.65f, 0.10f, 0f, 0f,
                        0.25f, 0.65f, 0.10f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                else -> {
                    // Standard Rec.709 luminance
                    monoMatrix.setSaturation(0.0f)
                }
            }
            matrix.postConcat(monoMatrix)
        } else {
            // 2. Saturation (-3 .. +3) + 2D Puck Color offset
            val satBase = 1.0f + (recipe.sat * 0.12f)
            val satPuck = 1.0f + (recipe.puckColor * 0.003f)
            val finalSat = (satBase * satPuck).coerceIn(0.1f, 2.0f)
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(finalSat)
            matrix.postConcat(satMatrix)
        }

        // 3. Contrast (-3 .. +3) + 2D Puck Tone offset
        val conBase = 1.0f + (recipe.con * 0.10f)
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
                // Warmer: boost red, reduce blue
                rScale += (-kDiff * 0.08f)
                bScale -= (-kDiff * 0.08f)
            } else {
                // Cooler: boost blue, reduce red
                bScale += (kDiff * 0.07f)
                rScale -= (kDiff * 0.07f)
            }
        }

        // Amber-Blue (-7 .. +7)
        val abTotal = recipe.ab + (recipe.puckColor * 0.05f)
        rScale += (abTotal * 0.025f)
        bScale -= (abTotal * 0.025f)

        // Green-Magenta (-7 .. +7)
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

        return matrix
    }

    /**
     * Processes a full-resolution bitmap by applying recipe transformations and crop.
     */
    fun processBitmap(
        source: Bitmap,
        recipe: Recipe,
        cropRect: Rect?
    ): Bitmap {
        // 1. Crop if requested
        val cropped = if (cropRect != null && cropRect.width() > 0 && cropRect.height() > 0) {
            val safeLeft = cropRect.left.coerceIn(0, source.width - 1)
            val safeTop = cropRect.top.coerceIn(0, source.height - 1)
            val safeWidth = cropRect.width().coerceAtMost(source.width - safeLeft)
            val safeHeight = cropRect.height().coerceAtMost(source.height - safeTop)
            Bitmap.createBitmap(source, safeLeft, safeTop, safeWidth, safeHeight)
        } else {
            source
        }

        // 2. Color matrix transformation
        val result = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(buildRecipeColorMatrix(recipe))
        canvas.drawBitmap(cropped, 0f, 0f, paint)

        return result
    }

    /**
     * Encodes bitmap to JPEG and injects EXIF metadata.
     */
    fun encodeJpegWithExif(
        bitmap: Bitmap,
        recipe: Recipe,
        cameraModel: String = "OPPO Find X9 Ultra"
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 96, stream)
        val jpegBytes = stream.toByteArray()

        // Inject EXIF metadata
        return try {
            val tempFile = File.createTempFile("mfl_exif_", ".jpg")
            FileOutputStream(tempFile).use { it.write(jpegBytes) }

            val exif = ExifInterface(tempFile.absolutePath)
            exif.setAttribute(ExifInterface.TAG_MAKE, "OPPO")
            exif.setAttribute(ExifInterface.TAG_MODEL, cameraModel)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MobileFilmLab v0.1.0-alpha.1")
            exif.setAttribute(
                ExifInterface.TAG_USER_COMMENT,
                "Recipe: ${recipe.name} [${recipe.group.displayName}] | ${recipe.summary()}"
            )
            exif.saveAttributes()

            val taggedBytes = tempFile.readBytes()
            tempFile.delete()
            taggedBytes
        } catch (e: Exception) {
            jpegBytes
        }
    }
}
