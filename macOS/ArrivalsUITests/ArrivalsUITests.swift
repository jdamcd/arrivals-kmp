import XCTest

@MainActor
final class ArrivalsUITests: XCTestCase {
    let app = XCUIApplication()

    override func setUp() async throws {
        continueAfterFailure = false
        app.launchArguments = ["--show-popover"]
        app.launch()
    }

    override func tearDown() async throws {
        await MainActor.run {
            app.terminate()
        }
    }

    func test1_PopoverShowsArrivals() {
        let popover = app.popovers.firstMatch
        XCTAssertTrue(popover.waitForExistence(timeout: 10), "Popover should appear")

        let stationName = popover.staticTexts["stationName"]
        XCTAssertTrue(stationName.waitForExistence(timeout: 15), "Station name should appear after loading")

        takeScreenshot(name: "1-popover-arrivals")
    }

    func test2_ConfigureTflStation() {
        openSettings(transitSystem: "London (TfL)", displayStyle: "Dot Matrix")

        let searchField = app.textFields["searchField"]
        XCTAssertTrue(searchField.waitForExistence(timeout: 5))
        searchField.click()
        searchField.typeText("shoreditch")

        let resultsList = app.outlines["searchResultsList"].firstMatch
        XCTAssertTrue(resultsList.waitForExistence(timeout: 15), "Search results should appear")

        let shoreditch = resultsList.staticTexts.matching(
            NSPredicate(format: "value CONTAINS 'Shoreditch High Street'")
        ).firstMatch
        XCTAssertTrue(shoreditch.waitForExistence(timeout: 5), "Shoreditch High Street should appear in results")
        shoreditch.click()

        takeScreenshot(name: "2a-tfl-station-selected")

        let platformField = app.textFields["platformField"]
        XCTAssertTrue(platformField.waitForExistence(timeout: 5))
        platformField.click()
        platformField.typeText("2")

        takeScreenshot(name: "2b-tfl-platform-set")

        saveAndVerifyPopoverUpdate()
        takeScreenshot(name: "2c-tfl-popover-updated")
    }

    func test3_ConfigureMtaStation() {
        openSettings(transitSystem: "NYC (MTA)", displayStyle: "Dot Matrix")

        let linePicker = app.popUpButtons["linePicker"]
        XCTAssertTrue(linePicker.waitForExistence(timeout: 5))
        linePicker.click()
        app.menuItems["ACE"].click()

        let resultsList = app.outlines["searchResultsList"].firstMatch
        XCTAssertTrue(resultsList.waitForExistence(timeout: 30), "Stop list should load")

        let hoyt = resultsList.staticTexts.matching(
            NSPredicate(format: "value CONTAINS 'Hoyt-Schermerhorn'")
        ).firstMatch
        XCTAssertTrue(hoyt.waitForExistence(timeout: 5), "Hoyt-Schermerhorn should appear in stop list")

        let scrollView = resultsList.scrollViews.firstMatch.exists
            ? resultsList.scrollViews.firstMatch
            : resultsList
        XCTAssertTrue(scrollView.scrollToElement(hoyt), "Should be able to scroll to Hoyt-Schermerhorn")
        hoyt.click()

        takeScreenshot(name: "3a-mta-stop-selected")

        saveAndVerifyPopoverUpdate()
        takeScreenshot(name: "3b-mta-popover-updated")
    }

    func test4_SwitchToLcdDisplay() {
        openSettings(transitSystem: "London (TfL)", displayStyle: "LCD")

        let searchField = app.textFields["searchField"]
        XCTAssertTrue(searchField.waitForExistence(timeout: 5))
        searchField.click()
        searchField.typeText("shoreditch")

        let resultsList = app.outlines["searchResultsList"].firstMatch
        XCTAssertTrue(resultsList.waitForExistence(timeout: 15), "Search results should appear")

        let shoreditch = resultsList.staticTexts.matching(
            NSPredicate(format: "value CONTAINS 'Shoreditch High Street'")
        ).firstMatch
        XCTAssertTrue(shoreditch.waitForExistence(timeout: 5))
        shoreditch.click()

        takeScreenshot(name: "4a-lcd-settings")

        saveAndVerifyPopoverUpdate()
        takeScreenshot(name: "4b-lcd-popover")
    }

    private func openSettings(transitSystem: String, displayStyle: String) {
        let popover = app.popovers.firstMatch
        XCTAssertTrue(popover.waitForExistence(timeout: 10))

        popover.buttons["settingsButton"].click()
        XCTAssertTrue(app.buttons["saveButton"].waitForExistence(timeout: 5), "Settings window should open")

        let transitPicker = app.popUpButtons["transitSystemPicker"]
        XCTAssertTrue(transitPicker.waitForExistence(timeout: 5))
        transitPicker.click()
        app.menuItems[transitSystem].click()

        let stylePicker = app.popUpButtons["displayStylePicker"]
        XCTAssertTrue(stylePicker.waitForExistence(timeout: 5))
        stylePicker.click()
        app.menuItems[displayStyle].click()
    }

    private func saveAndVerifyPopoverUpdate() {
        app.buttons["saveButton"].click()

        let popover = app.popovers.firstMatch
        XCTAssertTrue(popover.waitForExistence(timeout: 10), "Popover should reappear after save")

        let stationName = popover.staticTexts["stationName"]
        XCTAssertTrue(stationName.waitForExistence(timeout: 15), "Station name should appear after save")
    }

    private func takeScreenshot(name: String) {
        let target: XCUIScreenshotProviding = if app.windows.firstMatch.exists {
            app.windows.firstMatch
        } else if app.popovers.firstMatch.exists {
            app.popovers.firstMatch
        } else {
            XCUIScreen.main
        }
        let screenshot = target.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}

extension XCUIElement {
    // Scroll until element is hittable, stopping if the scroll boundary is reached.
    @discardableResult
    func scrollToElement(_ element: XCUIElement, deltaY: CGFloat = -150, maxScrolls: Int = 40) -> Bool {
        var lastFrame = element.frame
        var stuckCount = 0

        for _ in 0 ..< maxScrolls {
            if element.isHittable { return true }
            scroll(byDeltaX: 0, deltaY: deltaY)

            let newFrame = element.frame
            if newFrame == lastFrame {
                stuckCount += 1
                if stuckCount >= 3 { return false }
            } else {
                stuckCount = 0
            }
            lastFrame = newFrame
        }
        return element.isHittable
    }
}
