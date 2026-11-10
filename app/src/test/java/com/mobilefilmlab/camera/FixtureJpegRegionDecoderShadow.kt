package com.mobilefilmlab.camera

import android.graphics.*
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

/**
 * Test-only codec adapter: Robolectric 4.12's BitmapRegionDecoder shadow returns blank bitmaps,
 * even with native graphics. Decode small synthetic fixtures through native BitmapFactory then
 * crop/sample their pixels, so HDR math/strip geometry really see JPEG data. This is NOT evidence
 * of the device's region decoder; actual Android region/Camera2 performance still needs a handset.
 * No test source or synthetic photo is included in the APK.
 */
@Implements(BitmapRegionDecoder::class)
class FixtureJpegRegionDecoderShadow {
    private var data=ByteArray(0)
    private var width=0
    private var height=0
    @Implementation fun getWidth()=width
    @Implementation fun getHeight()=height
    @Implementation fun recycle() {data=ByteArray(0)}
    @Implementation fun decodeRegion(rect: Rect,options: BitmapFactory.Options): Bitmap {
        require(width.toLong()*height<=1_000_000) {"Test adapter is only for small synthetic fixtures"}
        val opts=BitmapFactory.Options().apply {inPreferredConfig=Bitmap.Config.ARGB_8888;inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB)}
        val full=BitmapFactory.decodeByteArray(data,0,data.size,opts) ?: error("Fixture JPEG decode failed")
        var crop=Bitmap.createBitmap(full,rect.left,rect.top,rect.width(),rect.height())
        if(crop!==full) full.recycle()
        val sample=options.inSampleSize.coerceAtLeast(1)
        if(sample>1) {
            val scaled=Bitmap.createScaledBitmap(crop,(rect.width()+sample-1)/sample,(rect.height()+sample-1)/sample,true)
            if(scaled!==crop) crop.recycle();crop=scaled
        }
        return crop
    }
    companion object {
        @JvmStatic @Implementation fun newInstance(bytes: ByteArray,offset: Int,length: Int,shareable: Boolean): BitmapRegionDecoder {
            val decoder=ReflectionHelpers.callConstructor(BitmapRegionDecoder::class.java,ReflectionHelpers.ClassParameter.from(Long::class.javaPrimitiveType,0L))
            val shadow=Shadow.extract<FixtureJpegRegionDecoderShadow>(decoder)
            shadow.data=bytes.copyOfRange(offset,offset+length)
            val opts=BitmapFactory.Options().apply {inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(shadow.data,0,shadow.data.size,opts)
            shadow.width=opts.outWidth;shadow.height=opts.outHeight
            return decoder
        }
    }
}
