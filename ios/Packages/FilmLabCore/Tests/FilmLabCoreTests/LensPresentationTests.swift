import XCTest
import FilmLabCore

final class LensPresentationTests: XCTestCase {
    private func rear(_ id: String, _ kind: CameraLensKind) -> CameraLensMetadata {
        CameraLensMetadata(id: id, facing: .back, kind: kind)
    }
    private func triple(_ factors: [Double] = [2, 10]) -> [CameraLensMetadata] {
        [rear("ultra", .ultraWide), rear("main", .wide), rear("tele", .telephoto),
         CameraLensMetadata(id: "triple", facing: .back, kind: .other, virtual: true, constituents: ["ultra", "main", "tele"], switchFactors: factors)]
    }
    func testTripleCameraUsesRuntimeRatiosAndCleanRearChoices() {
        let all = LensPresentation.lenses(triple())
        let primary = LensPresentation.primaryRear(all)
        XCTAssertEqual(primary.map(\.id), ["ultra", "main", "tele"])
        XCTAssertEqual(primary.map(\.shortName), ["0.5×", "1×", "5×"])
        XCTAssertEqual(LensPresentation.preferredRearID(all), "main")
        XCTAssertTrue(primary.allSatisfy { !$0.virtual && $0.facing == .back })
    }
    func testDoesNotHardcodeTelephotoOrInventTwoTimesLens() {
        let primary = LensPresentation.primaryRear(LensPresentation.lenses(triple([2, 14])))
        XCTAssertEqual(primary.last?.shortName, "7×")
        XCTAssertFalse(primary.contains { $0.shortName == "2×" })
    }
    func testDualWithoutUltraWideKeepsWideAtOneTimes() {
        let metadata = [rear("main", .wide), rear("tele", .telephoto), CameraLensMetadata(id: "dual", facing: .back, kind: .other, virtual: true, constituents: ["main", "tele"], switchFactors: [3])]
        let primary = LensPresentation.primaryRear(LensPresentation.lenses(metadata))
        XCTAssertEqual(primary.map(\.shortName), ["1×", "3×"])
    }
    func testInvalidCrossoverMetadataFallsBackToNamesNotGuesses() {
        for factors in [[2.0], [2, Double.nan], [2, 1], [0, 10], [2, Double.infinity]] {
            let primary = LensPresentation.primaryRear(LensPresentation.lenses(triple(factors)))
            XCTAssertEqual(primary.map(\.shortName), ["Ultra", "1×", "Tele"])
        }
    }
    func testPhysicalOnlyMetadataDoesNotGuessOpticalRatios() {
        let primary = LensPresentation.primaryRear(LensPresentation.lenses([rear("ultra", .ultraWide), rear("main", .wide), rear("tele", .telephoto)]))
        XCTAssertEqual(primary.map(\.shortName), ["Ultra", "1×", "Tele"])
        XCTAssertNil(primary.first?.relativeZoom)
        XCTAssertEqual(LensPresentation.zoomLabel(Double.nan), "Lens")
        XCTAssertEqual(LensPresentation.zoomLabel(3.7), "3.7×")
    }
    func testKeepsEveryDistinctTelephotoDevice() {
        let metadata = [rear("ultra", .ultraWide), rear("main", .wide), rear("tele3", .telephoto), rear("tele6", .telephoto), CameraLensMetadata(id: "multi", facing: .back, kind: .other, virtual: true, constituents: ["ultra", "main", "tele3", "tele6"], switchFactors: [2, 6, 12])]
        let primary = LensPresentation.primaryRear(LensPresentation.lenses(metadata))
        XCTAssertEqual(primary.map(\.shortName), ["0.5×", "1×", "3×", "6×"])
        XCTAssertEqual(Set(primary.map(\.id)).count, 4)
    }
    func testDeduplicatesIDsButNotDifferentDevices() {
        let main = rear("main", .wide)
        let lenses = LensPresentation.lenses([main, main, rear("second", .telephoto)])
        XCTAssertEqual(lenses.count, 2)
        XCTAssertEqual(Set(lenses.map(\.id)), ["main", "second"])
    }
    func testFrontAndVirtualAlternativesStayOutOfRearRow() {
        let metadata = triple() + [CameraLensMetadata(id: "front-wide", facing: .front, kind: .wide), CameraLensMetadata(id: "front-depth", facing: .front, kind: .trueDepth, virtual: true), CameraLensMetadata(id: "dual-extra", facing: .back, kind: .other, virtual: true, constituents: ["main", "tele"], switchFactors: [5])]
        let lenses = LensPresentation.lenses(metadata)
        XCTAssertEqual(LensPresentation.primaryRear(lenses).count, 3)
        XCTAssertEqual(LensPresentation.preferredFrontID(lenses), "front-depth")
        XCTAssertEqual(lenses.count, 7, "Advanced routes are retained, not deleted")
    }
    func testCollidingPhysicalLabelsRemainUsableAndDistinct() {
        let lenses = LensPresentation.primaryRear(LensPresentation.lenses([rear("main-a", .wide), rear("main-b", .wide)]))
        XCTAssertEqual(lenses.count, 2)
        XCTAssertEqual(Set(lenses.map(\.shortName)), ["Main 1", "Main 2"])
    }
}
