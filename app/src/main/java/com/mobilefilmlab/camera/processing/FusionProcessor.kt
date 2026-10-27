package com.mobilefilmlab.camera.processing

import android.graphics.Bitmap
import com.mobilefilmlab.camera.camera.CapturedJpeg
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

object FusionProcessor {
    data class Output(val bitmap: Bitmap, val note: String, val acceptedFrames: Int)
    fun fuse(frames: List<CapturedJpeg>): Output {
        require(frames.size==3)
        val bitmaps=mutableListOf<Bitmap>()
        try {
            frames.forEach { bitmaps+=BitmapIO.decodeUpright(it.bytes,12_600_000L) }
            val base=bitmaps[1]
            val width=base.width; val height=base.height
            if(bitmaps.any { it.width!=width || it.height!=height }) {
                bitmaps.filter { it !== base }.forEach { it.recycle() }
                return Output(base,"Fusion skipped: frame dimensions differed",1)
            }
            val tw=160; val th=(height*tw.toFloat()/width).roundToInt().coerceAtLeast(32)
            fun thumbnail(b: Bitmap): IntArray {
                val tiny=Bitmap.createScaledBitmap(b,tw,th,true)
                val a=IntArray(tw*th); tiny.getPixels(a,0,tw,0,0,tw,th)
                if(tiny !== b) tiny.recycle()
                return IntArray(a.size) { PixelFilters.luma(a[it]) }
            }
            val reference=thumbnail(base)
            val alignments=bitmaps.mapIndexed { i,b -> if(i==1) FrameAlignment.Alignment(0,0,0.0,true) else FrameAlignment.align(reference,thumbnail(b),tw,th) }
            val shifts=alignments.map { ((it.dx*width.toFloat()/tw).roundToInt()) to ((it.dy*height.toFloat()/th).roundToInt()) }
            val valid=alignments.mapIndexedNotNull { i,a -> if(a.accepted) i else null }
            if(valid.size==1) {
                bitmaps.filter { it !== base }.forEach { it.recycle() }
                return Output(base,"Fusion rejected: motion/low texture; middle frame used",1)
            }
            val refMeta=frames[1].metadata
            fun exposure(i: Int): Double? {
                val m=frames[i].metadata
                return if(m.iso != null && m.exposureTimeNs != null) m.iso.toDouble()*m.exposureTimeNs else null
            }
            val baseExposure=exposure(1)
            val normalizers=frames.indices.map { i ->
                val e=exposure(i)
                if(e != null && e>0 && baseExposure != null) (baseExposure/e).coerceIn(0.25,4.0) else when(i) { 0->2.0; 2->0.5; else->1.0 }
            }
            val linear=Array(3) { i -> FloatArray(256) { v -> ((v/255.0).pow(2.2)*normalizers[i]).coerceIn(0.0,1.0).toFloat() } }
            val gamma=IntArray(4097) { ((it/4096.0).pow(1.0/2.2)*255).roundToInt().coerceIn(0,255) }
            val out=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
            val rows=Array(3) { IntArray(width) }; val dest=IntArray(width)
            val threshold=(18f+kotlin.math.sqrt((refMeta.iso ?: 100)/100f)*2f).coerceAtMost(40f)
            for(y in 0 until height) {
                for(i in valid) {
                    val sy=(y+shifts[i].second).coerceIn(0,height-1)
                    bitmaps[i].getPixels(rows[i],0,width,0,sy,width,1)
                }
                for(x in 0 until width) {
                    val c=rows[1][x]
                    val br=c ushr 16 and 255; val bg=c ushr 8 and 255; val bb=c and 255
                    val baseL=PixelFilters.luma(c)
                    var rr=linear[1][br]; var gg=linear[1][bg]; var bbb=linear[1][bb]; var weight=1f
                    for(i in valid) {
                        if(i==1) continue
                        val xx=x+shifts[i].first; val yy=y+shifts[i].second
                        if(xx !in 0 until width || yy !in 0 until height) continue
                        val p=rows[i][xx]; val r=p ushr 16 and 255; val g=p ushr 8 and 255; val b=p and 255
                        if(maxOf(r,g,b)>247 || maxOf(r,g,b)<8) continue
                        val normalizedL=gamma[((linear[i][r]*0.299f+linear[i][g]*0.587f+linear[i][b]*0.114f)*4096).roundToInt().coerceIn(0,4096)]
                        // Per-pixel reference protection avoids averaging obvious moving subjects into ghosts.
                        val diff=abs(normalizedL-baseL)
                        if(diff>threshold) continue
                        val w=(1f-diff/threshold)*0.8f
                        rr+=linear[i][r]*w; gg+=linear[i][g]*w; bbb+=linear[i][b]*w; weight+=w
                    }
                    fun encoded(v: Float)=gamma[((v/weight)*4096).roundToInt().coerceIn(0,4096)]
                    dest[x]=-0x1000000 or (encoded(rr) shl 16) or (encoded(gg) shl 8) or encoded(bbb)
                }
                out.setPixels(dest,0,width,0,y,width,1)
            }
            bitmaps.forEach { it.recycle() }
            return Output(out,"${valid.size}/3 registered frames; translation-only, motion rejection; JPEG-domain fusion",valid.size)
        } catch(e: Throwable) { bitmaps.filter { !it.isRecycled }.forEach { it.recycle() }; throw e }
    }
}
