package com.mobilefilmlab.camera

import com.mobilefilmlab.camera.processing.hdr.*
import com.mobilefilmlab.camera.camera.NativeCapturePolicy
import com.mobilefilmlab.camera.camera.ResolutionSelector
import com.mobilefilmlab.camera.data.model.DeviceProfile
import com.mobilefilmlab.camera.settings.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class HdrMathAndPolicyTest {
    @Test fun missingOrUnsupportedActualExposureCannotInventAnHdrPlan() {
        val c=proTestCamera()
        assertNull(HdrCapturePolicy.plan(c.copy(manualSensor=false),100,10_000_000,false,null))
        assertNull(HdrCapturePolicy.plan(c,null,10_000_000,false,null))
        assertNull(HdrCapturePolicy.plan(c,0,0,false,null))
    }
    @Test fun handheldKeepsConstantShortExposuresAndNeverAddsLongFrames() {
        val scene=HdrSceneStats(1,.04,255,.4,.08)
        val plan=HdrCapturePolicy.plan(proTestCamera(),1600,50_000_000,false,scene)!!
        assertEquals(5,plan.helpers.size);assertTrue(plan.helpers.distinct().size==1)
        assertTrue(plan.helpers.all {!it.longFrame && it.timeNs<=25_000_000})
        assertEquals(1.25,plan.highlightStops,0.0)
        assertTrue(plan.helpers.first().iso*plan.helpers.first().timeNs.toDouble()<1600*50_000_000.0)
    }
    @Test fun brightQuietDaylightAdaptsToThreeHelpersButHighIsoKeepsFive() {
        val scene=HdrSceneStats(1,0.0,200,.05,.01)
        assertEquals(3,HdrCapturePolicy.plan(proTestCamera(),100,5_000_000,false,scene)!!.helpers.size)
        assertEquals(5,HdrCapturePolicy.plan(proTestCamera(),1600,5_000_000,false,scene)!!.helpers.size)
        assertEquals(0.0,HdrCapturePolicy.plan(proTestCamera(),100,5_000_000,false,scene)!!.highlightStops,0.0)
    }
    @Test fun qualityUsesSevenShortsAndTwoLongsOnlyForQuietScenes() {
        val scene=HdrSceneStats(1,.03,255,.5,.005)
        val quiet=HdrCapturePolicy.plan(proTestCamera(),800,50_000_000,true,scene)!!
        assertEquals(7,quiet.helpers.count {!it.longFrame});assertEquals(2,quiet.helpers.count {it.longFrame})
        assertTrue(quiet.helpers.filter {it.longFrame}.all {it.timeNs<=250_000_000})
        val moving=HdrCapturePolicy.plan(proTestCamera(),800,50_000_000,true,scene.copy(motionScore=.2))!!
        assertEquals(7,moving.helpers.size);assertTrue(moving.helpers.none {it.longFrame})
    }
    @Test fun extremeMeteredExposuresRespectRealSensorLimitsAndSafeGain() {
        for(iso in listOf(100,6400)) for(t in listOf(125_000L,30_000_000_000L)) {
            val plan=HdrCapturePolicy.plan(proTestCamera(),iso,t,true,null) ?: continue
            for(e in plan.helpers) {
                assertTrue(e.iso in 100..6400);assertTrue(e.timeNs in 125_000..30_000_000_000)
                assertTrue(iso.toDouble()*t/(e.iso.toDouble()*e.timeNs) in .125..8.0)
            }
        }
    }
    @Test fun rawModesNormalizeEveryHdrSelectionToOneNativeExposure() {
        for(format in listOf(CaptureFormat.RAW,CaptureFormat.RAW_JPEG)) for(p in CapturePipeline.values()) {
            val s=CaptureFormatPolicy.normalize(CaptureSettings(format=format,pipeline=p,zsl=true,outputSize=OutputSize.UPSCALE_28))
            assertEquals(CapturePipeline.STANDARD,s.pipeline);assertFalse(s.zsl);assertEquals(OutputSize.NATIVE,s.outputSize)
        }
        assertFalse(CaptureFormatPolicy.normalize(CaptureSettings(pipeline=CapturePipeline.SMART_HDR,zsl=true)).zsl)
    }
    @Test fun hdrModesUseBoundedAdvertisedStreamsEvenWhenMaximumWasRequested() {
        val sizes=listOf(ResolutionSelector.Candidate(8160,6120),ResolutionSelector.Candidate(4000,3000),ResolutionSelector.Candidate(1920,1080))
        for(p in listOf(CapturePipeline.SMART_HDR,CapturePipeline.QUALITY_HDR)) {
            val a=NativeCapturePolicy.attempts(sizes,CaptureSettings(pipeline=p,captureResolution=CaptureResolution.MAXIMUM_NATIVE),DeviceProfile.GENERIC)
            assertEquals(4000,a.first().width);assertTrue(a.all {it.area<=12_600_000})
        }
    }
    @Test fun rgbOutlierGateRejectsEqualLumaMovingColourAndNonfiniteConfidenceCannotHelp() {
        assertTrue(HdrMergeMath.weight(.1f,.1f,.1f,.1f,.1f,.1f,.01f,1f)>.99f)
        assertEquals(0f,HdrMergeMath.weight(.5f,0f,0f,0f,.1486f,0f,.01f,1f),0f)
        assertEquals(0f,HdrMergeMath.weight(.1f,.1f,.1f,.1f,.1f,.1f,.01f,0f),0f)
        assertEquals(0f,HdrMergeMath.weight(.1f,.1f,.1f,.1f,.1f,.1f,.01f,Float.NaN),0f)
        assertTrue(HdrMergeMath.sigma(.1f,6400,2f)>HdrMergeMath.sigma(.1f,100,2f))
    }
    @Test fun gammaLookupIsBoundedMonotonicAndRoundTripsPhotographicCodes() {
        assertEquals(0,HdrMergeMath.encoded(0f));assertEquals(255,HdrMergeMath.encoded(10f))
        for(v in 10..255) assertTrue(abs(v-HdrMergeMath.encoded(HdrMergeMath.linear[v]))<=1)
        assertTrue(HdrMergeMath.gamma.toList().zipWithNext().all {it.first<=it.second})
        assertTrue(HdrMergeMath.stripRows(4000,10,12,256L*1024*1024) in 32..384)
        assertTrue(HdrMergeMath.stripRows(4000,10,12,256L*1024*1024)<=HdrMergeMath.stripRows(4000,4,12,256L*1024*1024))
    }
    @Test fun previewMeterCountsUngradedHighlightsAndDetectsLocalChange() {
        val p=IntArray(64*48) {0xff303030.toInt()};for(i in 0 until 100) p[i]=0xffffffff.toInt()
        val (first,previous)=HdrSceneMeter.measure(p,64,48,null,1)
        assertEquals(100.0/p.size,first.clippedFraction,1e-6);assertNull(first.motionScore)
        val q=p.copyOf();for(y in 10..32) for(x in 12..40) q[y*64+x]=0xffc0c0c0.toInt()
        val (second,_)=HdrSceneMeter.measure(q,64,48,previous,2)
        assertTrue(second.motionScore!!>.04)
    }
    @Test fun cachedNoiseAndScanlineFlowMatchTheUnoptimisedMath() {
        for(iso in listOf(50,800,12800)) for(gain in listOf(.125f,1f,2f,8f)) for(luma in listOf(0f,.01f,.2f,2f,9f)) {
            assertEquals(HdrMergeMath.sigma(luma,iso,gain),HdrMergeMath.Noise(iso,gain).atRootLuma(sqrt(luma)),.000001f)
        }
        val f=TileAlignment.Field(4,3,32,FloatArray(12) {it*.4f},FloatArray(12) {it*-.17f},FloatArray(12) {it/12f},1.0,0.0)
        val value=FloatArray(3)
        for(scale in listOf(1f,1.34f,2.75f,6.3f)) for(y in listOf(0f,15.4f,63f,95f)) {
            val width=(128*scale).roundToInt();val row=FloatArray(width*3);f.fillRow(y,width,scale,row)
            for(x in 0 until width) {f.sample(x/scale,y,value);for(c in 0..2) assertEquals(value[c],row[x*3+c],.00001f)}
        }
    }
    @Test fun tileAlignmentFindsFractionalTranslationAndKeepsUnrelatedImagesOut() {
        val w=320;val h=240
        fun scene(x: Double,y: Double)=((sin(x*.11)+cos(y*.09)+sin((x+y)*.037))*32+125).roundToInt().coerceIn(0,255)
        val a=IntArray(w*h) {scene((it%w).toDouble(),(it/w).toDouble())}
        val b=IntArray(w*h) {scene(it%w-2.5,it/w+1.25)}
        val field=TileAlignment.align(a,b,w,h);val v=FloatArray(3);field.sample(160f,120f,v)
        assertTrue(field.acceptedFraction>.8);assertEquals(2.5,v[0].toDouble(),.65);assertEquals(-1.25,v[1].toDouble(),.65)
        val random=java.util.Random(34);val wrong=IntArray(a.size) {random.nextInt(256)}
        assertEquals(0.0,TileAlignment.align(a,wrong,w,h).acceptedFraction,0.0)
    }
}
