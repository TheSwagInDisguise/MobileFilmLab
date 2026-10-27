package com.mobilefilmlab.camera.processing

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Small, testable luminance operations. No AI/detail reconstruction claims. */
object PixelFilters {
    fun luma(c: Int): Int = (((c ushr 16 and 255) * 77 + (c ushr 8 and 255) * 150 + (c and 255) * 29) ushr 8)
    fun addLuma(c: Int, delta: Int): Int = (c and -0x1000000) or
        (((c ushr 16 and 255) + delta).coerceIn(0,255) shl 16) or
        (((c ushr 8 and 255) + delta).coerceIn(0,255) shl 8) or ((c and 255) + delta).coerceIn(0,255)

    /** Edge-adaptive 5-tap high pass, noise threshold and halo limiter. Mutates only the current row. */
    fun sharpenRow(top: IntArray, row: IntArray, bottom: IntArray, output: IntArray, strength: Int, iso: Int) {
        row.copyInto(output)
        val weight = strength.coerceIn(0,3) * 0.18f
        val noise = (2f + sqrt(iso.coerceAtLeast(50) / 100f) * 1.2f).coerceAtMost(18f)
        for (x in 1 until row.lastIndex) {
            val c = luma(row[x]); val a = luma(top[x]); val b = luma(bottom[x])
            val l = luma(row[x-1]); val r = luma(row[x+1])
            val high = c - (a+b+l+r)*0.25f
            val grad = (abs(l-r) + abs(a-b))*0.5f
            val edge = ((grad-noise)/(24f+noise)).coerceIn(0f,1f)
            val detail = (abs(high)-noise*0.5f).coerceAtLeast(0f) * if (high >= 0) 1 else -1
            val delta = (detail * weight * 4f * edge).coerceIn(-14f,14f).roundToInt()
            output[x] = addLuma(row[x], delta)
        }
    }
    /** Zero-centred, seeded luminance grain; no colour speckle; optional by user choice. */
    fun grainRow(pixels: IntArray, level: Int, startSeed: Int): Int {
        if (level <= 0) return startSeed
        val amp = when (level) { 1 -> 4f; 2 -> 8f; else -> 13f }
        var seed = startSeed
        for (i in pixels.indices) {
            seed = seed * 1664525 + 1013904223
            val u = (seed ushr 16 and 255) / 255f
            seed = seed * 1664525 + 1013904223
            val v = (seed ushr 16 and 255) / 255f
            val lum = luma(pixels[i]) / 255f
            val density = (4f * lum * (1f-lum)).coerceIn(0.15f,1f)
            pixels[i] = addLuma(pixels[i], ((u+v-1f)*amp*density).roundToInt())
        }
        return seed
    }
}
