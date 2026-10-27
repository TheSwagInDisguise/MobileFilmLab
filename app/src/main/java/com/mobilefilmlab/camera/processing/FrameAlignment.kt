package com.mobilefilmlab.camera.processing

import kotlin.math.abs
import kotlin.math.sqrt

/** Translation-only registration, deliberately rejects low-texture or badly mismatched frames. */
object FrameAlignment {
    data class Alignment(val dx: Int, val dy: Int, val error: Double, val accepted: Boolean)
    fun align(reference: IntArray, other: IntArray, width: Int, height: Int, maxShift: Int=8): Alignment {
        require(reference.size == width*height && other.size == reference.size)
        if (width < maxShift*2+8 || height < maxShift*2+8) return Alignment(0,0,Double.POSITIVE_INFINITY,false)
        fun normalize(a: IntArray): Pair<DoubleArray,Double> {
            val mean=a.average(); var variance=0.0
            a.forEach { variance+=(it-mean)*(it-mean) }
            val sd=sqrt(variance/a.size)
            return DoubleArray(a.size) { (a[it]-mean)/(sd.coerceAtLeast(1.0)) } to sd
        }
        val (ref,rsd)=normalize(reference); val (target,tsd)=normalize(other)
        if (rsd < 6 || tsd < 6) return Alignment(0,0,1.0,false)
        val margin=maxShift+2
        fun score(dx: Int,dy: Int): Double {
            var sum=0.0; var n=0
            for(y in margin until height-margin step 2) for(x in margin until width-margin step 2) {
                val i=y*width+x; val j=(y+dy)*width+x+dx
                sum+=abs(ref[i]-target[j]).coerceAtMost(2.0); n++
            }
            return if(n>0) sum/n else Double.POSITIVE_INFINITY
        }
        var bx=0; var by=0; var best=score(0,0)
        for(dy in -maxShift..maxShift) for(dx in -maxShift..maxShift) {
            val cost=score(dx,dy)+0.00001*(abs(dx)+abs(dy))
            if(cost<best) { best=cost; bx=dx; by=dy }
        }
        return Alignment(bx,by,best,best<0.38 && abs(bx)<maxShift && abs(by)<maxShift)
    }
}
