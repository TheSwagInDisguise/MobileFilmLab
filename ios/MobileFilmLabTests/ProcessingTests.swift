import XCTest
import CoreImage
import ImageIO
import UniformTypeIdentifiers
import FilmLabCore
@testable import MobileFilmLab

final class ProcessingTests:XCTestCase {
    func fixture(width:Int=192,height:Int=256,offset:Int=0,orientation:Int=1)->Data {
        var bytes=[UInt8](repeating:255,count:width*height*4)
        for y in 0..<height {for x in 0..<width {let i=(y*width+x)*4;bytes[i]=UInt8(clamp(30+x%120+offset,0,255));bytes[i+1]=UInt8(clamp(40+y%140+offset,0,255));bytes[i+2]=UInt8(50+(x*y)%100)}}
        let image=CIImage(bitmapData:Data(bytes),bytesPerRow:width*4,size:CGSize(width:width,height:height),format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
        let context=PhotoProcessor().context;let cg=context.createCGImage(image,from:image.extent)!
        let data=NSMutableData();let d=CGImageDestinationCreateWithData(data,UTType.jpeg.identifier as CFString,1,nil)!
        CGImageDestinationAddImage(d,cg,[kCGImageDestinationLossyCompressionQuality:1,kCGImagePropertyOrientation:orientation] as CFDictionary);XCTAssertTrue(CGImageDestinationFinalize(d));return data as Data
    }
    func frame(_ data:Data,exposure:Double=0.01,id:Int64=1)->PhotoFrame {PhotoFrame(jpeg:data,raw:nil,metadata:[:],iso:200,seconds:exposure,focal:6,timestamp:Date(timeIntervalSince1970:1700000000),requestID:id)}
    func testNaturalPreservesOriginalBytes() throws {let data=fixture(),r=try Catalogue.load().recipes[0];let out=try PhotoProcessor().process(PhotoBurst(frames:[frame(data)],pipeline:.original),recipe:r,preferences:CapturePreferences());XCTAssertEqual(out.data,data);XCTAssertEqual(out.width,192);XCTAssertEqual(out.height,256)}
    func testCropAppliedExactlyOnceAndExifUpright() throws {var p=CapturePreferences();p.crop=2;let out=try PhotoProcessor().process(PhotoBurst(frames:[frame(fixture())],pipeline:.original),recipe:Catalogue.load().recipes[0],preferences:p);XCTAssertEqual(out.width,96);XCTAssertEqual(out.height,128);let source=try XCTUnwrap(CGImageSourceCreateWithData(try XCTUnwrap(out.data) as CFData,nil));let properties=try XCTUnwrap(CGImageSourceCopyPropertiesAtIndex(source,0,nil) as? [String:Any]);XCTAssertEqual((properties[kCGImagePropertyOrientation as String] as? NSNumber)?.intValue,1)}
    func testRAWBytesUnchangedAndNoFakeJPEG() throws {var p=CapturePreferences();p.format = .raw;var f=frame(fixture());let raw=Data([73,73,42,0,4,5,6]);f.raw=raw;let out=try PhotoProcessor().process(PhotoBurst(frames:[f],pipeline:.original),recipe:Catalogue.load().recipes[0],preferences:p);XCTAssertEqual(out.raw,raw);XCTAssertNil(out.data)}
    func testHDRIncompleteFallsBackToSafetyBytes() throws {let data=fixture();let out=try PhotoProcessor().process(PhotoBurst(frames:[frame(data)],pipeline:.smartHDR),recipe:Catalogue.load().recipes[0],preferences:CapturePreferences());XCTAssertEqual(out.data,data);XCTAssertTrue(out.note.contains("fallback"))}
    func testMatrixUsesSRGBByteMath() throws {
        let cg=CIImage(color:CIColor(red:100.0/255,green:140.0/255,blue:180.0/255)).cropped(to:CGRect(x:0,y:0,width:2,height:2));var recipe=try Catalogue.load().recipes[0];recipe.con=2
        let actual=PhotoProcessor.matrix(cg,recipe:recipe,preferences:CapturePreferences());var bytes=[UInt8](repeating:0,count:16)
        PhotoProcessor().context.render(actual,toBitmap:&bytes,rowBytes:8,bounds:actual.extent,format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
        let matrix=ColourMatrix.recipe(recipe).values
        for i in 0..<3 {let expected=clamp(Int((Double([100,140,180][i])*matrix[i*5+i]+matrix[i*5+4]).rounded()),0,255);XCTAssertLessThanOrEqual(abs(Int(bytes[i])-expected),2)}
    }
    func testHDRIdentityStripOrientationAndColour() throws {
        let data=fixture();let frames=(0..<4).map {frame(data,id:Int64($0+1))};let burst=PhotoBurst(frames:frames,shorts:[1,2,3],pipeline:.smartHDR)
        let merged=try JPEGHDRProcessor(context:PhotoProcessor().context).merge(burst).image
        let original=try XCTUnwrap(PhotoProcessor.upright(data));let context=PhotoProcessor().context
        var a=[UInt8](repeating:0,count:192*256*4),b=a
        context.render(original,toBitmap:&a,rowBytes:192*4,bounds:original.extent,format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
        context.render(merged,toBitmap:&b,rowBytes:192*4,bounds:merged.extent,format:.RGBA8,colorSpace:PhotoProcessor.colourSpace)
        for y in [10,79,80,159,160,240] {for x in [5,91,180] {for c in 0..<3 {XCTAssertLessThanOrEqual(abs(Int(a[(y*192+x)*4+c])-Int(b[(y*192+x)*4+c])),2,"Stripe/orientation mismatch at \(x),\(y)")}}}
    }
    func testRotatedExifIsAppliedBeforeCropAndReset() throws {
        var p=CapturePreferences();p.crop=2
        let out=try PhotoProcessor().process(PhotoBurst(frames:[frame(fixture(orientation:6))],pipeline:.original),recipe:Catalogue.load().recipes[0],preferences:p)
        XCTAssertEqual(out.width,128);XCTAssertEqual(out.height,96)
        let decoded=try XCTUnwrap(PhotoProcessor.upright(XCTUnwrap(out.data)))
        XCTAssertEqual(Int(decoded.extent.width),128);XCTAssertEqual(Int(decoded.extent.height),96)
    }

}
