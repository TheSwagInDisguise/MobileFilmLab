package com.mobilefilmlab.camera.pipeline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.camera.PhotoExifMetadata
import com.mobilefilmlab.camera.data.model.Recipe
import com.mobilefilmlab.camera.data.model.WbMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        return matrix
    }

    /**
     * High-performance unsharp mask convolution filter for restoring edge acuity and texture detail.
     * Uses in-place scanline row caching to cut memory allocation in half, eliminating OOM pressure on 28MP/50MP frames.
     */
    fun applyPhotographicSharpening(bitmap: Bitmap, strength: Int): Bitmap {
        if (strength <= 0) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val w = (strength * 0.12f).coerceIn(0.08f, 0.40f)
        val centerWeight = 1.0f + 4.0f * w

        // Scanline buffers for in-place convolution: top row cache and current row copy
        val prevRow = IntArray(width)
        System.arraycopy(pixels, 0, prevRow, 0, width)
        val currRowOrig = IntArray(width)

        for (y in 1 until height - 1) {
            val yOffset = y * width
            System.arraycopy(pixels, yOffset, currRowOrig, 0, width)

            for (x in 1 until width - 1) {
                val idx = yOffset + x
                val cC = currRowOrig[x]
                val cT = prevRow[x]
                val cB = pixels[idx + width]
                val cL = currRowOrig[x - 1]
                val cR = currRowOrig[x + 1]

                val rC = (cC shr 16) and 0xFF
                val gC = (cC shr 8) and 0xFF
                val bC = cC and 0xFF

                val rT = (cT shr 16) and 0xFF
                val rB = (cB shr 16) and 0xFF
                val rL = (cL shr 16) and 0xFF
                val rR = (cR shr 16) and 0xFF

                val gT = (cT shr 8) and 0xFF
                val gB = (cB shr 8) and 0xFF
                val gL = (cL shr 8) and 0xFF
                val gR = (cR shr 8) and 0xFF

                val bT = cT and 0xFF
                val bB = cB and 0xFF
                val bL = cL and 0xFF
                val bR = cR and 0xFF

                val newR = (rC * centerWeight - (rT + rB + rL + rR) * w).toInt().coerceIn(0, 255)
                val newG = (gC * centerWeight - (gT + gB + gL + gR) * w).toInt().coerceIn(0, 255)
                val newB = (bC * centerWeight - (bT + bB + bL + bR) * w).toInt().coerceIn(0, 255)

                pixels[idx] = (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
            }

            System.arraycopy(currRowOrig, 0, prevRow, 0, width)
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Processes a full-resolution bitmap by applying recipe transformations, crop, and sharpening.
     * Intermediate buffers are eagerly recycled to maintain minimal memory footprint during 28MP/50MP processing.
     */
    fun processBitmap(
        source: Bitmap,
        recipe: Recipe,
        cropRect: Rect?
    ): Bitmap {
        val cropped = if (cropRect != null && cropRect.width() > 0 && cropRect.height() > 0) {
            val safeLeft = cropRect.left.coerceIn(0, source.width - 1)
            val safeTop = cropRect.top.coerceIn(0, source.height - 1)
            val safeWidth = cropRect.width().coerceAtMost(source.width - safeLeft)
            val safeHeight = cropRect.height().coerceAtMost(source.height - safeTop)
            val cr = Bitmap.createBitmap(source, safeLeft, safeTop, safeWidth, safeHeight)
            if (cr != source) {
                source.recycle()
            }
            cr
        } else {
            source
        }

        val graded = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(graded)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(buildRecipeColorMatrix(recipe))
        canvas.drawBitmap(cropped, 0f, 0f, paint)

        if (graded != cropped) {
            cropped.recycle()
        }

        // Apply recipe-driven sharpness (defaults to +1 for authentic crisp photographic definition)
        val effectiveSharpness = (recipe.sharp + 1).coerceIn(0, 3)
        return if (effectiveSharpness > 0) {
            val sharpened = applyPhotographicSharpening(graded, effectiveSharpness)
            if (sharpened != graded) {
                graded.recycle()
            }
            sharpened
        } else {
            graded
        }
    }

    /**
     * Encodes bitmap to JPEG at 100% maximum quality and injects full hardware EXIF shooting metadata.
     * Accurately displays camera model, lens name, ISO, shutter speed, f-stop, focal length, and EV.
     */
    fun encodeJpegWithExif(
        bitmap: Bitmap,
        recipe: Recipe,
        metadata: PhotoExifMetadata = PhotoExifMetadata()
    ): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val jpegBytes = stream.toByteArray()

        return try {
            val tempFile = File.createTempFile("mfl_exif_", ".jpg")
            FileOutputStream(tempFile).use { it.write(jpegBytes) }

            val exif = ExifInterface(tempFile.absolutePath)

            // Hardware & Camera Identifiers
            exif.setAttribute(ExifInterface.TAG_MAKE, "OPPO")
            exif.setAttribute(ExifInterface.TAG_MODEL, metadata.cameraModel)
            exif.setAttribute(ExifInterface.TAG_LENS_MAKE, "OPPO")
            exif.setAttribute(ExifInterface.TAG_LENS_MODEL, metadata.lensDescription)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MobileFilmLab v0.1.0-alpha.7")

            // Exposure & Optical Metadata for Google Photos / ColorOS Gallery info panel
            metadata.iso?.let { iso ->
                if (iso > 0) {
                    exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, iso.toString())
                    exif.setAttribute("SensitivityType", "3") // SOS
                }
            }

            metadata.fNumber?.let { fn ->
                if (fn > 0f) {
                    val fnStr = String.format(Locale.US, "%.1f", fn)
                    exif.setAttribute(ExifInterface.TAG_F_NUMBER, fnStr)
                    exif.setAttribute(ExifInterface.TAG_APERTURE_VALUE, fnStr)
                }
            }

            metadata.exposureTimeNs?.let { ns ->
                if (ns > 0L) {
                    val sec = ns / 1_000_000_000.0
                    exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, String.format(Locale.US, "%.6f", sec))
                }
            }

            metadata.focalLengthMm?.let { fl ->
                if (fl > 0f) {
                    val rational = "${(fl * 100).toInt()}/100"
                    exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, rational)
                }
            }

            metadata.equivalent35mm?.let { eq ->
                if (eq > 0) {
                    exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, eq.toString())
                }
            }

            metadata.evBias?.let { ev ->
                exif.setAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, String.format(Locale.US, "%.2f", ev))
            }

            // Timestamps
            val now = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
            exif.setAttribute(ExifInterface.TAG_DATETIME, now)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, now)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, now)

            // Recipe description
            exif.setAttribute(
                ExifInterface.TAG_USER_COMMENT,
                "Recipe: ${recipe.name} [${recipe.group.displayName}] | ${recipe.summary()}"
            )

            // Upright orientation
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())

            exif.saveAttributes()

            val taggedBytes = tempFile.readBytes()
            tempFile.delete()
            taggedBytes
        } catch (e: Exception) {
            jpegBytes
        }
    }
        } catch (e: Exception) {
            jpegBytes
        }
    }
}
