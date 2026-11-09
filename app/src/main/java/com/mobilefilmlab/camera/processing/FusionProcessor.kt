package com.mobilefilmlab.camera.processing

import android.graphics.Bitmap
import com.mobilefilmlab.camera.camera.CapturedJpeg
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object FusionProcessor {
    data class Output(val bitmap: Bitmap, val note: String, val acceptedFrames: Int)
    fun fuse(frames: List<CapturedJpeg>): Output {
        require(frames.size==3)
        val bitmaps=mutableListOf<Bitmap>()
        try {
            frames.forEach { bitmaps+=BitmapIO.decodeUpright(it.bytes,12_600_000L) }
            var base=bitmaps[1]
            fun middleOnly(reason: String): Output {
                bitmaps.filter { it !== base }.forEach { it.recycle() }
                return Output(base,"Fusion skipped: $reason; middle frame used",1)
            }
            val width=base.width;val height=base.height
            if(bitmaps.any {it.width!=width || it.height!=height}) return middleOnly("frame dimensions differed")
            val normalizers=FusionMath.normalizers(frames.map {
                val m=it.metadata
                if(m.iso!=null && m.iso>0 && m.exposureTimeNs!=null && m.exposureTimeNs>0) m.iso.toDouble()*m.exposureTimeNs else null
            }) ?: return middleOnly("missing/unsafe exposure metadata")
            val tw=minOf(160,width);val th=(height*tw.toFloat()/width).roundToInt().coerceAtLeast(1)
            fun thumbnail(b: Bitmap): IntArray {
                val tiny=Bitmap.createScaledBitmap(b,tw,th,true)
                val a=IntArray(tw*th);tiny.getPixels(a,0,tw,0,0,tw,th)
                if(tiny !== b) tiny.recycle()
                return a
            }
            val thumbs=bitmaps.map(::thumbnail)
            val reference=IntArray(tw*th) {PixelFilters.luma(thumbs[1][it])}
            val alignments=thumbs.mapIndexed {i,a -> if(i==1) FrameAlignment.Alignment(0,0,0.0,true)
                else FrameAlignment.align(reference,IntArray(a.size) {PixelFilters.luma(a[it])},tw,th)}
            val valid=alignments.mapIndexedNotNull {i,a -> if(a.accepted) i else null}
            if(valid.size==1) return middleOnly("motion/low texture prevented registration")
            val shifts=alignments.map { ((it.dx*width.toFloat()/tw).roundToInt()) to ((it.dy*height.toFloat()/th).roundToInt()) }
            // Never clamp the exposure-normalised radiance before merging: doing so destroys HDR headroom.
            val linear=Array(3) {i -> FloatArray(256) {v -> (FusionMath.toLinear(v)*normalizers[i]).toFloat()} }
            val gamma=IntArray(4097) {FusionMath.toSrgb(it/4096.0)}
            val exposureWeights=FloatArray(3) { (1/sqrt(normalizers[it].coerceAtLeast(1.0))).toFloat() }
            fun encoded(v: Float)=gamma[(v*4096).roundToInt().coerceIn(0,4096)]
            // Only reliable, registered darker exposures can establish a recovered highlight white point.
            val highlights=IntArray(513);var recovered=0
            for(i in valid) if(normalizers[i]>1.05) {
                val alignment=alignments[i]
                for(y in 0 until th) for(x in 0 until tw) {
                    val xx=x+alignment.dx;val yy=y+alignment.dy
                    if(xx !in 0 until tw || yy !in 0 until th) continue
                    val c=thumbs[1][y*tw+x];val p=thumbs[i][yy*tw+xx]
                    val br=c ushr 16 and 255;val bg=c ushr 8 and 255;val bb=c and 255
                    val r=p ushr 16 and 255;val g=p ushr 8 and 255;val b=p and 255
                    if(maxOf(br,bg,bb)<248 || maxOf(r,g,b) !in 12..246) continue
                    val value=maxOf(linear[i][r],linear[i][g],linear[i][b])
                    val yBase=linear[1][br]*.2126f+linear[1][bg]*.7152f+linear[1][bb]*.0722f
                    val yOther=linear[i][r]*.2126f+linear[i][g]*.7152f+linear[i][b]*.0722f
                    if(value<=1.03f || yOther<yBase*.65f) continue
                    highlights[(value*64).roundToInt().coerceIn(0,512)]++;recovered++
                }
            }
            var white=1.0
            if(recovered>=maxOf(8,tw*th/500)) {
                var count=0
                for(index in highlights.indices) {count+=highlights[index];if(count>=recovered*.98) {white=(index/64.0).coerceIn(1.0,8.0);break}}
            }
            // Look up a common RGB scale based on the largest channel, preserving hue and channel headroom.
            val toneScale=FloatArray(32769) { i -> val v=i/4096.0;if(i==0) 1f else (FusionMath.displayTone(v,white)/v).toFloat() }
            if(!base.isMutable) {
                val mutable=base.copy(Bitmap.Config.ARGB_8888,true) ?: error("Cannot allocate writable fusion pixels")
                base.recycle();bitmaps[1]=mutable;base=mutable
            }
            // Reuse the middle bitmap: three inputs + scanlines, rather than allocating a fourth image.
            val rows=Array(3) {IntArray(width)};val dest=IntArray(width)
            val threshold=(18f+sqrt((frames[1].metadata.iso ?: 100).coerceAtLeast(50)/100f)*2f).coerceAtMost(40f)
            for(y in 0 until height) {
                for(i in valid) {
                    val sy=(y+shifts[i].second).coerceIn(0,height-1)
                    bitmaps[i].getPixels(rows[i],0,width,0,sy,width,1)
                }
                for(x in 0 until width) {
                    val c=rows[1][x];val br=c ushr 16 and 255;val bg=c ushr 8 and 255;val bb=c and 255
                    val baseL=PixelFilters.luma(c);val baseMax=maxOf(br,bg,bb)
                    var weight=if(baseMax>=248) .15f else if(baseMax<8) .45f else 1f
                    var rr=linear[1][br]*weight;var gg=linear[1][bg]*weight;var blue=linear[1][bb]*weight
                    for(i in valid) {
                        if(i==1) continue
                        val xx=x+shifts[i].first;val yy=y+shifts[i].second
                        if(xx !in 0 until width || yy !in 0 until height) continue
                        val p=rows[i][xx];val r=p ushr 16 and 255;val g=p ushr 8 and 255;val b=p and 255
                        if(maxOf(r,g,b) !in 8..247) continue
                        val nr=encoded(linear[i][r]);val ng=encoded(linear[i][g]);val nb=encoded(linear[i][b])
                        val normalizedL=(nr*77+ng*150+nb*29) ushr 8
                        val diff=abs(normalizedL-baseL).toFloat()
                        val colourDiff=maxOf(abs(nr-br),abs(ng-bg),abs(nb-bb)).toFloat()
                        val confidence: Float
                        if(baseMax in 8..247) {
                            // RGB as well as luma rejects moving subjects with similar brightness/different hue.
                            if(diff>threshold || colourDiff>threshold*1.5f) continue
                            confidence=1f-maxOf(diff/threshold,colourDiff/(threshold*1.5f))
                        } else {
                            val luma=linear[i][r]*.2126f+linear[i][g]*.7152f+linear[i][b]*.0722f
                            val referenceLuma=linear[1][br]*.2126f+linear[1][bg]*.7152f+linear[1][bb]*.0722f
                            if(baseMax>=248 && luma<referenceLuma*.65f || baseMax<8 && luma>.08f) continue
                            if(diff>threshold*1.5f) continue
                            confidence=.8f
                        }
                        val w=confidence*.85f*exposureWeights[i]
                        rr+=linear[i][r]*w;gg+=linear[i][g]*w;blue+=linear[i][b]*w;weight+=w
                    }
                    rr/=weight;gg/=weight;blue/=weight
                    val largest=maxOf(rr,gg,blue)
                    val scale=toneScale[(largest*4096).roundToInt().coerceIn(0,32768)]
                    dest[x]=-0x1000000 or (encoded(rr*scale) shl 16) or (encoded(gg*scale) shl 8) or encoded(blue*scale)
                }
                base.setPixels(dest,0,width,0,y,width,1)
            }
            bitmaps.filter {it !== base}.forEach {it.recycle()}
            val detail=if(white>1.03) "; recovered-highlight tone mapping" else ""
            return Output(base,"${valid.size}/3 registered frames; exposure metadata + RGB motion rejection; sRGB JPEG-domain fusion$detail",valid.size)
        } catch(e: Throwable) {bitmaps.filter {!it.isRecycled}.forEach {it.recycle()};throw e}
    }
}
