package com.mobilefilmlab.camera.processing.hdr

import com.mobilefilmlab.camera.processing.FusionMath
import kotlin.math.*

/** Original conventional robust estimator; noise is a JPEG-domain proxy, not measured RAW sensor noise. */
object HdrMergeMath {
    val linear=FloatArray(256) {FusionMath.toLinear(it).toFloat()}
    val gamma=IntArray(8193) {FusionMath.toSrgb(it/8192.0)}
    fun encoded(v: Float)=gamma[(v*8192).roundToInt().coerceIn(0,8192)]
    fun sigma(luma: Float,iso: Int,normalizer: Float): Float =
        (.005f+.007f*sqrt(iso.coerceIn(50,25600)/100f)*sqrt((luma/normalizer).coerceIn(.015f,1f)))*normalizer
    /** Algebraically equivalent to sigma(); precomputes fixed per-frame factors outside pixel loops. */
    class Noise(iso: Int,gain: Float) {
        private val read=.005f*gain
        private val shot=.007f*sqrt(iso.coerceIn(50,25600)/100f*gain)
        private val minimum=sqrt(.015f*gain)
        private val maximum=sqrt(gain)
        fun atRootLuma(rootLuma: Float)=read+shot*rootLuma.coerceIn(minimum,maximum)
    }
    fun weight(r: Float,g: Float,b: Float,ar: Float,ag: Float,ab: Float,sigma: Float,confidence: Float): Float {
        if(!confidence.isFinite() || !sigma.isFinite() || confidence<=0f || sigma<=0f) return 0f
        val dl=(ar-r)*.2126f+(ag-g)*.7152f+(ab-b)*.0722f
        val chroma=maxOf(abs(ar-r-dl),abs(ag-g-dl),abs(ab-b-dl))
        val z=max(abs(dl)/sigma,chroma/(sigma*1.5f))
        if(z>=3.2f) return 0f
        val s=1-z*z/(3.2f*3.2f)
        return s*s*confidence
    }
    /** Memory for strip scratch scales with frame count/width, not full image height. */
    fun stripRows(width: Int,frames: Int,margin: Int,heap: Long): Int {
        require(width>0 && frames>0)
        val budget=(heap/12).coerceIn(6L*1024*1024,32L*1024*1024)
        return (budget/(width.toLong()*4*frames)-margin*2-3).toInt().coerceIn(32,384)
    }
    fun toneScale(peak: Float,luma: Float,localBase: Float,white: Double,quality: Boolean,support: Float): Float {
        if(peak<=0f) return 1f
        val global=(FusionMath.displayTone(peak.toDouble(),white)/peak).toFloat()
        val shadow=(1-luma/.18f).coerceIn(0f,1f)
        val local=(1-localBase/.22f).coerceIn(0f,1f)
        val lift=if(white>1.05) (if(quality) .18f else .11f)*shadow*local*support.coerceIn(0f,1f) else 0f
        return (global*(1+lift)).coerceAtMost(1f/peak)
    }
}
