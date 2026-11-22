import Foundation
import CoreImage
import ImageIO
import UniformTypeIdentifiers
import UIKit
import FilmLabCore

struct ProcessedPhoto {var data:Data?;var raw:Data?;var original:Data?;var width:Int;var height:Int;var note:String}

/// Conventional colour/denoise/detail only. There is no ML model or network-based enhancement.
final class PhotoProcessor {
    static let colourSpace=CGColorSpace(name:CGColorSpace.sRGB)!
    // Explicitly preserve the photograph's alpha. Signed noise in a zero-alpha
    // compositing layer is discarded by Core Image's premultiplication rules.
    private static let grainKernel=CIColorKernel(source:"""
        kernel vec4 filmLabGrain(__sample photograph, float strength, float seed) {
            vec4 rgba = unpremultiply(photograph);
            vec2 position = floor(destCoord()) + vec2(seed, 47.0);
            float noise = fract(sin(dot(position, vec2(12.9898, 78.233))) * 43758.5453);
            float delta = (noise - 0.5) * strength;
            return premultiply(vec4(clamp(rgba.rgb + vec3(delta), 0.0, 1.0), rgba.a));
        }
        """)
    static func grain(_ image:CIImage,amount:Double,seed:TimeInterval) throws -> CIImage {
        guard amount>0 else {return image}
        // Coordinate-hash luminance noise has bounded, zero-centred RGB deltas;
        // it never interprets a random image's alpha as a brightness divisor.
        guard let kernel=grainKernel else {throw FilmError.invalid("Grain renderer unavailable")}
        guard let result=kernel.apply(extent:image.extent,arguments:[image,amount*0.014,seed.truncatingRemainder(dividingBy:1000)]) else {throw FilmError.invalid("Grain renderer failed")}
        return result
    }
    let context=CIContext(options:[.workingColorSpace:NSNull(),.outputColorSpace:colourSpace,.cacheIntermediates:false])
    static func upright(_ data:Data)->CIImage? {CIImage(data:data,options:[.applyOrientationProperty:true,.colorSpace:colourSpace])}
    static func matrix(_ image:CIImage,recipe:Recipe,preferences p:CapturePreferences)->CIImage {
        let m=ColourMatrix.recipe(recipe,red:p.red,green:p.green,blue:p.blue).values
        return image.applyingFilter("CIColorMatrix",parameters:[
            "inputRVector":CIVector(x:m[0],y:m[1],z:m[2],w:m[3]),
            "inputGVector":CIVector(x:m[5],y:m[6],z:m[7],w:m[8]),
            "inputBVector":CIVector(x:m[10],y:m[11],z:m[12],w:m[13]),
            "inputAVector":CIVector(x:m[15],y:m[16],z:m[17],w:m[18]),
            "inputBiasVector":CIVector(x:m[4]/255,y:m[9]/255,z:m[14]/255,w:m[19]/255)])
    }
    static func crop(_ image:CIImage,factor:Double)->CIImage {
        guard factor>1 else {return image}
        let e=image.extent,w=floor(e.width/factor),h=floor(e.height/factor)
        return image.cropped(to:CGRect(x:e.midX-w/2,y:e.midY-h/2,width:w,height:h)).transformed(by:CGAffineTransform(translationX:-(e.midX-w/2),y:-(e.midY-h/2)))
    }
    func process(_ burst:PhotoBurst,recipe:Recipe,preferences p:CapturePreferences) throws -> ProcessedPhoto {
        guard let first=burst.frames.first else {throw FilmError.invalid("No camera image")}
        guard p.format.writesJPEG else {guard let raw=first.raw else {throw FilmError.invalid("Camera returned no DNG")};return ProcessedPhoto(data:nil,raw:raw,original:nil,width:0,height:0,note:burst.note+"; recipe is not baked into DNG")}
        guard let jpeg=first.jpeg,let original=Self.upright(jpeg) else {throw FilmError.invalid("JPEG decoder failed; DNG capture, if any, has not been discarded")}
        var image=original,note=burst.note,hdr=false
        if burst.pipeline != .original {
            if let fallback=burst.fallback {note+="; HDR safety fallback: \(fallback)"}
            else {
                do {let result=try JPEGHDRProcessor(context:context).merge(burst);image=result.image;note+="; \(result.note)";hdr=true}
                catch {context.clearCaches();image=original;note+="; HDR safety fallback: \(error.localizedDescription)"}
            }
        }
        let isIdentity=ColourMatrix.recipe(recipe,red:p.red,green:p.green,blue:p.blue).isIdentity
        if !hdr && !p.balanced && p.crop==1 && p.grain==0 && !p.enlarge28 && isIdentity && recipe.sharp<=0 {
            return ProcessedPhoto(data:jpeg,raw:first.raw,original:nil,width:Int(original.extent.width),height:Int(original.extent.height),note:note+"; Original JPEG bytes preserved; no app grade")
        }
        image=Self.crop(image,factor:p.crop)
        if p.crop>1 {note+="; \(String(format:"%0.1f",p.crop))× JPEG-only native-pixel centre crop"}
        if p.balanced && !hdr {
            let iso=first.iso ?? 100,strength=clamp(log2(max(100,iso)/100)/8,0,1)
            image=image.applyingFilter("CINoiseReduction",parameters:["inputNoiseLevel":0.006+strength*0.02,"inputSharpness":0.2])
            image=image.applyingFilter("CIHighlightShadowAdjust",parameters:["inputShadowAmount":0.10+strength*0.06,"inputHighlightAmount":0.95])
            note+="; conventional Balanced NR/tone"
        }
        // Match Android matrix on gamma-encoded sRGB rather than silently grading linear light.
        image=Self.matrix(image,recipe:recipe,preferences:p)
        let detail=max(0,recipe.sharp)
        if detail>0 {image=image.applyingFilter("CISharpenLuminance",parameters:["inputSharpness":Double(detail)*0.12]);note+="; mild luminance sharpening"}
        if p.grain>0 {
            image=try Self.grain(image,amount:Double(p.grain),seed:first.timestamp.timeIntervalSince1970)
            note+="; synthetic luminance grain"
        }
        if p.enlarge28 {
            let long=max(image.extent.width,image.extent.height),target=6120/p.crop
            if long<target {image=image.applyingFilter("CILanczosScaleTransform",parameters:[kCIInputScaleKey:target/long,kCIInputAspectRatioKey:1]);note+="; interpolated enlargement, not native detail"}
        }
        let rect=image.extent.integral
        guard rect.width*rect.height<=55000000,let cg=context.createCGImage(image,from:rect,format:.RGBA8,colorSpace:Self.colourSpace) else {throw FilmError.invalid("Image renderer could not allocate the export")}
        let data=NSMutableData()
        guard let destination=CGImageDestinationCreateWithData(data,UTType.jpeg.identifier as CFString,1,nil) else {throw FilmError.invalid("JPEG encoder unavailable")}
        var metadata=first.metadata
        metadata[kCGImagePropertyOrientation as String]=1
        metadata[kCGImagePropertyPixelWidth as String]=cg.width;metadata[kCGImagePropertyPixelHeight as String]=cg.height
        var exif=metadata[kCGImagePropertyExifDictionary as String] as? [String:Any] ?? [:]
        // A helper-backed HDR result retains the safety exposure in EXIF but labels the composite explicitly.
        exif[kCGImagePropertyExifPixelXDimension as String]=cg.width
        exif[kCGImagePropertyExifPixelYDimension as String]=cg.height
        exif[kCGImagePropertyExifUserComment as String]="MobileFilmLab | \(recipe.displayName) | \(note) | Synthetic recipe approximations; no app-added AI"
        metadata[kCGImagePropertyExifDictionary as String]=exif
        var tiff=metadata[kCGImagePropertyTIFFDictionary as String] as? [String:Any] ?? [:]
        tiff[kCGImagePropertyTIFFOrientation as String]=1
        tiff[kCGImagePropertyTIFFSoftware as String]="MobileFilmLab iOS \(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "development") (\(Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "local"))"
        metadata[kCGImagePropertyTIFFDictionary as String]=tiff
        metadata[kCGImageDestinationLossyCompressionQuality as String]=0.98
        CGImageDestinationAddImage(destination,cg,metadata as CFDictionary)
        guard CGImageDestinationFinalize(destination) else {throw FilmError.invalid("JPEG encoding failed")}
        return ProcessedPhoto(data:data as Data,raw:first.raw,original:(p.saveOriginal || p.compare) ? jpeg : nil,width:cg.width,height:cg.height,note:note+(first.raw != nil ? "; DNG bytes unmodified" : ""))
    }
}
