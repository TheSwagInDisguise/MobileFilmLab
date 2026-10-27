package com.mobilefilmlab.camera.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream

object BitmapIO {
    fun dimensions(bytes: ByteArray): Pair<Int,Int> {
        val o=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,o)
        return o.outWidth to o.outHeight
    }
    fun decodeUpright(bytes: ByteArray, maxPixels: Long=55_000_000L): Bitmap {
        val (w,h)=dimensions(bytes)
        require(w>0 && h>0) { "JPEG could not be decoded" }
        var sample=1
        while(w.toLong()*h/(sample.toLong()*sample)>maxPixels) sample*=2
        val options=BitmapFactory.Options().apply { inPreferredConfig=Bitmap.Config.ARGB_8888; inSampleSize=sample; inMutable=true }
        val b=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: error("JPEG could not be decoded")
        val orientation=runCatching { ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val m=Matrix()
        when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f,1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f,1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f,1f) }
            else -> return b
        }
        val upright=Bitmap.createBitmap(b,0,0,b.width,b.height,m,true)
        if(upright !== b) b.recycle()
        return upright
    }
}
