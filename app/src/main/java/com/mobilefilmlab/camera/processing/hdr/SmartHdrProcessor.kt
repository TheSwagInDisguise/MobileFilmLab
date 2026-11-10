package com.mobilefilmlab.camera.processing.hdr

import android.graphics.*
import com.mobilefilmlab.camera.camera.CaptureBundle
import com.mobilefilmlab.camera.camera.CapturedJpeg
import kotlinx.coroutines.CancellationException
import kotlin.math.*

/**
 * JPEG-domain conventional burst HDR. Keeps one full output bitmap and bounded decoded strips;
 * never N full-resolution RGB bitmaps. The vendor ISP has already processed every input JPEG.
 * A separate metered first exposure is retained for a truthful, unstacked Original fallback.
 */
object SmartHdrProcessor {
    data class Output(val bitmap: Bitmap?,val referenceIndex: Int=0,val usedFrames: Int=0,val note: String="",val fallbackReason: String?=null,val processingMs: Long=0)
    private data class Thumb(val width: Int,val height: Int,val pixels: IntArray,val luma: IntArray) {
        fun sharpness(): Double {
            var sum=0.0;var count=0
            for(y in 2 until height-2 step 2) for(x in 2 until width-2 step 2) {
                // Central differences across two pixels reduce the preference for high-frequency JPEG noise.
                val d=abs(luma[y*width+x+2]-luma[y*width+x-2])+abs(luma[(y+2)*width+x]-luma[(y-2)*width+x])
                sum+=(d-8).coerceAtLeast(0);count++
            }
            return sum/count.coerceAtLeast(1)
        }
    }
    private data class Input(val index: Int,val gain: Float,val iso: Int,val field: TileAlignment.Field,val isLong: Boolean) {val noise=HdrMergeMath.Noise(iso,gain)}
    private data class Strip(val pixels: IntArray,val width: Int,val y: Int,val height: Int) {
        fun nearest(x: Int,yy: Int)=pixels[(yy-y).coerceIn(0,height-1)*width+x.coerceIn(0,width-1)]
        fun rgb(x: Float,yy: Float,out: FloatArray): Boolean {
            if(x<0 || yy<y || x>width-1 || yy>y+height-1) return false
            val x0=x.toInt();val y0=yy.toInt();val fx=x-x0;val fy=yy-y0
            val a=nearest(x0,y0);val b=nearest(x0+1,y0);val c=nearest(x0,y0+1);val d=nearest(x0+1,y0+1)
            for(channel in 0..2) {
                val shift=16-channel*8
                out[channel]=(HdrMergeMath.linear[a ushr shift and 255]*(1-fx)+HdrMergeMath.linear[b ushr shift and 255]*fx)*(1-fy)+(HdrMergeMath.linear[c ushr shift and 255]*(1-fx)+HdrMergeMath.linear[d ushr shift and 255]*fx)*fy
            }
            val nearest=nearest(x.roundToInt(),yy.roundToInt())
            out[3]=maxOf(nearest ushr 16 and 255,nearest ushr 8 and 255,nearest and 255).toFloat()
            return true
        }
    }
    private fun options(sample: Int=1)=BitmapFactory.Options().apply {
        inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888;inScaled=false
        inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB)
    }
    private fun dimensions(frame: CapturedJpeg): Pair<Int,Int> {
        val o=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        BitmapFactory.decodeByteArray(frame.bytes,0,frame.bytes.size,o)
        require(o.outWidth>0 && o.outHeight>0) {"Unreadable JPEG input"}
        return o.outWidth to o.outHeight
    }
    private fun thumb(frame: CapturedJpeg): Thumb {
        val (w,h)=dimensions(frame);var sample=1
        while(max(w,h)/(sample*2)>=640) sample*=2
        var bitmap=BitmapFactory.decodeByteArray(frame.bytes,0,frame.bytes.size,options(sample)) ?: error("Preview decode failed")
        try {
            if(max(bitmap.width,bitmap.height)>640) {
                val ratio=640.0/max(bitmap.width,bitmap.height)
                val resized=Bitmap.createScaledBitmap(bitmap,(bitmap.width*ratio).roundToInt(),(bitmap.height*ratio).roundToInt(),true)
                if(resized!==bitmap) {bitmap.recycle();bitmap=resized}
            }
            val p=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(p,0,bitmap.width,0,0,bitmap.width,bitmap.height)
            val y=IntArray(p.size) {i -> val c=p[i];((c ushr 16 and 255)*77+(c ushr 8 and 255)*150+(c and 255)*29) ushr 8}
            return Thumb(bitmap.width,bitmap.height,p,y)
        } finally {bitmap.recycle()}
    }
    private fun exposure(frame: CapturedJpeg): Double? {
        val i=frame.metadata.iso ?: return null;val t=frame.metadata.exposureTimeNs ?: return null
        val boost=frame.metadata.postRawSensitivityBoost ?: 100
        if(boost<=0) return null
        return (i.toDouble()*t*boost/100).takeIf {i>0 && t>0 && it.isFinite()}
    }
    private class LocalBase(val width: Int,val height: Int,val values: FloatArray) {
        fun get(x: Float,y: Float): Float {
            val xx=(x*(width-1)).coerceIn(0f,(width-1).toFloat());val yy=(y*(height-1)).coerceIn(0f,(height-1).toFloat())
            val ix=xx.toInt();val iy=yy.toInt();val fx=xx-ix;val fy=yy-iy
            return (values[iy*width+ix]*(1-fx)+values[iy*width+min(ix+1,width-1)]*fx)*(1-fy)+(values[min(iy+1,height-1)*width+ix]*(1-fx)+values[min(iy+1,height-1)*width+min(ix+1,width-1)]*fx)*fy
        }
    }
    private fun localBase(t: Thumb,gain: Float): Pair<LocalBase,Double> {
        val gw=48;val gh=(48.0*t.height/t.width).roundToInt().coerceAtLeast(8)
        var a=FloatArray(gw*gh);val peaks=FloatArray(t.pixels.size)
        for(i in peaks.indices) {val p=t.pixels[i];peaks[i]=maxOf(HdrMergeMath.linear[p ushr 16 and 255],HdrMergeMath.linear[p ushr 8 and 255],HdrMergeMath.linear[p and 255])*gain}
        peaks.sort();val white=peaks[(peaks.size*.997).toInt().coerceAtMost(peaks.lastIndex)].toDouble().coerceIn(1.0,4.0)
        for(y in 0 until gh) for(x in 0 until gw) {
            val tx=(x*(t.width-1)/(gw-1)).coerceIn(0,t.width-1);val ty=(y*(t.height-1)/(gh-1)).coerceIn(0,t.height-1)
            var sum=0f;var n=0
            for(yy in max(0,ty-2)..min(t.height-1,ty+2)) for(xx in max(0,tx-2)..min(t.width-1,tx+2)) {
                val p=t.pixels[yy*t.width+xx]
                sum+=(.2126f*HdrMergeMath.linear[p ushr 16 and 255]+.7152f*HdrMergeMath.linear[p ushr 8 and 255]+.0722f*HdrMergeMath.linear[p and 255])*gain;n++
            }
            a[y*gw+x]=sum/n
        }
        repeat(2) {
            val b=FloatArray(a.size)
            for(y in 0 until gh) for(x in 0 until gw) {
                val center=a[y*gw+x];var sum=0f;var weights=0f
                for(yy in max(0,y-2)..min(gh-1,y+2)) for(xx in max(0,x-2)..min(gw-1,x+2)) {
                    val v=a[yy*gw+xx];val w=1f/(1+abs(v-center)*14+abs(xx-x)+abs(yy-y))
                    sum+=v*w;weights+=w
                }
                b[y*gw+x]=sum/weights
            }
            a=b
        }
        return LocalBase(gw,gh,a) to white
    }
    @Suppress("DEPRECATION")
    fun process(capture: CaptureBundle): Output {
        val started=System.nanoTime();var output: Bitmap?=null
        val decoders=mutableListOf<BitmapRegionDecoder>()
        try {
            val info=capture.hdr ?: return Output(null,fallbackReason="No HDR capture plan")
            if(info.fallbackReason!=null) return Output(null,fallbackReason=info.fallbackReason)
            val frames=capture.frames
            require(frames.size in 4..10) {"Incomplete burst; using metered Original"}
            val shorts=info.shortIndices.distinct().filter {it in 1..frames.lastIndex}
            require(shorts.size>=3 && info.shortIndices.size==shorts.size) {"Insufficient independent short exposures"}
            val size=dimensions(frames.first())
            require(frames.all {dimensions(it)==size}) {"Input sizes changed during burst"}
            val focal=frames[0].metadata.focalLengthMm
            require(focal==null || focal<=0 || frames.drop(1).all {f -> f.metadata.focalLengthMm?.let {abs(it-focal)/focal<.035f} ?: true}) {"Lens focal length changed during burst"}
            val safetyExposure=exposure(frames[0]) ?: error("Actual safety exposure missing")
            val firstShort=exposure(frames[shorts.first()]) ?: error("Actual short exposure missing")
            val seen=mutableSetOf<Long>()
            val validShorts=shorts.filter {i ->
                val e=exposure(frames[i]);val timestamp=frames[i].metadata.sensorTimestampNs
                e!=null && e/firstShort in .75..1.333 && (timestamp==null || seen.add(timestamp))
            }
            require(validShorts.size>=3) {"Short exposures missing, duplicated or inconsistent"}
            var referenceIndex=validShorts.first();var reference=thumb(frames[referenceIndex]);var sharpness=reference.sharpness()
            for(i in validShorts.drop(1).take(2)) {
                val t=thumb(frames[i]);val s=t.sharpness()
                if(s>sharpness*1.12) {referenceIndex=i;reference=t;sharpness=s}
            }
            val referenceGain=(safetyExposure/exposure(frames[referenceIndex])!!).toFloat()
            require(referenceGain in .125f..8f) {"Exposure difference exceeds safe JPEG reconstruction range"}
            val widthThumb=reference.width;val heightThumb=reference.height
            val inputs=mutableListOf(Input(referenceIndex,referenceGain,frames[referenceIndex].metadata.iso!!,TileAlignment.identity(widthThumb,heightThumb),false))
            val extras=(validShorts.filter {it!=referenceIndex}+info.longIndices.distinct().filter {it in 1..frames.lastIndex && it !in shorts}+0).distinct()
            for(i in extras) {
                val e=exposure(frames[i]) ?: continue;val gain=(safetyExposure/e).toFloat()
                if(gain !in .125f..8f) continue
                val t=thumb(frames[i]);if(t.width!=widthThumb || t.height!=heightThumb) continue
                val long=i in info.longIndices
                if(long && t.sharpness()<sharpness*.45) continue
                val field=TileAlignment.align(reference.luma,t.luma,widthThumb,heightThumb)
                if(field.acceptedFraction<(if(long || i==0) .5 else .3)) continue
                inputs+=Input(i,gain,frames[i].metadata.iso!!,field,long)
            }
            require(inputs.count {it.index in validShorts}>=3) {"Alignment rejected the short burst"}
            val (base,white)=localBase(reference,referenceGain)
            val (sourceW,sourceH)=size
            val heap=Runtime.getRuntime().maxMemory()
            val budget=min(12_600_000L,(heap/18).coerceAtLeast(3_200_000L))
            var sample=1
            while(sourceW.toLong()*sourceH/(sample*sample)>budget) sample*=2
            val w=(sourceW+sample-1)/sample;val h=(sourceH+sample-1)/sample
            val scaleX=w.toFloat()/widthThumb;val scaleY=h.toFloat()/heightThumb
            val margins=inputs.map {ceil(it.field.dy.maxOf {v -> abs(v)}*scaleY).toInt()+3}
            val margin=margins.max()
            val rows=HdrMergeMath.stripRows(w,inputs.size,margin,heap)
            inputs.forEach {input -> decoders+=BitmapRegionDecoder.newInstance(frames[input.index].bytes,0,frames[input.index].bytes.size,false)}
            output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
            val decodedOptions=options(sample);val flowRows=Array(inputs.size) {FloatArray(w*3)};val rgb=FloatArray(4)
            val colors=FloatArray(inputs.size*4);val agreements=FloatArray(inputs.size);val row=IntArray(w)
            var useful=0L;var supported=0L;val contributing=LongArray(inputs.size)
            for(top in 0 until h step rows) {
                if(Thread.currentThread().isInterrupted) throw CancellationException("HDR processing cancelled")
                val end=min(h,top+rows)
                val strips=decoders.mapIndexed {i,decoder ->
                    val y0=max(0,top-margins[i]);val y1=min(h,end+margins[i])
                    val bitmap=decoder.decodeRegion(Rect(0,y0*sample,sourceW,min(sourceH,y1*sample)),decodedOptions) ?: error("JPEG strip unavailable")
                    try {
                        require(bitmap.width==w && bitmap.height>=y1-y0) {"JPEG region decoder changed strip geometry"}
                        val p=IntArray(w*bitmap.height);bitmap.getPixels(p,0,w,0,0,w,bitmap.height)
                        require(p.any {it ushr 24!=0}) {"Empty/transparent JPEG region decode; preserving Original"}
                        Strip(p,w,y0,bitmap.height)
                    } finally {bitmap.recycle()}
                }
                for(y in top until end) {
                    for(i in 1 until inputs.size) inputs[i].field.fillRow(y/scaleY,w,scaleX,flowRows[i])
                    for(x in 0 until w) {
                        val referencePixel=strips[0].nearest(x,y)
                        val ar=HdrMergeMath.linear[referencePixel ushr 16 and 255]*referenceGain
                        val ag=HdrMergeMath.linear[referencePixel ushr 8 and 255]*referenceGain
                        val ab=HdrMergeMath.linear[referencePixel and 255]*referenceGain
                        val al=.2126f*ar+.7152f*ag+.0722f*ab
                        val rootLuma=sqrt(al.coerceAtLeast(0f))
                        val anchorSigma=inputs[0].noise.atRootLuma(rootLuma)
                        agreements.fill(0f);var consensus=0f;var longConsensus=0f
                        for(i in 1 until inputs.size) {
                            val input=inputs[i];val flow=flowRows[i];val offset=x*3;val confidence=flow[offset+2]
                            if(confidence<.08f || !strips[i].rgb(x+flow[offset]*scaleX,y+flow[offset+1]*scaleY,rgb)) continue
                            val cr=rgb[0]*input.gain;val cg=rgb[1]*input.gain;val cb=rgb[2]*input.gain
                            if(rgb[3]>=250) continue // Cannot recover ISP-clipped information from a helper.
                            val noise=input.noise.atRootLuma(rootLuma)
                            val sigma=anchorSigma+noise+.008f*rootLuma
                            val weight=HdrMergeMath.weight(cr,cg,cb,ar,ag,ab,sigma,confidence)
                            if(weight<.08f) continue
                            val j=i*4;colors[j]=cr;colors[j+1]=cg;colors[j+2]=cb
                            // Variance-aware weighting is deliberately capped to keep long exposures from dominating.
                            colors[j+3]=(anchorSigma*anchorSigma/(noise*noise).coerceAtLeast(.000001f)).coerceIn(.25f,if(input.isLong) 2f else 1.5f)
                            agreements[i]=weight
                            if(input.isLong) longConsensus+=weight else consensus+=weight
                        }
                        var r=ar;var g=ag;var b=ab;var total=1f;var used=0f
                        // Require redundant agreement for motion-sensitive long exposures. No neighbouring-pixel fill.
                        for(i in 1 until inputs.size) {
                            if(agreements[i]<=0f || (inputs[i].isLong && (consensus<1.25f || longConsensus<.6f))) continue
                            val j=i*4;val weight=agreements[i]*colors[j+3]
                            r+=colors[j]*weight;g+=colors[j+1]*weight;b+=colors[j+2]*weight;total+=weight;used+=agreements[i];contributing[i]++
                        }
                        r/=total;g/=total;b/=total
                        val luma=.2126f*r+.7152f*g+.0722f*b
                        val gain=HdrMergeMath.toneScale(maxOf(r,g,b),luma,base.get(x.toFloat()/(w-1).coerceAtLeast(1),y.toFloat()/(h-1).coerceAtLeast(1)),white,info.quality,(used/2).coerceAtMost(1f))
                        row[x]=(255 shl 24) or (HdrMergeMath.encoded(r*gain) shl 16) or (HdrMergeMath.encoded(g*gain) shl 8) or HdrMergeMath.encoded(b*gain)
                        if(used>.6f) supported++
                        useful++
                    }
                    output.setPixels(row,0,w,0,y,w,1)
                }
            }
            val support=supported.toDouble()/useful.coerceAtLeast(1)
            require(support>.18) {"Too little agreeing image area; preserving metered Original"}
            val usedFrameCount=1+contributing.drop(1).count {it>0}
            val note="${if(info.quality) "Quality HDR" else "Smart HDR"}: ${usedFrameCount} frames used / ${frames.size} captured; short reference ${referenceIndex+1}; ${(support*100).roundToInt()}% temporal support; actual ISO/time/available post-RAW-gain normalised, JPEG-domain; local tile registration, RGB motion rejection, hue-preserving tone; ${w}x$h${if(sample>1) "; memory-safe ${sample}x decode" else "; native source grid"}; ${info.captureNote}"
            val result=Output(output,referenceIndex,usedFrameCount,note,processingMs=(System.nanoTime()-started)/1_000_000)
            output=null // Ownership passes to ImageProcessor, which crops/grades/encodes and recycles it.
            return result
        } catch(e: CancellationException) {throw e}
        catch(e: OutOfMemoryError) {return Output(null,fallbackReason="HDR memory budget exceeded; using metered Original",processingMs=(System.nanoTime()-started)/1_000_000)}
        catch(e: Exception) {return Output(null,fallbackReason=e.message?.take(140) ?: "HDR processing rejected the burst",processingMs=(System.nanoTime()-started)/1_000_000)}
        finally {output?.recycle();decoders.forEach {it.recycle()}}
    }
}
