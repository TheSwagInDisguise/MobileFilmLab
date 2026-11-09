package com.mobilefilmlab.camera.processing

import kotlin.math.pow
import kotlin.math.roundToInt

/** IEC sRGB transfer and bounded display mapping for an estimate made from processed JPEGs, not RAW. */
object FusionMath {
    fun toLinear(value: Int): Double {
        val x=value.coerceIn(0,255)/255.0
        return if(x<=.04045) x/12.92 else ((x+.055)/1.055).pow(2.4)
    }
    fun toSrgb(value: Double): Int {
        val x=value.coerceIn(0.0,1.0)
        return ((if(x<=.0031308) 12.92*x else 1.055*x.pow(1/2.4)-.055)*255).roundToInt().coerceIn(0,255)
    }
    /** Missing/invalid exposure data is a reason to use the middle frame, never to guess an EV. */
    fun normalizers(exposures: List<Double?>): DoubleArray? {
        if(exposures.size!=3 || exposures.any { it==null || !it.isFinite() || it<=0 }) return null
        val middle=exposures[1]!!
        val ratios=DoubleArray(3) {middle/exposures[it]!!}
        return ratios.takeIf { values -> values.all {it.isFinite() && it in .125..8.0} }
    }
    /** Identity without recovered highlights; anchors 18% and maps the recovered white point to one. */
    fun displayTone(value: Double,white: Double): Double {
        val x=value.coerceAtLeast(0.0);val w=white.coerceIn(1.0,8.0)
        if(w<=1.0001) return x.coerceAtMost(1.0)
        val a=(w-.18)/(w-1);val b=w*(1-.18)/(w-1)
        return (a*x/(x+b)).coerceIn(0.0,1.0)
    }
}
