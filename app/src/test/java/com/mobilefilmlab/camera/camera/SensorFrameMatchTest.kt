package com.mobilefilmlab.camera.camera

import org.junit.Assert.*
import org.junit.Test

class SensorFrameMatchTest {
    @Test fun pairedFormatsRequireOneSensorTimestamp() { assertTrue(SensorFrameMatch.matches(123,123,123,true)) }
    @Test fun mismatchedJpegCannotBecomeARawJpegPair() { assertFalse(SensorFrameMatch.matches(123,123,124,true)) }
    @Test fun missingOrMismatchedMetadataCannotProduceADng() { assertFalse(SensorFrameMatch.matches(123,null,123,true));assertFalse(SensorFrameMatch.matches(123,125,123,true)) }
    @Test fun rawOnlyDoesNotRequireOrFabricateAJpeg() { assertTrue(SensorFrameMatch.matches(123,123,null,false)) }
    @Test fun invalidTimestampsAreRejected() { assertFalse(SensorFrameMatch.matches(0,0,0,true));assertFalse(SensorFrameMatch.matches(-1,-1,null,false)) }
}
