package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.camera.CaptureOrientation as O
import org.junit.Assert.*
import org.junit.Test

class CaptureOrientationTest {
    @Test fun portraitUsesSensorRotation() { assertEquals(90,O.jpeg(90,0)); assertEquals(270,O.jpeg(270,0)) }
    @Test fun landscapeAndUpsideDownAreNotForcedToPortrait() {
        assertEquals(180,O.jpeg(90,90)); assertEquals(270,O.jpeg(90,180)); assertEquals(0,O.jpeg(90,270))
    }
    @Test fun orientationRoundsAndFrontDirectionIsReversed() {
        assertEquals(90,O.jpeg(90,20)); assertEquals(0,O.jpeg(90,90,true)); assertEquals(90,O.jpeg(90,-1))
    }
}
