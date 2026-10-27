package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.camera.PreviewGeometry as G
import org.junit.Assert.*
import org.junit.Test

class PreviewGeometryTest {
    @Test fun portrait90MapsTopLeftToSensorBottomLeft() {
        val p=G.mapToSensor(0f,0f,.75f,4f/3,90)
        assertEquals(0f,p.x,.001f); assertEquals(1f,p.y,.001f)
    }
    @Test fun portrait270MappingIsInverse() {
        val p=G.mapToSensor(0f,0f,.75f,4f/3,270)
        assertEquals(1f,p.x,.001f); assertEquals(0f,p.y,.001f)
    }
    @Test fun centreRemainsCentreForAllRotationsAndAspects() {
        for(r in listOf(0,90,180,270)) {
            val p=G.mapToSensor(.5f,.5f,.6f,4f/3,r)
            assertEquals(.5f,p.x,.001f); assertEquals(.5f,p.y,.001f)
        }
    }
    @Test fun centreFillCorrectsForNarrowView() {
        val p=G.mapToSensor(0f,0f,.5f,4f/3,90)
        assertEquals(0f,p.x,.001f); assertEquals(5f/6,p.y,.001f)
    }
    @Test fun outOfRangeTapClampsSafely() {
        val p=G.mapToSensor(-10f,10f,.75f,4f/3,90)
        assertTrue(p.x in 0f..1f && p.y in 0f..1f)
    }
}
