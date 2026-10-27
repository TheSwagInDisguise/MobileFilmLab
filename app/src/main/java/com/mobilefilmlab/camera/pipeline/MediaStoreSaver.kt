package com.mobilefilmlab.camera.pipeline

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object MediaStoreSaver {
    const val RELATIVE_FOLDER="Pictures/MobileFilmLab/"
    suspend fun savePhoto(context: Context, jpegBytes: ByteArray, recipeName: String): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp=SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(Date())
            val safeName=recipeName.replace("[^a-zA-Z0-9]".toRegex(),"").take(40)
            val filename="MFL_${timestamp}_${UUID.randomUUID().toString().take(4)}_$safeName.jpg"
            val values=ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME,filename); put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN,System.currentTimeMillis())
                if(Build.VERSION.SDK_INT>=29) { put(MediaStore.Images.Media.RELATIVE_PATH,RELATIVE_FOLDER); put(MediaStore.Images.Media.IS_PENDING,1) }
                else {
                    @Suppress("DEPRECATION") val directory=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MobileFilmLab")
                    check(directory.exists() || directory.mkdirs())
                    @Suppress("DEPRECATION") put(MediaStore.Images.Media.DATA,File(directory,filename).absolutePath)
                }
            }
            val resolver=context.contentResolver
            val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Could not create a photo record")
            try {
                resolver.openOutputStream(uri,"w")?.use { it.write(jpegBytes); it.flush() } ?: error("Could not write photo")
                if(Build.VERSION.SDK_INT>=29) resolver.update(uri,ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING,0) },null,null)
                uri
            } catch(e: Exception) { runCatching { resolver.delete(uri,null,null) }; throw e }
        }
    }
}
