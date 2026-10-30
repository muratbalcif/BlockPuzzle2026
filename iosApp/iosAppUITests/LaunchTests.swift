import XCTest

final class LaunchTests: XCTestCase {
    func testAppDisplaysTitle() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.staticTexts["BlockPuzzle2026"].firstMatch.waitForExistence(timeout: 15))
    }
    func testStartPauseAndResume() {
        let app = XCUIApplication()
        app.launch()
        let start = app.buttons["Oyuna başla"]
        for _ in 0..<8 {
            if start.isHittable { break }
            app.swipeUp()
        }
        XCTAssertTrue(start.isHittable)
        start.tap()
        let replace = app.buttons["Yeni oyun başlat"]
        if replace.waitForExistence(timeout: 1) { replace.tap() }
        let pause = app.buttons["Duraklat"]
        XCTAssertTrue(pause.waitForExistence(timeout: 15))
        pause.tap()
        let resume = app.buttons["Devam et"]
        XCTAssertTrue(resume.waitForExistence(timeout: 5))
        resume.tap()
        XCTAssertTrue(pause.waitForExistence(timeout: 5))
    }

    func testSavedGameSurvivesRelaunchPaused() {
        let app = XCUIApplication()
        app.launch()
        let start = app.buttons["Oyuna başla"]
        for _ in 0..<8 {
            if start.isHittable { break }
            app.swipeUp()
        }
        XCTAssertTrue(start.isHittable)
        start.tap()
        let replace = app.buttons["Yeni oyun başlat"]
        if replace.waitForExistence(timeout: 1) { replace.tap() }
        let pause = app.buttons["Duraklat"]
        XCTAssertTrue(pause.waitForExistence(timeout: 15))
        pause.tap()
        guard let home = app.buttons.matching(identifier: "Ana ekran").allElementsBoundByIndex.first(where: { $0.isHittable }) else {
            XCTFail("Paused home control is missing")
            return
        }
        home.tap()
        let save = app.buttons["Kaydet ve dön"]
        XCTAssertTrue(save.waitForExistence(timeout: 5))
        save.tap()
        XCTAssertTrue(app.staticTexts["Cihazda kayıtlı."].waitForExistence(timeout: 10))
        app.terminate()
        app.launch()
        let resume = app.buttons["Kayıtlı oyuna devam"]
        XCTAssertTrue(resume.waitForExistence(timeout: 10))
        if !resume.isHittable { app.swipeUp() }
        resume.tap()
        XCTAssertTrue(app.staticTexts["Oyun duraklatıldı"].waitForExistence(timeout: 10))
    }

}
