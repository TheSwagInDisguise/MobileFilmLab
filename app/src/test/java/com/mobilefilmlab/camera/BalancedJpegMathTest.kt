package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.processing.BalancedJpegMath as B
import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import kotlin.math.abs

class BalancedJpegMathTest {
    private fun rgb(r: Int,g: Int,b: Int)=-0x1000000 or (r.coerceIn(0,255) shl 16) or (g.coerceIn(0,255) shl 8) or b.coerceIn(0,255)
    @Test fun flatColoursAndAlphaAreExactlyPreservedWithoutInventedCalibration() {
        for(iso in listOf(50,100,800,6400,25600)) for(c in listOf(rgb(0,0,0),rgb(255,255,255),rgb(127,127,127),rgb(30,110,190))) {
            val row=IntArray(9) {c};val out=IntArray(9)
            B.denoiseRow(row,row,row,out,B.strength(iso));assertArrayEquals(row,out)
        }
    }
    @Test fun deterministicHighIsoChromaNoiseIsReducedWithoutAnExcessiveMeanShift() {
        val w=120;val h=80;val random=Random(34)
        val pixels=Array(h) {IntArray(w) {rgb(128+random.nextInt(35)-17,128+random.nextInt(19)-9,128+random.nextInt(35)-17)}}
        var before=0.0;var after=0.0;var meanShift=0.0;var n=0
        for(y in 1 until h-1) {
            val out=IntArray(w);B.denoiseRow(pixels[y-1],pixels[y],pixels[y+1],out,B.strength(3200))
            for(x in 1 until w-1) {
                fun error(c: Int): Int {val r=c ushr 16 and 255;val g=c ushr 8 and 255;val b=c and 255;return (r-g)*(r-g)+(b-g)*(b-g)}
                before+=error(pixels[y][x]);after+=error(out[x])
                meanShift+=(out[x] ushr 16 and 255)-(pixels[y][x] ushr 16 and 255);n++
            }
        }
        assertTrue("Chroma variance $before -> $after",after<before*.9)
        assertTrue(abs(meanShift/n)<1.5)
    }
    @Test fun sharpLumaAndEqualLumaColourBoundariesAreProtected() {
        for(pair in listOf(rgb(20,20,20) to rgb(220,220,220),rgb(160,0,0) to rgb(0,82,0))) {
            val row=IntArray(10) {if(it<5) pair.first else pair.second};val out=IntArray(10)
            B.denoiseRow(row,row,row,out,B.strength(3200));assertArrayEquals(row,out)
        }
    }
    @Test fun adaptiveToneIsMonotonicBoundedAndAnchorsBlackAndWhite() {
        val histogram=IntArray(256).apply {this[10]=50;this[240]=50}
        val table=B.toneTable(histogram,100)
        assertEquals(0,table.first());assertEquals(255,table.last())
        assertTrue(table.toList().zipWithNext().all {it.first<=it.second})
        assertTrue(table[32]>32);assertTrue(table[224]<224)
        assertTrue(B.toneTable(histogram,6400)[32]<table[32])
    }
    @Test fun lowContrastScenesGetNoToneCurveAndHighIsoGetsNoAddedSharpening() {
        val histogram=IntArray(256).apply {this[127]=500}
        assertArrayEquals(IntArray(256) {it},B.toneTable(histogram,100))
        assertEquals(0,B.strength(1600).sharpen)
        assertTrue(B.strength(6400).chromaBlend>B.strength(100).chromaBlend)
    }
    @Test fun tonePreservesNeutralGreyAndDoesNotWrapSaturatedChannels() {
        val histogram=IntArray(256).apply {this[8]=100;this[248]=100};val table=B.toneTable(histogram,100)
        val grey=B.applyTone(rgb(32,32,32),table)
        assertEquals(grey ushr 16 and 255,grey ushr 8 and 255);assertEquals(grey and 255,grey ushr 8 and 255)
        assertEquals(-0x1000000,grey and -0x1000000)
        val colour=B.applyTone(rgb(250,25,25),table)
        assertTrue((colour ushr 16 and 255)>=250);assertEquals(colour ushr 8 and 255,colour and 255)
    }
}
