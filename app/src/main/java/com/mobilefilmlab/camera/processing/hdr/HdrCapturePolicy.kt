package com.mobilefilmlab.camera.processing.hdr

import com.mobilefilmlab.camera.camera.ProControlMath
import com.mobilefilmlab.camera.data.model.CameraSpec
import kotlin.math.pow
import kotlin.math.roundToInt

/** JPEG-burst exposure policy inspired by published HDR+ principles, not a Google implementation. */
object HdrCapturePolicy {
    data class Exposure(val iso: Int,val timeNs: Long,val longFrame: Boolean=false)
    data class Plan(val helpers: List<Exposure>,val highlightStops: Double,val note: String)
    fun plan(spec: CameraSpec,iso: Int?,timeNs: Long?,quality: Boolean,scene: HdrSceneStats?): Plan? {
        if(!spec.manualSensor || iso==null || iso<=0 || timeNs==null || timeNs<=0) return null
        val stops=when {
            scene==null -> .67
            scene.clippedFraction>.012 -> 1.25
            scene.highPercentile>=245 -> .85
            scene.highPercentile>=220 && scene.shadowFraction>.15 -> .4
            else -> 0.0
        }
        val base=iso.toDouble()*timeNs
        val bounds=ProControlMath.exposureRange(spec);val isos=ProControlMath.isoRange(spec)
        fun exposure(target: Double,cap: Long,long: Boolean=false): Exposure? {
            // Do not silently ask for >8x reconstruction gain when a shutter/ISO limit is reached.
            val minimum=(base/(isos.last*6.0)).toLong().coerceAtLeast(bounds.first)
            val t=(timeNs*(target/base)).toLong().coerceAtMost(cap).coerceAtLeast(minimum).coerceIn(bounds)
            val i=(target/t).roundToInt().coerceIn(isos)
            if(base/(i.toDouble()*t) !in .125..8.0) return null
            return Exposure(i,t,long)
        }
        val moving=scene?.motionScore?.let {it>.055} ?: false
        val shortCap=if(quality) 66_666_667L else if(moving) 25_000_000L else 40_000_000L
        val short=exposure(base*2.0.pow(-stops),shortCap) ?: return null
        val shorts=if(quality) 7 else if(iso<320 && scene!=null && scene.clippedFraction<.005 && !moving) 3 else 5
        val helpers=MutableList(shorts) {short}
        if(quality && scene?.motionScore?.let {it<=.055}==true) {
            val long=exposure(base*2.0,250_000_000L,true)
            if(long!=null && long.iso.toDouble()*long.timeNs>short.iso.toDouble()*short.timeNs*1.5) repeat(2) {helpers+=long}
        }
        return Plan(helpers,stops,"${shorts} highlight-protecting short frames${if(helpers.size>shorts) " + ${helpers.size-shorts} long shadow frames" else ""}; all post-shutter; scene motion ${scene?.motionScore ?: "unknown"}")
    }
}

data class HdrCaptureInfo(val quality: Boolean,val shortIndices: List<Int>,val longIndices: List<Int> = emptyList(),val captureNote: String="",val fallbackReason: String?=null)

data class HdrSceneStats(val timestampMs: Long,val clippedFraction: Double,val highPercentile: Int,val shadowFraction: Double,val motionScore: Double?)

/** Small, ungraded preview samples only; no scene classifier, ML model or stored preview history. */
object HdrSceneMeter {
    fun measure(pixels: IntArray,width: Int,height: Int,previous: IntArray?,timestampMs: Long): Pair<HdrSceneStats,IntArray> {
        require(width*height==pixels.size && width>0 && height>0)
        val histogram=IntArray(256);val luma=IntArray(pixels.size)
        var clipped=0;var shadow=0
        for(i in pixels.indices) {
            val p=pixels[i];val r=p ushr 16 and 255;val g=p ushr 8 and 255;val b=p and 255
            val y=(77*r+150*g+29*b) ushr 8;luma[i]=y;histogram[y]++
            if(maxOf(r,g,b)>=250) clipped++
            if(y<48) shadow++
        }
        var n=0;var high=255
        for(i in histogram.indices) {n+=histogram[i];if(n>=pixels.size*.985) {high=i;break}}
        val motion=if(previous!=null && previous.size==luma.size) {
            val shift=com.mobilefilmlab.camera.processing.FrameAlignment.align(previous,luma,width,height,3)
            var diff=0.0;var count=0
            val dx=if(shift.accepted) shift.dx else 0;val dy=if(shift.accepted) shift.dy else 0
            var mean=0.0
            for(i in luma.indices) mean+=luma[i]-previous[i]
            mean/=luma.size
            for(y in 4 until height-4 step 2) for(x in 4 until width-4 step 2) {
                diff+=kotlin.math.abs(luma[(y+dy)*width+x+dx]-previous[y*width+x]-mean);count++
            }
            (diff/count.coerceAtLeast(1)/255.0).coerceIn(0.0,1.0)
        } else null
        return HdrSceneStats(timestampMs,clipped.toDouble()/pixels.size,high,shadow.toDouble()/pixels.size,motion) to luma
    }
}
