package com.mobilefilmlab.camera.gallery

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.mobilefilmlab.camera.pipeline.MediaStoreSaver
import kotlin.math.roundToInt

/** Only this app's output folder; no broad photo-library permission is requested. */
class PhotoRepository(private val context: Context) {
    data class Photo(val uri: Uri,val name: String,val width: Int,val height: Int,val mimeType: String="image/jpeg") {
        val isRaw: Boolean get() = name.endsWith(".dng",true) || mimeType in setOf("image/x-adobe-dng","image/dng","image/x-dng")
        val shareMime: String get() = if(isRaw) "image/x-adobe-dng" else mimeType
    }
    data class Details(val title: String,val size: String,val exposure: String,val processing: String)
    fun list(): List<Photo> {
        val collection=MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection=arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME,MediaStore.Images.Media.WIDTH,MediaStore.Images.Media.HEIGHT,MediaStore.Images.Media.MIME_TYPE)
        val selection=if(Build.VERSION.SDK_INT>=29) "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.IS_PENDING} = 0" else "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.DATA} LIKE ?"
        val args=if(Build.VERSION.SDK_INT>=29) arrayOf(MediaStoreSaver.RELATIVE_FOLDER,"MFL_%") else arrayOf("MFL_%","%/Pictures/MobileFilmLab/%")
        val photos=mutableListOf<Photo>()
        context.contentResolver.query(collection,projection,selection,args,"${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC")?.use { c ->
            while(c.moveToNext() && photos.size<500) photos+=Photo(ContentUris.withAppendedId(collection,c.getLong(0)),c.getString(1),c.getInt(2),c.getInt(3),c.getString(4) ?: "image/jpeg")
        }
        return photos
    }
    fun load(uri: Uri,fullResolution: Boolean=false): Bitmap {
        val source=ImageDecoder.createSource(context.contentResolver,uri)
        return ImageDecoder.decodeBitmap(source) { decoder,info,_ ->
            val maxPixels=if(fullResolution) (Runtime.getRuntime().maxMemory()/3).coerceAtMost(160_000_000L)/4 else 4_194_304L
            var sample=1
            while(info.size.width.toLong()*info.size.height/(sample.toLong()*sample)>maxPixels) sample*=2
            decoder.setTargetSampleSize(sample); decoder.allocator=ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }
    fun details(photo: Photo): Details {
        val e=context.contentResolver.openInputStream(photo.uri)?.use { ExifInterface(it) }
        val comment=e?.getAttribute(ExifInterface.TAG_USER_COMMENT).orEmpty()
        val title=if(photo.isRaw) "RAW sensor capture · DNG" else comment.substringAfter("Recipe: ","Photo").substringBefore(" [").take(80)
        val iso=e?.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }
        val time=e?.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME,0.0)?.takeIf { it>0 }?.let { if(it<1) "1/${(1/it).roundToInt()}s" else "${"%.2f".format(it)}s" }
        val aperture=e?.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
        val focal=e?.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let { "${it}mm eq." }
        val bias=e?.getAttributeDouble(ExifInterface.TAG_EXPOSURE_BIAS_VALUE,Double.NaN)?.takeUnless { it.isNaN() }?.let { "EV ${"%+.1f".format(it)}" }
        val width=e?.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION,e.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,photo.width))?.takeIf { it>0 } ?: photo.width
        val height=e?.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION,e.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,photo.height))?.takeIf { it>0 } ?: photo.height
        return Details(title,"${if(photo.isRaw) "DNG · " else "JPEG · "}${"%.1f".format(width.toLong()*height/1e6)}MP · $width × $height",
            listOfNotNull(iso,time,aperture,focal,bias).joinToString(" · "),if(photo.isRaw) e?.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION).orEmpty().take(400) else comment.substringAfter(" | Source:","").take(400))
    }
    fun delete(photo: Photo) { check(context.contentResolver.delete(photo.uri,null,null)>0) { "Could not delete photo" } }
}
