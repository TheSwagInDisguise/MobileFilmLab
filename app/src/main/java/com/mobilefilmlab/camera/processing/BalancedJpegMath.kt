package com.mobilefilmlab.camera.processing

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Conventional JPEG-domain filtering. No neural enhancement, invented detail or sensor-WB calibration. */
object BalancedJpegMath {
    data class Strength(val edgeThreshold: Int,val lumaBlend: Int,val chromaBlend: Int,val sharpen: Int)
    fun strength(iso: Int): Strength {
        val noise=sqrt(iso.coerceIn(50,25600)/100.0)
        return Strength((14+noise*5).roundToInt().coerceIn(18,72),
            ((noise-1.4)*18).roundToInt().coerceIn(0,100),
            (48+noise*20).roundToInt().coerceIn(64,180),if(iso<800) 1 else 0)
    }
    private fun channel(c: Int,shift: Int)=(c ushr shift and 255)
    private fun rgb(alpha: Int,r: Int,g: Int,b: Int)=alpha or (r.coerceIn(0,255) shl 16) or (g.coerceIn(0,255) shl 8) or b.coerceIn(0,255)

    /** Five-tap joint luma/chroma filter, O(width) scratch. Strong colour/luma boundaries are protected. */
    fun denoiseRow(top: IntArray,row: IntArray,bottom: IntArray,out: IntArray,s: Strength) {
        require(top.size==row.size && row.size==bottom.size && out.size==row.size)
        row.copyInto(out)
        for(x in 1 until row.lastIndex) {
            val c=row[x];val cy=PixelFilters.luma(c)
            val cr=channel(c,16);val cg=channel(c,8);val cb=channel(c,0)
            var rr=cr*512;var gg=cg*512;var bb=cb*512;var total=512
            // A fixed loop avoids captured-variable/closure allocations for every pixel.
            for(i in 0..3) {
                val p=when(i) {0->top[x];1->row[x-1];2->row[x+1];else->bottom[x]}
                val r=channel(p,16);val g=channel(p,8);val b=channel(p,0)
                val lum=abs(PixelFilters.luma(p)-cy)
                val chroma=maxOf(abs((r-g)-(cr-cg)),abs((b-g)-(cb-cg)))
                val lw=(256-lum*256/s.edgeThreshold).coerceIn(0,256)
                val cw=(256-chroma*256/(s.edgeThreshold*3)).coerceIn(0,256)
                val weight=lw*cw/256
                rr+=r*weight;gg+=g*weight;bb+=b*weight;total+=weight
            }
            val r=rr/total;val g=gg/total;val b=bb/total
            val ny=(r*77+g*150+b*29) ushr 8
            val dy=(ny-cy)*s.lumaBlend/256
            val du=((r-cr)-(ny-cy))*s.chromaBlend/256
            val dv=((b-cb)-(ny-cy))*s.chromaBlend/256
            val dr=dy+du;val db=dy+dv
            val dg=(dy*256-77*dr-29*db)/150
            out[x]=rgb(c and -0x1000000,cr+dr,cg+dg,cb+db)
        }
    }

    /** Mild, monotonic shadow/highlight balancing only when the sampled JPEG is high-contrast. */
    fun toneTable(histogram: IntArray,iso: Int): IntArray {
        require(histogram.size==256)
        val total=histogram.sum().coerceAtLeast(1)
        fun percentile(fraction: Double): Int {
            val target=(total*fraction).roundToInt().coerceAtLeast(1);var count=0
            for(i in histogram.indices) {count+=histogram[i];if(count>=target)return i}
            return 255
        }
        val highContrast=percentile(.05)<=48 && percentile(.95)>=208
        val amount=if(!highContrast) 0.0 else if(iso>=1600) 12.0 else 28.0
        var previous=0
        return IntArray(256) { value ->
            val t=value/255.0
            (value+amount*4*t*(1-t)*(.5-t)).roundToInt().coerceIn(previous,255).also {previous=it}
        }
    }
    /** Uniform RGB scaling preserves channel ratios; a saturated channel limits shadow lifting. */
    fun applyTone(pixel: Int,table: IntArray): Int {
        val y=PixelFilters.luma(pixel);if(y==0 || table[y]==y) return pixel
        val r=channel(pixel,16);val g=channel(pixel,8);val b=channel(pixel,0)
        val scale=(table[y]*256/y).coerceAtMost(255*256/maxOf(r,g,b,1))
        return rgb(pixel and -0x1000000,(r*scale+128)/256,(g*scale+128)/256,(b*scale+128)/256)
    }
}
