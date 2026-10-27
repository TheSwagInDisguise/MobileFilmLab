package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.data.model.*
import com.mobilefilmlab.camera.settings.*
import org.junit.Assert.*
import org.junit.Test

class CaptureFormatPolicyTest {
    private val camera=CameraSpec("0",true,false,focalLengthMm=6f,equivalentFocalLength35mm=23,sensorWidthMm=10f,sensorHeightMm=8f,
        apertureFNumber=1.8f,activeArrayWidth=4000,activeArrayHeight=3000,maxResolutionMp=12f,hasRawSupport=true,
        lensType=LensType.WIDE,displayName="test",rawSizes=listOf(StreamSize(4000,3000)))
    @Test fun savedFileModesAreDistinct() {
        assertTrue(CaptureFormat.JPEG.writesJpeg);assertFalse(CaptureFormat.JPEG.writesRaw)
        assertTrue(CaptureFormat.RAW_JPEG.writesJpeg);assertTrue(CaptureFormat.RAW_JPEG.writesRaw)
        assertFalse(CaptureFormat.RAW.writesJpeg);assertTrue(CaptureFormat.RAW.writesRaw)
    }
    @Test fun rawModesForceOneNativeStandardExposureButDoNotInventAJpegOnlyFallback() {
        for(format in listOf(CaptureFormat.RAW,CaptureFormat.RAW_JPEG)) {
            val value=CaptureFormatPolicy.normalize(CaptureSettings(format=format,pipeline=CapturePipeline.OEM_NIGHT,zsl=true,outputSize=OutputSize.UPSCALE_28))
            assertEquals(format,value.format);assertEquals(CapturePipeline.STANDARD,value.pipeline);assertFalse(value.zsl);assertEquals(OutputSize.NATIVE,value.outputSize)
        }
    }
    @Test fun jpegKeepsExperimentalAndEditingOptions() {
        val value=CaptureSettings(format=CaptureFormat.JPEG,pipeline=CapturePipeline.BURST,zsl=true,grain=2,outputSize=OutputSize.UPSCALE_28)
        assertEquals(value,CaptureFormatPolicy.normalize(value))
    }
    @Test fun unsupportedRawIsNotAdvertisedAsAvailable() {
        assertTrue(CaptureFormatPolicy.supported(CaptureFormat.RAW,camera))
        assertFalse(CaptureFormatPolicy.supported(CaptureFormat.RAW,camera.copy(hasRawSupport=false,rawSizes=emptyList())))
        assertFalse(CaptureFormatPolicy.supported(CaptureFormat.RAW_JPEG,null))
    }
    @Test fun rawOnlyCanWorkWithoutAJpegStreamButRawJpegCannot() {
        val rawOnly=camera.copy(hasJpegSupport=false)
        assertTrue(CaptureFormatPolicy.supported(CaptureFormat.RAW,rawOnly))
        assertFalse(CaptureFormatPolicy.supported(CaptureFormat.RAW_JPEG,rawOnly))
        assertFalse(CaptureFormatPolicy.supported(CaptureFormat.JPEG,rawOnly))
    }
    @Test fun rawCropAndPostProcessedPipelinesAreDisabled() {
        assertFalse(CaptureFormatPolicy.allowsCrop(CaptureFormat.RAW));assertFalse(CaptureFormatPolicy.allowsCrop(CaptureFormat.RAW_JPEG))
        assertTrue(CaptureFormatPolicy.allowsCrop(CaptureFormat.JPEG))
        assertFalse(CaptureFormatPolicy.allowsPipeline(CaptureFormat.RAW,CapturePipeline.BURST))
        assertTrue(CaptureFormatPolicy.allowsPipeline(CaptureFormat.RAW,CapturePipeline.STANDARD))
    }
}
