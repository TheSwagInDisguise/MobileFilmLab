import XCTest

final class UITests:XCTestCase {
    @MainActor func testNativeScreensAndCompleteCatalogueAreAccessible() throws {
        let app=XCUIApplication();app.launch()
        addUIInterruptionMonitor(withDescription:"Camera permission") {alert in if alert.buttons["Allow"].exists {alert.buttons["Allow"].tap();return true};if alert.buttons["OK"].exists {alert.buttons["OK"].tap();return true};return false}
        if app.alerts.firstMatch.waitForExistence(timeout:2) {let a=app.alerts.firstMatch;if a.buttons["Allow"].exists {a.buttons["Allow"].tap()} else if a.buttons["OK"].exists {a.buttons["OK"].tap()}}
        XCTAssertTrue(app.buttons["openSettings"].waitForExistence(timeout:10));screenshot("SIMULATOR-no-handset-camera")
        app.buttons["openSettings"].tap();XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout:5));XCTAssertTrue(app.staticTexts["Capture pipeline"].exists || app.staticTexts["CAPTURE PIPELINE"].exists);screenshot("SIMULATOR-settings")
        app.buttons["Done"].firstMatch.tap();app.buttons["openRecipes"].tap();XCTAssertTrue(app.navigationBars["The look library"].waitForExistence(timeout:5));screenshot("SIMULATOR-full-catalogue")
        XCTAssertTrue(app.buttons.containing(NSPredicate(format:"label CONTAINS %@","All 80")).firstMatch.exists)
        app.buttons["Import or export recipes"].tap()
        app.buttons["Export current recipe"].tap()
        let nativeShare=app.otherElements["mfl-share-sheet"]
        XCTAssertTrue(nativeShare.waitForExistence(timeout:10) || app.buttons["Copy"].exists,"Recipe export must present above the recipe drawer")
        screenshot("SIMULATOR-native-recipe-export")
        app.terminate()
    }
    @MainActor private func screenshot(_ name:String) {let attachment=XCTAttachment(screenshot:XCUIApplication().screenshot());attachment.name=name;attachment.lifetime = .keepAlways;add(attachment)}
}
