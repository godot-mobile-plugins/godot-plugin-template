#
# Copyright (c) 2026-present https://github.com/<<GitHubUsername>>
#

# Minimal headless integration test runner.
#
# Usage (normally invoked by `./gradlew :addon:runIntegrationTests`):
#   godot --headless --path <project> --script res://runner/IntegrationTestRunner.gd -- --junit-dir=<dir>
#
# Runs every res://tests/*.gd suite (scripts extending IntegrationTest), prints one line per test
# and writes one JUnit XML file per suite into --junit-dir. Exits with code 1 when any test fails
# or no tests were found.
extends SceneTree

const TESTS_DIR: String = "res://tests"
const TEST_PREFIX: String = "test_"
const LOAD_TEST_NAME: String = "_load_suite"

var _collector: ErrorCollector = ErrorCollector.new()
var _junit_dir: String = ""


func _initialize() -> void:
	OS.add_logger(_collector)
	for __arg in OS.get_cmdline_user_args():
		if __arg.begins_with("--junit-dir="):
			_junit_dir = __arg.trim_prefix("--junit-dir=")
	_run.call_deferred()


func _run() -> void:
	var __total: int = 0
	var __failed: int = 0

	for __file in _find_suite_files():
		var __cases: Array[Dictionary] = await _run_suite(__file)
		for __case in __cases:
			__total += 1
			if not __case.failures.is_empty():
				__failed += 1
		_write_junit(__file.get_basename(), __cases)

	print("Integration tests: %d run, %d failed" % [__total, __failed])
	OS.remove_logger(_collector)
	if __total == 0:
		printerr("No integration tests found in %s" % TESTS_DIR)
	quit(1 if __failed > 0 or __total == 0 else 0)


func _find_suite_files() -> PackedStringArray:
	var __files: PackedStringArray = []
	for __file in DirAccess.get_files_at(TESTS_DIR):
		if __file.ends_with(".gd"):
			__files.append(__file)
	__files.sort()
	return __files


func _run_suite(a_file: String) -> Array[Dictionary]:
	var __suite_name: String = a_file.get_basename()
	var __cases: Array[Dictionary] = []

	# Loading the suite also compiles every script it references (e.g. the plugin's classes),
	# so errors logged here are reported as a failed test case instead of being lost.
	_collector.take_errors()
	var __script: Script = load(TESTS_DIR.path_join(a_file))
	var __suite: IntegrationTest = null
	if __script != null and __script.can_instantiate():
		__suite = __script.new() as IntegrationTest
	var __load_errors: PackedStringArray = _collector.take_errors()
	if __suite == null or not __load_errors.is_empty():
		var __failures: PackedStringArray = []
		if __suite == null:
			__failures.append("Could not load %s as an IntegrationTest suite." % a_file)
		for __error in __load_errors:
			__failures.append("Error while loading suite: %s" % __error)
		__cases.append(_report(__suite_name, LOAD_TEST_NAME, __failures, 0.0))
		if __suite == null:
			return __cases

	for __method in __script.get_script_method_list():
		var __name: String = __method.name
		if not __name.begins_with(TEST_PREFIX):
			continue
		var __start: int = Time.get_ticks_msec()
		__suite._begin_test(self, _collector)
		await __suite.before_each()
		await __suite.call(__name)
		await __suite.after_each()
		var __failures: PackedStringArray = __suite._end_test()
		var __seconds: float = (Time.get_ticks_msec() - __start) / 1000.0
		__cases.append(_report(__suite_name, __name, __failures, __seconds))

	return __cases


func _report(a_suite: String, a_test: String, a_failures: PackedStringArray, a_seconds: float) -> Dictionary:
	if a_failures.is_empty():
		print("PASS %s.%s" % [a_suite, a_test])
	else:
		print("FAIL %s.%s" % [a_suite, a_test])
		for __failure in a_failures:
			print("    %s" % __failure)
	return {"name": a_test, "failures": a_failures, "time": a_seconds}


func _write_junit(a_suite: String, a_cases: Array[Dictionary]) -> void:
	if _junit_dir.is_empty():
		return
	DirAccess.make_dir_recursive_absolute(_junit_dir)

	var __failed: int = 0
	var __time: float = 0.0
	var __body: String = ""
	for __case in a_cases:
		__time += __case.time
		__body += '  <testcase classname="%s" name="%s" time="%.3f"' % [_xml(a_suite), _xml(__case.name), __case.time]
		if __case.failures.is_empty():
			__body += "/>\n"
		else:
			__failed += 1
			var __text: String = "\n".join(__case.failures)
			__body += (
				'>\n    <failure message="%s">%s</failure>\n  </testcase>\n' % [_xml(__case.failures[0]), _xml(__text)]
			)

	var __xml: String = '<?xml version="1.0" encoding="UTF-8"?>\n'
	__xml += (
		'<testsuite name="%s" tests="%d" failures="%d" errors="0" skipped="0" time="%.3f">\n'
		% [_xml(a_suite), a_cases.size(), __failed, __time]
	)
	__xml += __body + "</testsuite>\n"

	var __path: String = _junit_dir.path_join("TEST-%s.xml" % a_suite)
	var __out: FileAccess = FileAccess.open(__path, FileAccess.WRITE)
	if __out == null:
		printerr("Could not write %s: %s" % [__path, error_string(FileAccess.get_open_error())])
		return
	__out.store_string(__xml)
	__out.close()


func _xml(a_text: String) -> String:
	return a_text.xml_escape(true)
