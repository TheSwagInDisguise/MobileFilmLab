import XCTest
@testable import FilmLabCore

final class HDRTests:XCTestCase {
    func testMovingHandheldShortsAndNoLongs() throws {
        let scene=SceneStats(clipped:0.03,percentile:253,shadows:0.4,motion:0.3,uptime:1)
        let p=try XCTUnwrap(HDRPolicy.plan(base:Exposure(iso:800,seconds:0.05),isoRange:25..0.6400,timeRange:0.00001..0.1,quality:false,scene:scene))
        XCTAssertEqual(p.count,5);XCTAssertTrue(p.allSatisfy {$0.seconds<=0.025 && !$0.long});XCTAssertLessThan(p[0].product,40)
    }
    func testQualityAllowsLongsOnlyWithQuietMeasuredMotion() throws {
        for motion in [Double?.none,0.2,0.01] {
            let scene=SceneStats(clipped:0.02,percentile:251,shadows:0.4,motion:motion,uptime:1)
            let p=try XCTUnwrap(HDRPolicy.plan(base:Exposure(iso:800,seconds:0.04),isoRange:25..0.6400,timeRange:0.00001..0.1,quality:true,scene:scene))
            XCTAssertEqual(p.filter(\.long).count,motion==0.01 ? 2 : 0);XCTAssertTrue(p.allSatisfy {$0.seconds<=0.25})
        }
    }
    func testInvalidExposureDoesNotPlan() {XCTAssertNil(HDRPolicy.plan(base:Exposure(iso:0,seconds:0),isoRange:25..0.6400,timeRange:0.0001..0.1,quality:false,scene:nil))}
    func testSubpixelTileAlignmentKnownTranslation() {
        let w=128,h=128
        let r=(0..<w*h).map {i in UInt8(((i%w)*17+(i/w)*31+((i%w)*(i/w))%67)%180+25)}
        let t=(0..<w*h).map {i in r[clamp(i/w+2,0,h-1)*w+clamp(i%w-3,0,w-1)]}
        let a=TileAlign.align(r,t,width:w,height:h),centre=a.sample(64,64)
        XCTAssertGreaterThan(a.fraction,0.55);XCTAssertEqual(centre.x,3,accuracy:0.6);XCTAssertEqual(centre.y,-2,accuracy:0.6)
    }
    func testAlignmentRejectsUnrelatedScene() {
        let w=96,h=96;var seed:UInt64=1
        func random()->[UInt8] {(0..<w*h).map {_ in seed=seed &* 6364136223846793005 &+ 1;return UInt8(truncatingIfNeeded:seed>>32)}}
        let a=TileAlign.align(random(),random(),width:w,height:h);XCTAssertLessThan(a.fraction,0.55)
    }
    func testMotionAndColourOutliersRejectAndQuietNoiseMerges() {
        let a=SIMD3<Float>(0.2,0.3,0.4)
        XCTAssertGreaterThan(HDRMath.weight(a+SIMD3(repeating:0.005),a,sigma:0.02,confidence:1),0.8)
        XCTAssertEqual(HDRMath.weight(SIMD3(0.9,0.9,0.9),a,sigma:0.02,confidence:1),0)
        XCTAssertEqual(HDRMath.weight(SIMD3(0.5,0.1,0.4),a,sigma:0.02,confidence:1),0)
    }
    func testToneCurveProtectsMidGreyAndHighlightWhite() {
        for white:Float in [1,1.2,2,4] {XCTAssertEqual(HDRMath.tone(0.18,white:white),0.18,accuracy:0.0001);XCTAssertEqual(HDRMath.tone(white,white:white),1,accuracy:0.0001);var previous:Float=0;for n in 0..0.100 {let value=HDRMath.tone(Float(n)/100*white,white:white);XCTAssertGreaterThanOrEqual(value,previous);previous=value}}
    }
    func testSRGBRoundtripEveryByte() {for n in 0..0.255 {XCTAssertLessThanOrEqual(abs(Int(HDRMath.encode(HDRMath.linear[n]))-n),1)}}
}
