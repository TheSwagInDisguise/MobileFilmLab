package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.processing.PixelFilters as P
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PixelFiltersTest {
    private fun grey(v: Int)=-0x1000000 or (v shl 16) or (v shl 8) or v
    @Test fun flatFieldIsNeverSharpenedIntoNoise() {
        val row=IntArray(64) { grey(127+it%2) }; val out=IntArray(64)
        P.sharpenRow(row,row,row,out,3,6400)
        assertArrayEquals(row,out)
    }
    @Test fun haloMagnitudeIsBoundedAndLuminanceOnly() {
        val row=IntArray(32) { grey(if(it<16) 60 else 190) }; val out=IntArray(32)
        P.sharpenRow(row,row,row,out,3,100)
        assertTrue(abs(P.luma(out[15])-P.luma(row[15]))<=14)
        assertTrue(P.luma(out[16])>=P.luma(row[16]))
        out.forEach { assertEquals(it ushr 16 and 255,it ushr 8 and 255); assertEquals(it ushr 8 and 255,it and 255) }
    }
    @Test fun grainIsSeededBoundedAndZeroCentred() {
        val a=IntArray(10000) { grey(128) }; val b=a.copyOf()
        P.grainRow(a,3,123); P.grainRow(b,3,123)
        assertArrayEquals(a,b)
        assertTrue(abs(a.map { P.luma(it)-128 }.average())<.5)
        assertTrue(a.all { abs(P.luma(it)-128)<=13 })
    }
    @Test fun grainOffDoesNotModifyPixels() { val a=intArrayOf(grey(90)); P.grainRow(a,0,5); assertEquals(grey(90),a[0]) }
}
