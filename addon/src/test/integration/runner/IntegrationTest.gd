#
# Copyright (c) 2026-present https://github.com/<<GitHubUsername>>
#

# Base class for integration test suites. Each file in res://tests/ extends this class; every
# method whose name starts with "test_" is run as one test case (it may use await).
#
# Any engine error logged during a test fails that test unless the test consumes it with
# expect_error(). This is how "loads without errors" is checked.
class_name IntegrationTest extends RefCounted

## SceneTree the tests run in; use it to add nodes and to await frames.
var tree: SceneTree

var _failures: PackedStringArray = []
var _collector: ErrorCollector
var _errors: PackedStringArray = []


## Called before each test. Override in a suite when needed.
func before_each() -> void:
	pass


## Called after each test (also when it failed). Override in a suite when needed.
func after_each() -> void:
	pass


## Waits for [param a_frames] process frames, e.g. so that added nodes receive _ready().
func wait_frames(a_frames: int = 1) -> void:
	for __i in a_frames:
		await tree.process_frame


func assert_true(a_condition: bool, a_message: String = "") -> void:
	if not a_condition:
		_fail("Expected true. %s" % a_message)


func assert_false(a_condition: bool, a_message: String = "") -> void:
	if a_condition:
		_fail("Expected false. %s" % a_message)


func assert_eq(a_actual: Variant, a_expected: Variant, a_message: String = "") -> void:
	if typeof(a_actual) != typeof(a_expected) or a_actual != a_expected:
		_fail("Expected <%s> but was <%s>. %s" % [var_to_str(a_expected), var_to_str(a_actual), a_message])


func assert_not_null(a_value: Variant, a_message: String = "") -> void:
	if a_value == null:
		_fail("Expected a non-null value. %s" % a_message)


## Asserts that exactly [param a_count] errors containing [param a_substring] were logged since
## the test started (or since the previous expect_error call), and marks them as expected.
func expect_error(a_substring: String, a_count: int = 1) -> void:
	_errors.append_array(_collector.take_errors())
	var __remaining: PackedStringArray = []
	var __matched: int = 0
	for __error in _errors:
		if __error.contains(a_substring):
			__matched += 1
		else:
			__remaining.append(__error)
	_errors = __remaining
	if __matched != a_count:
		_fail("Expected %d error(s) containing '%s' but %d were logged." % [a_count, a_substring, __matched])


func _fail(a_message: String) -> void:
	_failures.append(a_message)


# -- Used by IntegrationTestRunner ----------------------------------------------


func _begin_test(a_tree: SceneTree, a_collector: ErrorCollector) -> void:
	tree = a_tree
	_collector = a_collector
	_failures = []
	a_collector.take_errors()  # Discard anything logged outside a test (e.g. while loading suites)
	_errors = []


## Returns the failure messages of the finished test, including any unexpected errors.
func _end_test() -> PackedStringArray:
	_errors.append_array(_collector.take_errors())
	for __error in _errors:
		_failures.append("Unexpected error: %s" % __error)
	_errors = []
	return _failures
