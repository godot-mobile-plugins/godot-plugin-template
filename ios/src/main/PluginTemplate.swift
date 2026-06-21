//
// © 2026-present https://github.com/<<GitHubUsername>>
//

import Foundation

@objc public class PluginTemplate: NSObject {

	// TODO: callbacks to be set by the Objective-C bridge
	@objc public var onThisHappened: ((_ info: [String: Any]) -> Void)?

	// TODO: members accessible from Objective-C
	@objc static let isActiveKey = "is_active"

	override init() {
		super.init()
		// TODO
	}

	deinit {
		// TODO
	}
}
