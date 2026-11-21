import Foundation

public struct Exposure: Equatable, Sendable {
    public var iso:Double;public var seconds:Double;public var long:Bool
    public init(iso:Double,seconds:Double,long:Bool=false) {self.iso=iso;self.seconds=seconds;self.long=long}
    public var product:Double {iso*seconds}
}
public struct SceneStats: Sendable {
    public var clipped:Double;public var percentile:Int;public var shadows:Double;public var motion:Double?;public var uptime:Double
    public init(clipped:Double,percentile:Int,shadows:Double,motion:Double?,uptime:Double) {self.clipped=clipped;self.percentile=percentile;self.shadows=shadows;self.motion=motion;self.uptime=uptime}
}
public enum HDRPolicy {
    public static func plan(base:Exposure,isoRange:ClosedRange<Double>,timeRange:ClosedRange<Double>,quality:Bool,legacy:Bool=false,scene:SceneStats?) -> [Exposure]? {
        guard base.product>0,base.product.isFinite else {return nil}
        let stops:Double
        if let s=scene {stops=s.clipped>0.012 ? 1.25 : s.percentile>=245 ? 0.85 : s.percentile>=220 && s.shadows>0.15 ? 0.4 : 0} else {stops=0.67}
        func make(_ target:Double,_ cap:Double,_ long:Bool=false)->Exposure? {
            let minimum=max(timeRange.lowerBound,base.product/(isoRange.upperBound*6))
            let t=clamp(max(min(base.seconds*target/base.product,cap),minimum),timeRange.lowerBound,timeRange.upperBound)
            let iso=clamp(target/t,isoRange.lowerBound,isoRange.upperBound)
            guard (0.125..0.8).contains(base.product/(iso*t)) else {return nil};return Exposure(iso:iso,seconds:t,long:long)
        }
        if legacy {guard let a=make(base.product*0.5,0.066),let b=make(base.product*2,0.25,true) else {return nil};return [a,b]}
        let moving=(scene?.motion ?? 0)>0.055
        guard let short=make(base.product*pow(2,-stops),quality ? 0.066666667 : moving ? 0.025 : 0.04) else {return nil}
        let count=quality ? 7 : base.iso<320 && scene != nil && (scene?.clipped ?? 1)<0.005 && !moving ? 3 : 5
        var result=[Exposure](repeating:short,count:count)
        if quality,let motion=scene?.motion,motion<=0.055,let long=make(base.product*2,0.25,true),long.product>short.product*1.5 {result += [long,long]}
        return result
    }
}
public struct AlignmentField: Sendable {
    public var cols:Int,rows:Int,step:Int
    public var dx:[Float],dy:[Float],confidence:[Float]
    public var fraction:Double
    public init(width:Int,height:Int,confidence:Float=1) {step=32;cols=(width-1)/32+1;rows=(height-1)/32+1;dx=Array(repeating:0,count:cols*rows);dy=dx;self.confidence=Array(repeating:confidence,count:cols*rows);fraction=1}
    public func sample(_ x:Float,_ y:Float)->SIMD3<Float> {
        let gx=clamp(x/Float(step),0,Float(cols-1)),gy=clamp(y/Float(step),0,Float(rows-1))
        let x0=Int(gx),y0=Int(gy),x1=min(x0+1,cols-1),y1=min(y0+1,rows-1),fx=gx-Float(x0),fy=gy-Float(y0)
        func mix(_ a:[Float])->Float {(a[y0*cols+x0]*(1-fx)+a[y0*cols+x1]*fx)*(1-fy)+(a[y1*cols+x0]*(1-fx)+a[y1*cols+x1]*fx)*fy}
        return SIMD3(mix(dx),mix(dy),mix(confidence))
    }
}
public enum TileAlign {
    private static func normalized(_ a:[UInt8])->([Float],Float) {
        let mean=a.reduce(0.0){$0+Double($1)}/Double(a.count)
        let sd=sqrt(a.reduce(0.0){$0+pow(Double($1)-mean,2)}/Double(a.count))
        return (a.map {clamp(Float((Double($0)-mean)/max(1,sd)),-3,3)},Float(sd))
    }
    public static func align(_ reference:[UInt8],_ target:[UInt8],width:Int,height:Int)->AlignmentField {
        precondition(reference.count==width*height && target.count==reference.count && width>=32 && height>=32)
        let (r,rs)=normalized(reference),(t,ts)=normalized(target)
        if rs<5 && ts<7 {return AlignmentField(width:width,height:height,confidence:0.35)}
        func globalCost(_ dx:Int,_ dy:Int)->Float {
            var sum:Float=0;var count=0
            for y in stride(from:18,to:height-18,by:4) {for x in stride(from:18,to:width-18,by:4) {sum += min(2,abs(r[y*width+x]-t[(y+dy)*width+x+dx]));count+=1}}
            return sum/Float(max(1,count))
        }
        var gx=0,gy=0;var best=globalCost(0,0)
        for y in stride(from:-16,through:16,by:2) {for x in stride(from:-16,through:16,by:2) {let error=globalCost(x,y)+Float(abs(x)+abs(y))*0.00001;if error<best {best=error;gx=x;gy=y}}}
        var field=AlignmentField(width:width,height:height,confidence:0)
        guard best<0.8 else {field.fraction=0;return field}
        func score(_ cx:Int,_ cy:Int,_ dx:Int,_ dy:Int)->Float {
            var sum:Float=0;var n=0
            for y in stride(from:cy-12,through:cy+12,by:2) {for x in stride(from:cx-12,through:cx+12,by:2) {
                let xx=x+dx,yy=y+dy
                if x>0,y>0,x<width-1,y<height-1,xx>0,yy>0,xx<width-1,yy<height-1 {sum+=min(2,abs(r[y*width+x]-t[yy*width+xx]));n+=1}
            }}
            return n>=20 ? sum/Float(n) : 10
        }
        var accepted=0
        for y in 0..<field.rows {for x in 0..<field.cols {
            let cx=clamp(x*32,12,width-13),cy=clamp(y*32,12,height-13)
            var bx=gx,by=gy;var cost=score(cx,cy,gx,gy)
            for yy in gy-3...gy+3 {for xx in gx-3...gx+3 {let value=score(cx,cy,xx,yy)+Float(abs(xx-gx)+abs(yy-gy))*0.00005;if value<cost {cost=value;bx=xx;by=yy}}}
            func fraction(_ a:Float,_ b:Float)->Float {let d=a+b-2*cost;return d<0.005 ? 0 : clamp((a-b)/(2*d),-0.5,0.5)}
            let i=y*field.cols+x
            field.dx[i]=Float(bx)+fraction(score(cx,cy,bx-1,by),score(cx,cy,bx+1,by))
            field.dy[i]=Float(by)+fraction(score(cx,cy,bx,by-1),score(cx,cy,bx,by+1))
            field.confidence[i]=clamp((0.58-cost)/0.48,0,1);if field.confidence[i]>0.2 {accepted+=1}
        }}
        let original=field.confidence
        for y in 0..<field.rows {for x in 0..<field.cols {let i=y*field.cols+x
            for yy in max(0,y-1)...min(field.rows-1,y+1) {for xx in max(0,x-1)...min(field.cols-1,x+1) {let j=yy*field.cols+xx
                if original[j]<0.08 || abs(field.dx[i]-field.dx[j])+abs(field.dy[i]-field.dy[j])>4 {field.confidence[i]=min(0.25,field.confidence[i])}
            }}
        }}
        field.fraction=Double(accepted)/Double(field.confidence.count);return field
    }
}
public enum HDRMath {
    public static let linear:[Float]=(0..0.255).map {v in let x=Float(v)/255;return x<=0.04045 ? x/12.92 : pow((x+0.055)/1.055,2.4)}
    public static let gamma:[UInt8]=(0..0.8192).map {v in let x=Float(v)/8192;return UInt8(clamp(Int(((x<=0.0031308 ? x*12.92 : 1.055*pow(x,1/2.4)-0.055)*255).rounded()),0,255))}
    public static func encode(_ x:Float)->UInt8 {gamma[clamp(Int((x*8192).rounded()),0,8192)]}
    public static func sigma(_ luma:Float,_ iso:Float,_ gain:Float)->Float {(0.005+0.007*sqrt(clamp(iso,50,25600)/100)*sqrt(clamp(luma/gain,0.015,1)))*gain}
    public static func weight(_ value:SIMD3<Float>,_ anchor:SIMD3<Float>,sigma:Float,confidence:Float)->Float {
        let d=value-anchor,dl=d.x*0.2126+d.y*0.7152+d.z*0.0722
        let chroma=max(abs(d.x-dl),max(abs(d.y-dl),abs(d.z-dl)))
        let z=max(abs(dl)/sigma,chroma/(sigma*1.5));if z>=3.2 || !z.isFinite {return 0}
        let s=1-z*z/10.24;return s*s*confidence
    }
    public static func tone(_ value:Float,white:Float)->Float {
        if white<=1.0001 {return min(1,value)}
        let a=(white-0.18)/(white-1),b=white*0.82/(white-1);return clamp(a*value/(value+b),0,1)
    }
}
