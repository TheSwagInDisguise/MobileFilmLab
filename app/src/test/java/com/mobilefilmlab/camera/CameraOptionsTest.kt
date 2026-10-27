package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.data.model.*
import org.junit.Assert.*
import org.junit.Test

class CameraOptionsTest {
    private fun camera(id: String, eq: Int, type: LensType = LensType.TELEPHOTO) = CameraSpec(id,true,false,
        focalLengthMm=7f,equivalentFocalLength35mm=eq,sensorWidthMm=10f,sensorHeightMm=8f,apertureFNumber=2f,
        activeArrayWidth=4000,activeArrayHeight=3000,maxResolutionMp=12f,hasRawSupport=false,lensType=type,displayName=id)

    @Test fun allCameraIdsRemainEvenWhenSeveralHaveTheSameLegacyLensType() {
        val cameras=listOf(camera("a",45),camera("b",60),camera("c",85),camera("d",105),camera("e",150))
        assertEquals(5,CameraOptions.rear(cameras).size)
        assertEquals(listOf("a","b","c","d","e"),CameraOptions.rear(cameras).map { it.cameraId })
    }
    @Test fun logicalAutoIsClearlyDistinguishedFromPhysicalOutput() {
        val logical=camera("0",23).copy(isLogicalMultiCamera=true,physicalCameraIds=listOf("1","2"))
        assertEquals("Auto · 23mm",CameraOptions.label(logical))
        assertTrue(CameraOptions.description(logical).contains("logical/automatic"))
        assertEquals(3,CameraOptions.rear(listOf(logical,camera("1",23),camera("2",70))).size)
    }
    @Test fun identicalFocalLengthsDoNotMergeDifferentHardwareIds() {
        assertEquals(2,CameraOptions.rear(listOf(camera("1",23),camera("2",23))).size)
        assertEquals(1,CameraOptions.rear(listOf(camera("1",23),camera("1",23))).size)
    }
    @Test fun focalLabelsUseReportedMetadataNotFixedZoomNames() {
        assertEquals("83mm",CameraOptions.label(camera("4",83)))
        assertEquals("7.0mm*",CameraOptions.label(camera("5",0)))
        assertEquals("ID unknown",CameraOptions.label(camera("unknown",0).copy(focalLengthMm=0f)))
    }
    @Test fun sortingIsNumericAndNonPhotoOrFrontRoutesAreNotRearPhotoButtons() {
        val cameras=listOf(camera("10",230),camera("2",14),camera("3",70),camera("depth",0).copy(hasJpegSupport=false),camera("front",23).copy(isBackFacing=false))
        assertEquals(listOf(14,70,230),CameraOptions.rear(cameras).map { it.equivalentFocalLength35mm })
        val rawOnly=camera("raw",30).copy(hasJpegSupport=false,hasRawSupport=true,rawSizes=listOf(StreamSize(4000,3000)))
        assertTrue(CameraOptions.rear(listOf(rawOnly)).contains(rawOnly))
    }
    @Test fun rawStreamSelectionKeepsAdvertisedNativeSizesWithinMemoryBudget() {
        val input=listOf(StreamSize(16000,12000),StreamSize(8192,6144),StreamSize(4000,3000),StreamSize(4000,3000),StreamSize(0,5))
        assertEquals(listOf(StreamSize(8192,6144),StreamSize(4000,3000)),RawStreamSelector.candidates(input))
    }
}
