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
        XCTAssertTrue(stationName.waitForExistence(timeout: 30), "Station name should appear after loading")

        takeScreenshot(name: "1-popover-arrivals")
    }

    func test2_ConfigureTflStation() {
        openSettings(displayStyle: "Dot Matrix", transitSystem: "London (TfL)")
        clearSelectedStop()

        selectTflShoreditch()

        takeScreenshot(name: "2a-tfl-station-selected")

        setPlatform("2")

        takeScreenshot(name: "2b-tfl-platform-set")

        saveAndVerifyPopoverUpdate()
        takeScreenshot(name: "2c-tfl-popover-updated")
    }

    func test3_ConfigureMtaStation() {
        openSettings(displayStyle: "Dot Matrix", transitSystem: "NYC (MTA)")
        clearSelectedStop()

        let linePicker = app.popUpButtons["linePicker"]
        XCTAssertTrue(linePicker.waitForExistence(timeout: 5))
        linePicker.click()
        app.menuItems["ACE"].click()

        let filterField = app.textFields["stopFilterField"]
        XCTAssertTrue(filterField.waitForExistence(timeout: 60), "Stop filter field should appear after load")
        filterField.click()
        filterField.typeText("Hoyt")

        let resultsList = app.outlines["searchResultsList"].firstMatch
        XCTAssertTrue(resultsList.waitForExistence(timeout: 5), "Filtered results should appear")

        let hoyt = resultsList.staticTexts.matching(
            NSPredicate(format: "value CONTAINS 'Hoyt-Schermerhorn'")
        ).firstMatch
        XCTAssertTrue(hoyt.waitForExistence(timeout: 5), "Hoyt-Schermerhorn should appear in filtered results")
        hoyt.click()

        takeScreenshot(name: "3a-mta-stop-selected")

        saveAndVerifyPopoverUpdate()
        takeScreenshot(name: "3b-mta-popover-updated")
    }

    func test4_SwitchToLcdDisplay() {
        openSettings(displayStyle: "LCD", transitSystem: "London (TfL)")
        clearSelectedStop()

        selectTflShoreditch()
        setPlatform("2")

        takeScreenshot(name: "4a-lcd-settings")

        saveAndVerifyPopoverUpdate()
        takeScreenshot(name: "4b-lcd-popover")
    }

    func test5_DisplayStyleOnlySaveKeepsStopConfig() {
        openSettings(displayStyle: "Dot Matrix", transitSystem: "London (TfL)")
        clearSelectedStop()
        selectTflShoreditch()
        setPlatform("2")
        saveAndVerifyPopoverUpdate()

        // Custom GTFS fields are empty for a TfL user, so Save is only enabled by
        // the display style change and must not commit the invalid GTFS config
        openSettings(displayStyle: "LCD", transitSystem: "Custom GTFS")
        let saveButton = app.buttons["saveButton"]
        XCTAssertTrue(saveButton.isEnabled, "Save should be enabled by the display style change alone")
        saveButton.click()

        let popover = app.popovers.firstMatch
        XCTAssertTrue(popover.waitForExistence(timeout: 10), "Popover should reappear after save")
        let stationName = popover.staticTexts["stationName"]
        XCTAssertTrue(stationName.waitForExistence(timeout: 30), "Arrivals should still load, not error")

        takeScreenshot(name: "5a-popover-after-style-only-save")

        popover.buttons["settingsButton"].click()
        XCTAssertTrue(app.buttons["saveButton"].waitForExistence(timeout: 5), "Settings window should open")

        let transitPicker = app.popUpButtons["transitSystemPicker"]
        XCTAssertTrue(transitPicker.waitForExistence(timeout: 5))
        XCTAssertEqual(transitPicker.value as? String, "London (TfL)", "Transit system should still be TfL")

        let selectedStop = app.staticTexts["selectedStopName"]
        XCTAssertTrue(selectedStop.waitForExistence(timeout: 5), "Stop should still be selected")
        XCTAssertTrue(
            (selectedStop.value as? String ?? "").contains("Shoreditch High Street"),
            "Stop should still be Shoreditch High Street"
        )

        takeScreenshot(name: "5b-settings-config-intact")
    }

    func test6_ConfigureTflBusStop() {
        openSettings(displayStyle: "Dot Matrix", transitSystem: "London Buses (TfL)")
        clearSelectedStop()

        let searchField = app.textFields["searchField"]
        XCTAssertTrue(searchField.waitForExistence(timeout: 5))
        searchField.click()
        searchField.typeText("east dulwich")

        let resultsList = app.outlines["searchResultsList"].firstMatch
        XCTAssertTrue(resultsList.waitForExistence(timeout: 30), "Search results should appear")

        // Only single stops get a letter, so this can't match a stop group
        // that would need a second disambiguation step
        let stop = resultsList.staticTexts.matching(
            NSPredicate(format: "value BEGINSWITH 'East Dulwich Station ('")
        ).firstMatch
        XCTAssertTrue(stop.waitForExistence(timeout: 5), "A lettered East Dulwich Station stop should appear")
        stop.click()

        let routeField = app.textFields["routeField"]
        XCTAssertTrue(routeField.waitForExistence(timeout: 5), "Route filter should appear once a stop is selected")

        takeScreenshot(name: "6a-tfl-bus-stop-selected")

        saveAndVerifyPopoverUpdate()
        takeScreenshot(name: "6b-tfl-bus-popover-updated")
    }

    // Doesn't save: test6 covers that, and a bus station bay can have no arrivals overnight
    func test7_SelectTflBusStopFromGroup() {
        openSettings(displayStyle: "Dot Matrix", transitSystem: "London Buses (TfL)")
        clearSelectedStop()

        let searchField = app.textFields["searchField"]
        XCTAssertTrue(searchField.waitForExistence(timeout: 5))
        searchField.click()
        searchField.typeText("victoria road")

        let resultsList = app.outlines["searchResultsList"].firstMatch
        XCTAssertTrue(resultsList.waitForExistence(timeout: 30), "Search results should appear")

        // London has ten stop groups called just "Victoria Road". This search
        // returns no single stops, so any "towards" subtitle belongs to a group
        let labelledGroup = resultsList.staticTexts.matching(
            NSPredicate(format: "value BEGINSWITH 'towards '")
        ).firstMatch
        XCTAssertTrue(labelledGroup.waitForExistence(timeout: 5), "Same-named groups should say where their stops head")
        takeScreenshot(name: "7a-tfl-bus-groups-labelled")

        searchField.click()
        searchField.typeKey("a", modifierFlags: .command)
        searchField.typeText("canada water")

        // An interchange whose bus stops sit two levels down, inside its bus station
        let interchange = resultsList.staticTexts.matching(NSPredicate(format: "value == 'Canada Water'")).firstMatch
        XCTAssertTrue(interchange.waitForExistence(timeout: 30), "Canada Water interchange should appear")
        interchange.click()

        let stop = resultsList.staticTexts.matching(
            NSPredicate(format: "value BEGINSWITH 'Canada Water Bus Station ('")
        ).firstMatch
        XCTAssertTrue(stop.waitForExistence(timeout: 30), "The interchange should expand to lettered bus stops")
        takeScreenshot(name: "7b-tfl-bus-group-expanded")
        stop.click()

        let routeField = app.textFields["routeField"]
        XCTAssertTrue(routeField.waitForExistence(timeout: 5), "Route filter should appear once a stop is selected")
        XCTAssertTrue(app.buttons["saveButton"].isEnabled, "A stop picked from a group should be saveable")
        takeScreenshot(name: "7c-tfl-bus-stop-from-group-selected")
    }

    private func selectTflShoreditch() {
        let searchField = app.textFields["searchField"]
        XCTAssertTrue(searchField.waitForExistence(timeout: 5))
        searchField.click()
        searchField.typeText("shoreditch")

        let resultsList = app.outlines["searchResultsList"].firstMatch
        XCTAssertTrue(resultsList.waitForExistence(timeout: 30), "Search results should appear")

        let shoreditch = resultsList.staticTexts.matching(
            NSPredicate(format: "value CONTAINS 'Shoreditch High Street'")
        ).firstMatch
        XCTAssertTrue(shoreditch.waitForExistence(timeout: 5), "Shoreditch High Street should appear in results")
        shoreditch.click()
    }

    private func openSettings(displayStyle: String, transitSystem: String) {
        let popover = app.popovers.firstMatch
        XCTAssertTrue(popover.waitForExistence(timeout: 10))

        popover.buttons["settingsButton"].click()
        XCTAssertTrue(app.buttons["saveButton"].waitForExistence(timeout: 5), "Settings window should open")

        let stylePicker = app.popUpButtons["displayStylePicker"]
        XCTAssertTrue(stylePicker.waitForExistence(timeout: 5))
        stylePicker.click()
        app.menuItems[displayStyle].click()

        let transitPicker = app.popUpButtons["transitSystemPicker"]
        XCTAssertTrue(transitPicker.waitForExistence(timeout: 5))
        transitPicker.click()
        app.menuItems[transitSystem].click()
    }

    private func setPlatform(_ value: String) {
        let platformField = app.textFields["platformField"]
        XCTAssertTrue(platformField.waitForExistence(timeout: 5))
        platformField.click()
        // Pre-filled so clear before setting a new platform
        platformField.typeKey("a", modifierFlags: .command)
        platformField.typeText(value)
    }

    private func clearSelectedStop() {
        let changeButton = app.buttons["changeStopButton"]
        if changeButton.waitForExistence(timeout: 2) {
            changeButton.click()
        }
    }

    private func saveAndVerifyPopoverUpdate() {
        app.buttons["saveButton"].click()

        let popover = app.popovers.firstMatch
        XCTAssertTrue(popover.waitForExistence(timeout: 10), "Popover should reappear after save")

        let stationName = popover.staticTexts["stationName"]
        XCTAssertTrue(stationName.waitForExistence(timeout: 30), "Station name should appear after save")
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
