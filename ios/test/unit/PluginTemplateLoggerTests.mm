//
// © 2026-present https://github.com/<<GitHubUsername>>
//

#import <XCTest/XCTest.h>
#import <os/log.h>

#import "plugin_template_logger.h"

// MARK: - Logger Tests

@interface PluginTemplateLoggerTests : XCTestCase
@end

@implementation PluginTemplateLoggerTests

/// The __attribute__((constructor)) in plugin_template_logger.mm runs before
/// any test code, so by the time this suite executes the global is already set.

- (void)test_pluginTemplateLog_isNotNil {
	XCTAssertNotEqual(plugin_template_log, (os_log_t)NULL,
		@"plugin_template_log must be initialized before any test runs");
}

- (void)test_pluginTemplateLog_isValidOSLogObject {
	// os_log objects respond to _os_log_impl; the safest observable check is
	// that the pointer is non-null and that a debug-level write does not crash.
	XCTAssertNoThrow(
		os_log_debug(plugin_template_log, "PluginTemplateLoggerTests: sanity write"),
		@"Writing a debug message to plugin_template_log must not throw or crash"
	);
}

- (void)test_pluginTemplateLog_acceptsFormatArguments {
	XCTAssertNoThrow({
		os_log_debug(plugin_template_log,
			"PluginTemplateLoggerTests: int=%d float=%f string=%{public}s",
			42, 3.14, "hello");
	}, @"Formatted os_log writes with multiple argument types must not crash");
}

- (void)test_pluginTemplateLog_acceptsInfoLevel {
	XCTAssertNoThrow(
		os_log_info(plugin_template_log, "PluginTemplateLoggerTests: info-level write"),
		@"Info-level writes must not crash"
	);
}

- (void)test_pluginTemplateLog_acceptsErrorLevel {
	XCTAssertNoThrow(
		os_log_error(plugin_template_log, "PluginTemplateLoggerTests: error-level write"),
		@"Error-level writes must not crash"
	);
}

@end
