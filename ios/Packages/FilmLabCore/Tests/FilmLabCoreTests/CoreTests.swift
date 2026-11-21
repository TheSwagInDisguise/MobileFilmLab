import XCTest
@testable import FilmLabCore

final class CoreTests:XCTestCase {
    func testFullCatalogueAndAttributionPreservingIDs() throws {let c=try Catalogue.load();XCTAssertEqual(c.recipes.count,80);XCTAssertEqual(c.recipes.filter {$0.group != "OPPO"}.count,77);XCTAssertEqual(c.recipes.first?.displayName,"iPhone Natural (AVFoundation)");XCTAssertEqual(c.recipes.first?.id,"stock_oppo_natural")}
    func testEveryMatrixMatchesAndroidVectors() throws {
        struct Vector:Decodable {var id:String;var tone:Double;var color:Double;var matrix:[Double]}
        let catalogue=try Catalogue.load();let url=try XCTUnwrap(Bundle.module.url(forResource:"colour-vectors",withExtension:"json"));let vectors=try JSONDecoder().decode([Vector].self,from:Data(contentsOf:url))
        XCTAssertEqual(vectors.count,160)
        for v in vectors {var r=try XCTUnwrap(catalogue.recipes.first {$0.id==v.id});r.puckTone=v.tone;r.puckColor=v.color;let actual=ColourMatrix.recipe(r).values;for i in 0..<20 {XCTAssertEqual(actual[i],v.matrix[i],accuracy:0.00001,"\(v.id) coefficient \(i)")}}
    }
    func testNaturalIsIdentityAndColourControlsAreBounded() throws {var r=try XCTUnwrap(Catalogue.load().recipes.first);XCTAssertTrue(ColourMatrix.recipe(r).isIdentity);r.puckTone=100;XCTAssertFalse(ColourMatrix.recipe(r).isIdentity);r.puckTone=Double.nan;XCTAssertThrowsError(try r.validate())}
    func testJSONAndQRRoundTripAllRecipes() throws {for r in try Catalogue.load().recipes {XCTAssertEqual(try Interchange.decode(Interchange.encode([r])),[r]);XCTAssertEqual(try Interchange.decodeQR(Interchange.qrPayload(r)),[r])}}
    func testLegacyArrayAndInvalidFormats() throws {let r=try Catalogue.load().recipes[0];let data=try JSONEncoder().encode([r]);XCTAssertEqual(try Interchange.decode(data),[r]);XCTAssertThrowsError(try Interchange.decode(Data("{}".utf8)));XCTAssertThrowsError(try Interchange.decode(Data(repeating:32,count:262145)));XCTAssertThrowsError(try Interchange.decodeQR("FILMRECIPE:1:!!"))}
    func testRawNormalizesEveryHDRModeToSingleUncroppedCapture() {for f in CaptureFormat.allCases where f.writesRaw {for p in CapturePipeline.allCases {var s=CapturePreferences();s.format=f;s.pipeline=p;s.crop=2;s.responsive=true;s.normalize();XCTAssertEqual(s.pipeline,.original);XCTAssertEqual(s.crop,1);XCTAssertFalse(s.responsive)}}}
    func testAllRecipeNamesAndGroupsValidate() throws {let c=try Catalogue.load();XCTAssertEqual(c.groups.count,14);for r in c.recipes {XCTAssertNoThrow(try r.validate())}}
    func testDecodeJavaDeflaterQRAndEnvelopeFixture() throws {
        struct Fixture:Decodable {var document:Document;var androidDeflaterQR:String}
        struct Document:Codable {var type:String;var version:Int;var recipes:[Recipe]}
        let url=try XCTUnwrap(Bundle.module.url(forResource:"interchange-fixtures",withExtension:"json"))
        let fixture=try JSONDecoder().decode(Fixture.self,from:Data(contentsOf:url))
        XCTAssertEqual(try Interchange.decodeQR(fixture.androidDeflaterQR),fixture.document.recipes)
        XCTAssertEqual(try Interchange.decode(JSONEncoder().encode(fixture.document)),fixture.document.recipes)
    }

}
