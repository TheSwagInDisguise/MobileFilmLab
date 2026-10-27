package com.mobilefilmlab.camera.pipeline

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mobilefilmlab.camera.settings.CaptureFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Streams DNG to storage; paired files share a stem and failed writes remove all created rows. */
object RawMediaStoreSaver {
    const val DNG_MIME = "image/x-adobe-dng"
    data class Saved(val raw: Uri, val jpeg: Uri?, val rawWidth: Int, val rawHeight: Int, val jpegWidth: Int = 0, val jpegHeight: Int = 0) {
        val preferred: Uri get() = jpeg ?: raw
    }
    private data class Target(val uri: Uri, val legacyFile: File?)

    suspend fun save(
        context: Context,
        format: CaptureFormat,
        rawWidth: Int,
        rawHeight: Int,
        writeRaw: suspend (OutputStream) -> Unit,
        makeJpeg: suspend () -> ImageProcessor.Output
    ): Result<Saved> = withContext(Dispatchers.IO) {
        runCatching {
            require(format.writesRaw && rawWidth > 0 && rawHeight > 0)
            val resolver = context.contentResolver
            val targets = mutableListOf<Target>()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val stem = "MFL_${timestamp}_${UUID.randomUUID().toString().take(6)}"
            fun create(extension: String, mime: String, width: Int, height: Int): Target {
                var legacy: File? = null
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$stem.$extension")
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    put(MediaStore.Images.Media.WIDTH, width); put(MediaStore.Images.Media.HEIGHT, height)
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, MediaStoreSaver.RELATIVE_FOLDER)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    } else {
                        @Suppress("DEPRECATION") val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MobileFilmLab")
                        check(folder.exists() || folder.mkdirs()) { "Could not create photo folder" }
                        legacy = File(folder, "$stem.$extension")
                        @Suppress("DEPRECATION") put(MediaStore.Images.Media.DATA, legacy!!.absolutePath)
                    }
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create $extension photo record")
                return Target(uri, legacy).also { targets += it }
            }
            try {
                val raw = create("dng", DNG_MIME, rawWidth, rawHeight)
                resolver.openOutputStream(raw.uri, "w")?.use { stream -> writeRaw(stream); stream.flush() }
                    ?: error("Could not open DNG output")
                currentCoroutineContext().ensureActive()
                // The caller releases the acquired RAW image before optional JPEG grading starts.
                var jpeg: Target? = null
                var jpegWidth = 0; var jpegHeight = 0
                if (format.writesJpeg) {
                    val output = makeJpeg()
                    currentCoroutineContext().ensureActive()
                    jpegWidth = output.width; jpegHeight = output.height
                    jpeg = create("jpg", "image/jpeg", output.width, output.height)
                    resolver.openOutputStream(jpeg.uri, "w")?.use { it.write(output.bytes); it.flush() }
                        ?: error("Could not open JPEG output")
                }
                currentCoroutineContext().ensureActive()
                if (Build.VERSION.SDK_INT >= 29) {
                    // Both byte streams are complete before either pending flag is cleared.
                    targets.forEach { target ->
                        check(resolver.update(target.uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) > 0) {
                            "Could not publish captured photo"
                        }
                    }
                }
                Saved(raw.uri, jpeg?.uri, rawWidth, rawHeight, jpegWidth, jpegHeight)
            } catch (failure: Throwable) {
                targets.forEach { target ->
                    runCatching { resolver.delete(target.uri, null, null) }
                    runCatching { target.legacyFile?.delete() }
                }
                throw failure
            }
        }
    }
}
