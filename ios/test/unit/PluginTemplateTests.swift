//
// © 2026-present https://github.com/<<GitHubUsername>>
//

import XCTest
@testable import plugin_template_plugin

// MARK: - PluginTemplate Swift Class Tests

final class PluginTemplateTests: XCTestCase {

	// MARK: - Lifecycle

	var sut: PluginTemplate!

	override func setUp() {
		super.setUp()
		sut = PluginTemplate()
	}

	override func tearDown() {
		sut = nil
		super.tearDown()
	}

	// MARK: - Initialization

	func test_init_sut_isNotNil() {
		XCTAssertNotNil(sut, "PluginTemplate instance should be created successfully")
	}

	func test_init_onThisHappened_isNil() {
		XCTAssertNil(
			sut.onThisHappened,
			"onThisHappened callback should be nil until explicitly assigned"
		)
	}

	func test_init_multipleInstances_areIndependent() {
		let second = PluginTemplate()
		XCTAssertFalse(
			sut === second,
			"Each call to init should produce a distinct PluginTemplate instance"
		)
	}

	// MARK: - Static Keys

	func test_isActiveKey_value() {
		XCTAssertEqual(
			PluginTemplate.isActiveKey,
			"is_active",
			"isActiveKey must match the Godot-side dictionary key exactly"
		)
	}

	func test_isActiveKey_isNotEmpty() {
		XCTAssertFalse(
			PluginTemplate.isActiveKey.isEmpty,
			"isActiveKey must not be an empty string"
		)
	}

	// MARK: - onThisHappened Callback Assignment

	func test_onThisHappened_canBeAssigned() {
		let expectation = expectation(description: "onThisHappened callback is invoked")

		sut.onThisHappened = { _ in
			expectation.fulfill()
		}

		XCTAssertNotNil(sut.onThisHappened, "onThisHappened should be non-nil after assignment")
		sut.onThisHappened?([:])

		waitForExpectations(timeout: 1)
	}

	func test_onThisHappened_receivesCorrectInfo() {
		let expectedKey = "event"
		let expectedValue = "launched"
		var receivedInfo: [String: Any]?

		sut.onThisHappened = { info in
			receivedInfo = info
		}

		sut.onThisHappened?([expectedKey: expectedValue])

		XCTAssertNotNil(receivedInfo, "Callback should have been called with info dictionary")
		XCTAssertEqual(
			receivedInfo?[expectedKey] as? String,
			expectedValue,
			"Callback should forward the exact info dictionary it received"
		)
	}

	func test_onThisHappened_receivesEmptyDictionary() {
		var receivedInfo: [String: Any]?

		sut.onThisHappened = { info in
			receivedInfo = info
		}

		sut.onThisHappened?([:])

		XCTAssertNotNil(receivedInfo)
		XCTAssertTrue(receivedInfo?.isEmpty == true, "Callback should handle empty info dictionaries")
	}

	func test_onThisHappened_canBeCalledMultipleTimes() {
		var callCount = 0

		sut.onThisHappened = { _ in
			callCount += 1
		}

		sut.onThisHappened?([:])
		sut.onThisHappened?([:])
		sut.onThisHappened?([:])

		XCTAssertEqual(callCount, 3, "onThisHappened should be invocable multiple times")
	}

	func test_onThisHappened_canBeReassigned() {
		var firstCount = 0
		var secondCount = 0

		sut.onThisHappened = { _ in firstCount += 1 }
		sut.onThisHappened?([:])

		sut.onThisHappened = { _ in secondCount += 1 }
		sut.onThisHappened?([:])

		XCTAssertEqual(firstCount, 1, "First callback should only fire once")
		XCTAssertEqual(secondCount, 1, "Second callback should fire after reassignment")
	}

	func test_onThisHappened_canBeSetToNil() {
		sut.onThisHappened = { _ in XCTFail("This should not be called") }
		sut.onThisHappened = nil

		XCTAssertNil(sut.onThisHappened, "onThisHappened should be nil-able after assignment")
	}

	// MARK: - Objective-C Interoperability

	func test_isNSObject_subclass() {
		XCTAssertTrue(sut is NSObject, "PluginTemplate must inherit from NSObject for ObjC bridging")
	}

	func test_isActiveKey_isObjCAccessible() {
		// Verify the key survives an NSString round-trip (Obj-C bridge requirement)
		let nsKey = PluginTemplate.isActiveKey as NSString
		XCTAssertEqual(
			nsKey as String,
			PluginTemplate.isActiveKey,
			"isActiveKey must be losslessly bridgeable to NSString"
		)
	}

	func test_onThisHappened_receivesNSCompatibleDictionary() {
		// The Obj-C bridge passes NSDictionary which Swift sees as [String: Any].
		// Verify a value carried as AnyObject survives the round-trip.
		let bridgedValue: AnyObject = NSNumber(value: 42)
		var captured: [String: Any]?

		sut.onThisHappened = { info in captured = info }
		sut.onThisHappened?(["count": bridgedValue])

		XCTAssertEqual(
			captured?["count"] as? Int,
			42,
			"Integer values from NSDictionary should be accessible after bridging"
		)
	}
}

// MARK: - PluginTemplate Concurrency Tests

final class PluginTemplateConcurrencyTests: XCTestCase {

	func test_onThisHappened_calledOnMainThread() {
		let sut = PluginTemplate()
		let expectation = expectation(description: "Callback fires on main thread")

		sut.onThisHappened = { _ in
			XCTAssertTrue(Thread.isMainThread, "UI-bound callbacks should be dispatched on the main thread")
			expectation.fulfill()
		}

		DispatchQueue.main.async {
			sut.onThisHappened?([:])
		}

		waitForExpectations(timeout: 1)
	}

	func test_multipleInstances_doNotShareCallbackState() {
		let first = PluginTemplate()
		let second = PluginTemplate()
		var firstFired = false
		var secondFired = false

		first.onThisHappened = { _ in firstFired = true }
		second.onThisHappened = { _ in secondFired = true }

		first.onThisHappened?([:])

		XCTAssertTrue(firstFired, "First instance callback should fire")
		XCTAssertFalse(secondFired, "Second instance callback must not be affected by first")
	}
}
