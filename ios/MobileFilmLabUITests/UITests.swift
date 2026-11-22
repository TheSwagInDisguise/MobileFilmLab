import XCTest
import UIKit

final class UITests: XCTestCase {
    @MainActor func testNativeScreensAndCompleteCatalogueAreAccessible() throws {
        let app = XCUIApplication(); app.launch()
        addUIInterruptionMonitor(withDescription: "Camera permission") { alert in
            if alert.buttons["Allow"].exists { alert.buttons["Allow"].tap(); return true }
            if alert.buttons["OK"].exists { alert.buttons["OK"].tap(); return true }
            return false
        }
        if app.alerts.firstMatch.waitForExistence(timeout: 2) {
            let alert = app.alerts.firstMatch
            if alert.buttons["Allow"].exists { alert.buttons["Allow"].tap() }
            else if alert.buttons["OK"].exists { alert.buttons["OK"].tap() }
        }
        XCTAssertTrue(app.buttons["openSettings"].waitForExistence(timeout: 10))
        selectJPEG(app)
        screenshot("SIMULATOR-no-handset-camera")
        app.buttons["openSettings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Capture pipeline"].exists || app.staticTexts["CAPTURE PIPELINE"].exists)
        screenshot("SIMULATOR-settings")
        app.navigationBars["Settings"].buttons["Done"].tap()
        let crop = app.buttons["jpegCrop"]
        XCTAssertTrue(crop.waitForExistence(timeout: 5)); crop.tap()
        XCTAssertTrue(app.buttons["1.5× JPEG crop"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["2× JPEG crop"].exists)
        XCTAssertTrue(app.buttons["3× JPEG crop"].exists)
        screenshot("SIMULATOR-four-JPEG-crop-choices")
        app.buttons["1.5× JPEG crop"].tap()
        XCTAssertEqual(crop.value as? String, "1.5× JPEG crop")
        XCTAssertTrue(app.staticTexts["1.5× crop"].exists)
        crop.tap(); app.buttons["1× · full frame"].tap()
        app.buttons["openRecipes"].tap()
        XCTAssertTrue(app.navigationBars["The look library"].waitForExistence(timeout: 5))
        screenshot("SIMULATOR-full-catalogue")
        XCTAssertTrue(app.buttons.containing(NSPredicate(format: "label CONTAINS %@", "All 80")).firstMatch.exists)
        app.buttons["Import or export recipes"].tap(); app.buttons["Export current recipe"].tap()
        let nativeShare = app.otherElements["mfl-share-sheet"]
        XCTAssertTrue(nativeShare.waitForExistence(timeout: 10) || app.buttons["Copy"].exists, "Recipe export must present above the recipe drawer")
        screenshot("SIMULATOR-native-recipe-export"); app.terminate()
    }

