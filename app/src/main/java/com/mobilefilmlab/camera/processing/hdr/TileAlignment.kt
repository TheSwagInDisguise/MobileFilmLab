package com.mobilefilmlab.camera.processing.hdr

import com.mobilefilmlab.camera.processing.FrameAlignment
import kotlin.math.*

/** Coarse-to-fine, overlapping local translation tiles with fractional refinement, not optical-flow ML. */
object TileAlignment {
    data class Field(val cols: Int,val rows: Int,val step: Int,val dx: FloatArray,val dy: FloatArray,val confidence: FloatArray,val acceptedFraction: Double,val meanError: Double) {
        /** Same bilinear field, with vertical interpolation reused for an entire native scanline. */
        fun fillRow(y: Float,width: Int,scaleX: Float,out: FloatArray) {
            require(out.size>=width*3 && scaleX>0)
            val gy=(y/step).coerceIn(0f,(rows-1).toFloat());val y0=gy.toInt();val y1=min(y0+1,rows-1);val fy=gy-y0
            fun vertical(a: FloatArray,x: Int)=a[y0*cols+x]*(1-fy)+a[y1*cols+x]*fy
            val inverse=1f/(scaleX*step)
            for(cell in 0 until cols) {
                val start=ceil(cell*step*scaleX).toInt().coerceIn(0,width)
                val end=if(cell==cols-1) width else ceil((cell+1)*step*scaleX).toInt().coerceIn(0,width)
                val next=min(cell+1,cols-1)
                val ax=vertical(dx,cell);val bx=vertical(dx,next)-ax
                val ay=vertical(dy,cell);val by=vertical(dy,next)-ay
                val ac=vertical(confidence,cell);val bc=vertical(confidence,next)-ac
                for(x in start until end) {
                    val f=(x*inverse-cell).coerceIn(0f,1f);val j=x*3
                    out[j]=ax+bx*f;out[j+1]=ay+by*f;out[j+2]=ac+bc*f
                }
            }
        }
        fun sample(x: Float,y: Float,out: FloatArray) {
            val gx=(x/step).coerceIn(0f,(cols-1).toFloat());val gy=(y/step).coerceIn(0f,(rows-1).toFloat())
            val x0=gx.toInt();val y0=gy.toInt();val x1=min(x0+1,cols-1);val y1=min(y0+1,rows-1)
            val fx=gx-x0;val fy=gy-y0
            fun mix(a: FloatArray): Float=(a[y0*cols+x0]*(1-fx)+a[y0*cols+x1]*fx)*(1-fy)+(a[y1*cols+x0]*(1-fx)+a[y1*cols+x1]*fx)*fy
            out[0]=mix(dx);out[1]=mix(dy);out[2]=mix(confidence)
        }
    }
    fun identity(width: Int,height: Int,step: Int=32)=Field((width-1)/step+1,(height-1)/step+1,step,FloatArray(((width-1)/step+1)*((height-1)/step+1)),FloatArray(((width-1)/step+1)*((height-1)/step+1)),FloatArray(((width-1)/step+1)*((height-1)/step+1)) {1f},1.0,0.0)
    fun align(reference: IntArray,target: IntArray,width: Int,height: Int): Field {
        require(reference.size==width*height && target.size==reference.size)
        val step=32;val cols=(width-1)/step+1;val rows=(height-1)/step+1
        val xs=FloatArray(cols*rows);val ys=FloatArray(xs.size);val confidences=FloatArray(xs.size)
        fun normalized(a: IntArray): Pair<FloatArray,Double> {
            val mean=a.average();val sd=sqrt(a.sumOf {(it-mean)*(it-mean)}/a.size).coerceAtLeast(1.0)
            return FloatArray(a.size) {((a[it]-mean)/sd).toFloat().coerceIn(-3f,3f)} to sd
        }
        val (r,rsd)=normalized(reference);val (t,tsd)=normalized(target)
        if(rsd<5 && tsd<7) return identity(width,height,step).copy(confidence=FloatArray(xs.size) {.35f},meanError=.1)
        val scale=if(width>=320 && height>=160) 4 else 1
        val cw=width/scale;val ch=height/scale
        fun coarse(a: IntArray)=IntArray(cw*ch) {i -> a[(i/cw*scale)*width+i%cw*scale]}
        val global=FrameAlignment.align(coarse(reference),coarse(target),cw,ch,if(scale>1) 8 else 6)
        if(!global.error.isFinite() || global.error>.85) return Field(cols,rows,step,xs,ys,confidences,0.0,global.error)
        val gx=global.dx*scale;val gy=global.dy*scale
        val radius=12;val search=if(scale>1) 4 else 2
        fun score(cx: Int,cy: Int,dx: Int,dy: Int): Double {
            var sum=0.0;var count=0
            for(y in cy-radius..cy+radius step 2) for(x in cx-radius..cx+radius step 2) {
                val xx=x+dx;val yy=y+dy
                if(x !in 1 until width-1 || y !in 1 until height-1 || xx !in 1 until width-1 || yy !in 1 until height-1) continue
                sum+=abs(r[y*width+x]-t[yy*width+xx]).coerceAtMost(2f);count++
            }
            return if(count<20) 10.0 else sum/count
        }
        var errors=0.0;var accepted=0
        for(ty in 0 until rows) for(tx in 0 until cols) {
            val cx=(tx*step).coerceIn(min(radius,width/2),max(width-radius-1,width/2));val cy=(ty*step).coerceIn(min(radius,height/2),max(height-radius-1,height/2))
            var bx=gx;var by=gy;var best=score(cx,cy,bx,by)
            for(dy in gy-search..gy+search) for(dx in gx-search..gx+search) {
                val s=score(cx,cy,dx,dy)+.00005*(abs(dx-gx)+abs(dy-gy))
                if(s<best) {best=s;bx=dx;by=dy}
            }
            fun fraction(negative: Double,positive: Double): Float {
                val d=negative+positive-2*best
                return if(d<.005) 0f else ((negative-positive)/(2*d)).toFloat().coerceIn(-.5f,.5f)
            }
            val index=ty*cols+tx
            xs[index]=bx+fraction(score(cx,cy,bx-1,by),score(cx,cy,bx+1,by))
            ys[index]=by+fraction(score(cx,cy,bx,by-1),score(cx,cy,bx,by+1))
            confidences[index]=((.58-best)/.48).toFloat().coerceIn(0f,1f)
            errors+=best;if(confidences[index]>.2f) accepted++
        }
        // Do not interpolate a confident background flow across an isolated, inconsistent motion tile.
        val original=confidences.copyOf()
        for(y in 0 until rows) for(x in 0 until cols) {
            val i=y*cols+x
            for(yy in max(0,y-1)..min(rows-1,y+1)) for(xx in max(0,x-1)..min(cols-1,x+1)) {
                val j=yy*cols+xx
                if(original[j]<.08f || abs(xs[i]-xs[j])+abs(ys[i]-ys[j])>4f) confidences[i]=min(confidences[i],.25f)
            }
        }
        return Field(cols,rows,step,xs,ys,confidences,accepted.toDouble()/xs.size,errors/xs.size)
    }
}
