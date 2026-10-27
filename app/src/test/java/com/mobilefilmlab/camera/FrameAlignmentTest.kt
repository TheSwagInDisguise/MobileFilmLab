package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.processing.FrameAlignment
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class FrameAlignmentTest {
    @Test fun recoversKnownTranslationAndExposureChange() {
        val w=96; val h=80; val random=Random(12)
        val base=IntArray(w*h) { 30+random.nextInt(150) }
        val shifted=IntArray(w*h)
        for(y in 0 until h) for(x in 0 until w) { val sy=y+2; val sx=x-3; if(sy in 0 until h && sx in 0 until w) shifted[sy*w+sx]=(base[y*w+x]*.65).toInt()+8 }
        val alignment=FrameAlignment.align(base,shifted,w,h,6)
        assertTrue(alignment.accepted); assertEquals(-3,alignment.dx); assertEquals(2,alignment.dy)
    }
    @Test fun flatSceneCannotInventAnAlignment() {
        val a=IntArray(96*80) { 120 }; assertFalse(FrameAlignment.align(a,a,96,80).accepted)
    }
    @Test fun unrelatedFramesAreRejected() {
        val r=Random(42); val a=IntArray(96*80) { r.nextInt(256) }; val b=IntArray(a.size) { r.nextInt(256) }
        assertFalse(FrameAlignment.align(a,b,96,80).accepted)
    }
}