    @MainActor func testSamplePhysicalLensesFrontSwitchAndAdvancedRoutes() throws {
        let app = fixtureApp(); defer { app.terminate() }
        let rowButtons = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "lens-"))
        XCTAssertEqual(rowButtons.count, 3, "Only physical rear devices belong in the normal row")
        XCTAssertEqual(app.buttons["lens-fixture-ultra"].label, "Ultra Wide, 0.5×")
        XCTAssertEqual(app.buttons["lens-fixture-main"].label, "Main, 1×")
        XCTAssertEqual(app.buttons["lens-fixture-tele"].label, "Telephoto, 5×")
        XCTAssertFalse(app.buttons["lens-fixture-auto"].exists)
        XCTAssertFalse(app.buttons["lens-fixture-front"].exists)
        XCTAssertFalse(app.buttons["lens-fixture-dual"].exists)
        XCTAssertFalse(app.buttons["shutter"].isEnabled, "The metadata fixture must not enable camera capture")
        XCTAssertFalse(app.staticTexts["ISO 100 · 1/100s"].exists, "Technical readouts are not part of AUTO")
        screenshot("SIMULATOR-sample-lenses-AUTO")
        app.buttons["lens-fixture-tele"].tap()
        XCTAssertEqual(app.buttons["lens-fixture-tele"].value as? String, "Selected")
        app.buttons["flipCamera"].tap()
        XCTAssertTrue(app.buttons["lens-fixture-front"].waitForExistence(timeout: 5))
        XCTAssertEqual(rowButtons.count, 1)
        XCTAssertFalse(app.buttons["lens-fixture-main"].exists)
        screenshot("SIMULATOR-sample-front-camera")
        app.buttons["flipCamera"].tap()
        XCTAssertTrue(app.buttons["lens-fixture-tele"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.buttons["lens-fixture-tele"].value as? String, "Selected", "Flipping back remembers the physical rear lens")
        app.buttons["openSettings"].tap()
        let routing = app.buttons["Camera routing"]
        reveal(routing, in: app); XCTAssertTrue(routing.exists); routing.tap()
        let diagnostics = app.buttons["openDiagnostics"]
        reveal(diagnostics, in: app); diagnostics.tap()
        XCTAssertTrue(app.navigationBars["Diagnostics"].waitForExistence(timeout: 5))
        let automatic = app.buttons["useRoute-fixture-auto"]
        reveal(automatic, in: app); XCTAssertTrue(automatic.exists); automatic.tap()
        app.navigationBars["Diagnostics"].buttons["Done"].tap()
        app.navigationBars["Settings"].buttons["Done"].tap()
        XCTAssertTrue(app.buttons["lens-fixture-auto"].waitForExistence(timeout: 5))
        XCTAssertEqual(rowButtons.count, 4, "An explicitly selected automatic route stays visible and escapable")
        XCTAssertEqual(app.buttons["lens-fixture-auto"].value as? String, "Selected")
        screenshot("SIMULATOR-sample-advanced-route")
    }

    @MainActor func testProShowsOneSliderAtATimeAndAutoHidesTechnicalReadouts() throws {
        let app = fixtureApp(); defer { app.terminate() }
        XCTAssertEqual(app.sliders.count, 0)
        app.buttons["cameraMode"].tap()
        XCTAssertTrue(app.sliders["ISO"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.sliders.count, 1)
        XCTAssertTrue(app.staticTexts["ISO 100 · 1/100s"].exists)
        app.buttons["Adjust WB"].tap()
        XCTAssertTrue(app.sliders["WB"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.sliders.count, 1)
        screenshot("SIMULATOR-sample-lenses-PRO")
        app.buttons["cameraMode"].tap()
        XCTAssertEqual(app.sliders.count, 0)
        XCTAssertFalse(app.staticTexts["ISO 100 · 1/100s"].exists)
    }

    @MainActor func testRawHidesCropAndFrontCameraFallsBackToJPEGInSampleMetadata() throws {
        let app = fixtureApp(); defer { app.terminate() }
        app.buttons["jpegCrop"].tap(); app.buttons["2× JPEG crop"].tap()
        XCTAssertEqual(app.buttons["jpegCrop"].value as? String, "2× JPEG crop")
        app.buttons["captureFormat"].tap(); app.buttons["RAW (Bayer DNG)"].tap()
        XCTAssertEqual(app.buttons["captureFormat"].value as? String, "RAW (Bayer DNG)")
        XCTAssertFalse(app.buttons["jpegCrop"].exists, "RAW must remain uncropped")
        app.buttons["flipCamera"].tap()
        XCTAssertTrue(app.buttons["lens-fixture-front"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.buttons["captureFormat"].value as? String, "JPEG", "Unavailable RAW must not stay selected on the front route")
        XCTAssertTrue(app.buttons["jpegCrop"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.buttons["jpegCrop"].value as? String, "Full frame")
    }

    @MainActor func testNativePreviewSurfaceRendersUprightSyntheticPattern() throws {
        let app = XCUIApplication(); app.launchArguments = ["--ui-lens-fixture", "--ui-preview-pattern"]; app.launch()
        defer { app.terminate() }
        XCTAssertTrue(app.buttons["lens-fixture-main"].waitForExistence(timeout: 10))
        selectJPEG(app)
        app.buttons["jpegCrop"].tap(); app.buttons["1× · full frame"].tap()
        let surface = app.otherElements["cameraPreviewSurface"]
        XCTAssertTrue(surface.waitForExistence(timeout: 10))
        expectation(for: NSPredicate(format: "value CONTAINS %@", "frame rendered"), evaluatedWith: surface)
        waitForExpectations(timeout: 20)
        XCTAssertFalse(app.buttons["shutter"].isEnabled, "The synthetic renderer test must not enable photo capture")
        XCTAssertTrue(app.staticTexts["SIMULATOR · SYNTHETIC PREVIEW TEST"].exists)
        let shot = app.screenshot(), frame = surface.frame
        for (x, y, expected) in [(0.25, 0.25, [255, 0, 0]), (0.75, 0.25, [0, 255, 0]), (0.25, 0.75, [0, 0, 255]), (0.75, 0.75, [255, 255, 255])] {
            let point = CGPoint(x: frame.minX + frame.width * CGFloat(x), y: frame.minY + frame.height * CGFloat(y))
            let pixel = try pixelRGB(in: shot, point: point, screen: app.frame)
            for channel in 0..<3 { XCTAssertLessThanOrEqual(abs(pixel[channel] - expected[channel]), 20, "Preview orientation/colour at \(x),\(y)") }
        }
        print("MFL_PREVIEW_UI_BACKEND=\(surface.value as? String ?? "unknown")")
        screenshot("SIMULATOR-synthetic-native-preview")
    }
    @MainActor private func pixelRGB(in screenshot: XCUIScreenshot, point: CGPoint, screen: CGRect) throws -> [Int] {
        let image = try XCTUnwrap(screenshot.image.cgImage)
        let x = floor((point.x - screen.minX) * CGFloat(image.width) / screen.width)
        let y = floor((point.y - screen.minY) * CGFloat(image.height) / screen.height)
        let cropped = try XCTUnwrap(image.cropping(to: CGRect(x: x, y: y, width: 1, height: 1)))
        var bytes = [UInt8](repeating: 0, count: 4)
        let context = try XCTUnwrap(CGContext(data: &bytes, width: 1, height: 1, bitsPerComponent: 8, bytesPerRow: 4, space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue))
        context.draw(cropped, in: CGRect(x: 0, y: 0, width: 1, height: 1))
        return bytes.prefix(3).map(Int.init)
    }

    @MainActor private func fixtureApp() -> XCUIApplication {
        let app = XCUIApplication(); app.launchArguments = ["--ui-lens-fixture"]; app.launch()
        XCTAssertTrue(app.buttons["lens-fixture-main"].waitForExistence(timeout: 10))
        selectJPEG(app)
        return app
    }
    @MainActor private func selectJPEG(_ app: XCUIApplication) {
        XCTAssertTrue(app.buttons["captureFormat"].waitForExistence(timeout: 5))
        app.buttons["captureFormat"].tap(); app.buttons["JPEG"].tap()
    }
    @MainActor private func reveal(_ element: XCUIElement, in app: XCUIApplication) {
        for _ in 0..<7 {
            if element.exists && element.isHittable { return }
            app.swipeUp()
        }
    }
    @MainActor private func screenshot(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIApplication().screenshot())
        attachment.name = name; attachment.lifetime = .keepAlways; add(attachment)
    }
}
