import Foundation
import CoreImage
import UIKit
import ImageIO
import FilmLabCore

/// Tile-aligned, noise-weighted, motion-rejected JPEG burst merge. Not Bayer-domain HDR+ or stock Night mode.
final class JPEGHDRProcessor {
    let context:CIContext
    init(context:CIContext) {self.context=context}
    private struct Raster {var image:CIImage;var width:Int;var height:Int;var luma:[UInt8]}
    private struct Helper {var image:CIImage;var field:AlignmentField;var gain:Float;var iso:Float;var long:Bool}
    private func raster(_ data:Data) throws -> Raster {
        guard let source=CGImageSourceCreateWithData(data as CFData,nil),let cg=CGImageSourceCreateThumbnailAtIndex(source,0,[kCGImageSourceCreateThumbnailFromImageAlways:true,kCGImageSourceCreateThumbnailWithTransform:true,kCGImageSourceThumbnailMaxPixelSize:512,kCGImageSourceShouldCacheImmediately:true] as CFDictionary),let image=PhotoProcessor.upright(data) else {throw FilmError.invalid("HDR thumbnail decode failed")}
        let thumb=CIImage(cgImage:cg),w=cg.width,h=cg.height
        var bytes=[UInt8](repeating:0,count:w*h*4)
        context.render(thumb,toBitmap:&bytes,rowBytes:w*4,bounds:thumb.extent,format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
        var luma=[UInt8](repeating:0,count:w*h)
        for i in luma.indices {luma[i]=UInt8((Int(bytes[i*4])*54+Int(bytes[i*4+1])*183+Int(bytes[i*4+2])*19)>>8)}
        return Raster(image:image,width:w,height:h,luma:luma)
    }
    func merge(_ burst:PhotoBurst) throws -> (image:CIImage,note:String) {
        let legacy=burst.pipeline == .legacyFusion
        guard let safety=burst.frames.first,let safetyData=safety.jpeg else {throw FilmError.invalid("Missing safety JPEG")}
        let shortIndices=burst.shorts
        guard legacy ? shortIndices.count>=1 && !burst.longs.isEmpty : shortIndices.count>=3 else {throw FilmError.invalid("Insufficient complete HDR helpers")}
        let referenceIndex=legacy ? 0 : shortIndices[0]
        let reference=burst.frames[referenceIndex]
        guard let data=reference.jpeg,let exposure=reference.exposure else {throw FilmError.invalid("Missing reference exposure")}
        let ref=try raster(data),s=try raster(safetyData)
        let width=Int(ref.image.extent.width),height=Int(ref.image.extent.height)
        guard width*height<=13000000,width>0,height>0 else {throw FilmError.invalid("HDR is limited to approximately 12 MP for memory safety")}
        guard ref.width==s.width,ref.height==s.height else {throw FilmError.invalid("HDR dimensions changed")}
        let safetyField=TileAlign.align(ref.luma,s.luma,width:ref.width,height:ref.height)
        guard safetyField.fraction>=0.55 else {throw FilmError.invalid("Camera motion exceeded safety-reference alignment")}
        var helpers:[Helper]=[];var acceptedShorts=1
        let all=shortIndices+burst.longs
        for index in all where index != referenceIndex {
            try safetyCheck()
            let frame=burst.frames[index]
            guard let data=frame.jpeg,let e=frame.exposure,(0.125...8).contains(exposure/e) else {continue}
            let candidate=try raster(data)
            guard candidate.width==ref.width,candidate.height==ref.height,Int(candidate.image.extent.width)==width,Int(candidate.image.extent.height)==height else {continue}
            let field=TileAlign.align(ref.luma,candidate.luma,width:ref.width,height:ref.height)
            guard field.fraction>=0.55 else {continue}
            let long=burst.longs.contains(index)
            helpers.append(Helper(image:candidate.image,field:field,gain:Float(exposure/e),iso:Float(frame.iso ?? 100),long:long))
            if !long {acceptedShorts+=1}
        }
        guard legacy ? helpers.count>=2 : acceptedShorts>=3 else {throw FilmError.invalid("Too few locally aligned short frames; Original retained")}
        // Safety is used for robust brightness/white estimation, not blindly averaged into a protected reference.
        guard let safetyExposure=safety.exposure else {throw FilmError.invalid("Safety exposure missing")}
        let displayGain=Float(clamp(safetyExposure/exposure,0.5,8))
        var histogram=[Int](repeating:0,count:4097)
        for v in ref.luma {let l=HDRMath.linear[Int(v)]*displayGain;histogram[clamp(Int(l*512),0,4096)]+=1}
        var count=0,white:Float=1
        for i in histogram.indices {count+=histogram[i];if Double(count)>=Double(ref.luma.count)*0.998 {white=max(1,Float(i)/512);break}}
        // One output raster plus bounded stripes. Core Image decoder/driver allocations remain OS-owned.
        var output=[UInt8](repeating:0,count:width*height*4)
        let stripeHeight=80,scaleX=Float(ref.width)/Float(width),scaleY=Float(ref.height)/Float(height)
        let sourceISO=Float(reference.iso ?? 100)
        // Bitmap rows are top-down; Core Image ROI coordinates are bottom-up.
        // Convert the ROI, not the output row order, including helper margins.
        for y0 in stride(from:0,to:height,by:stripeHeight) {
            try safetyCheck()
            let h=min(stripeHeight,height-y0),pixels=width*h
            var anchor=[UInt8](repeating:0,count:pixels*4)
            context.render(ref.image,toBitmap:&anchor,rowBytes:width*4,bounds:CGRect(x:0,y:height-y0-h,width:width,height:h),format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
            var sum=[SIMD3<Float>](repeating:.zero,count:pixels),weights=[Float](repeating:1,count:pixels)
            for i in 0..<pixels {sum[i]=SIMD3(HDRMath.linear[Int(anchor[i*4])],HDRMath.linear[Int(anchor[i*4+1])],HDRMath.linear[Int(anchor[i*4+2])])}
            for helper in helpers {
                try safetyCheck()
                let margin=Int(ceil(22/scaleY)),minY=max(0,y0-margin),maxY=min(height,y0+h+margin),hh=maxY-minY
                var bytes=[UInt8](repeating:0,count:width*hh*4)
                context.render(helper.image,toBitmap:&bytes,rowBytes:width*4,bounds:CGRect(x:0,y:height-maxY,width:width,height:hh),format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
                for y in 0..<h {for x in 0..<width {
                    let i=y*width+x,flow=helper.field.sample(Float(x)*scaleX,Float(y+y0)*scaleY)
                    guard flow.z>0.08 else {continue}
                    let xx=Float(x)+flow.x/scaleX,yy=Float(y+y0-minY)+flow.y/scaleY
                    guard xx>=0,yy>=0,xx<Float(width-1),yy<Float(hh-1) else {continue}
                    let x1=Int(xx),y1=Int(yy),fx=xx-Float(x1),fy=yy-Float(y1)
                    func sample(_ c:Int)->Float {
                        let a=HDRMath.linear[Int(bytes[(y1*width+x1)*4+c])],b=HDRMath.linear[Int(bytes[(y1*width+x1+1)*4+c])],cc=HDRMath.linear[Int(bytes[((y1+1)*width+x1)*4+c])],d=HDRMath.linear[Int(bytes[((y1+1)*width+x1+1)*4+c])]
                        return ((a*(1-fx)+b*fx)*(1-fy)+(cc*(1-fx)+d*fx)*fy)*helper.gain
                    }
                    let value=SIMD3(sample(0),sample(1),sample(2))
                    if max(value.x,max(value.y,value.z))/helper.gain>0.965 {continue}
                    let base=SIMD3(HDRMath.linear[Int(anchor[i*4])],HDRMath.linear[Int(anchor[i*4+1])],HDRMath.linear[Int(anchor[i*4+2])])
                    let l=base.x*0.2126+base.y*0.7152+base.z*0.0722
                    let noise=HDRMath.sigma(l,sourceISO,1)+HDRMath.sigma(l,helper.iso,helper.gain)
                    var w=HDRMath.weight(value,base,sigma:noise,confidence:flow.z)
                    if helper.long {w *= 0.75}
                    sum[i]+=value*w;weights[i]+=w
                }}
            }
            for i in 0..<pixels {
                var rgb=sum[i]/weights[i]*displayGain
                let l=max(0.000001,rgb.x*0.2126+rgb.y*0.7152+rgb.z*0.0722),mapped=HDRMath.tone(l,white:white)
                rgb *= mapped/l
                // Chroma compression protects bright colours instead of independently clipping channels.
                let peak=max(rgb.x,max(rgb.y,rgb.z))
                if peak>1 {let chroma=rgb-SIMD3(repeating:mapped);let amount=(1-mapped)/max(0.000001,peak-mapped);rgb=SIMD3(repeating:mapped)+chroma*clamp(amount,0,1)}
                let j=(y0*width+i)*4;output[j]=HDRMath.encode(rgb.x);output[j+1]=HDRMath.encode(rgb.y);output[j+2]=HDRMath.encode(rgb.z);output[j+3]=255
            }
        }
        let image=CIImage(bitmapData:Data(output),bytesPerRow:width*4,size:CGSize(width:width,height:height),format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
        return (image,"\(legacy ? "Experimental fusion" : "Motion-rejected JPEG HDR"): \(1+helpers.count) aligned frames; conventional sRGB merge, not RAW HDR+; ~12 MP cap")
    }
    private func safetyCheck() throws {
        if ProcessInfo.processInfo.thermalState == .serious || ProcessInfo.processInfo.thermalState == .critical {throw FilmError.invalid("Device temperature guard")}
        if MemoryPressure.shared.triggered {throw FilmError.invalid("iOS memory-pressure guard")}
    }
}
final class MemoryPressure {
    static let shared=MemoryPressure()
    private let lock=NSLock()
    private var warningTime=Date.distantPast
    private var observer:NSObjectProtocol?
    private init() {observer=NotificationCenter.default.addObserver(forName:UIApplication.didReceiveMemoryWarningNotification,object:nil,queue:nil) {[weak self] _ in self?.lock.lock();self?.warningTime=Date();self?.lock.unlock()}}
    var triggered:Bool {lock.lock();defer {lock.unlock()};return Date().timeIntervalSince(warningTime)<20}
}
