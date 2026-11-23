#
# Copyright (c) 2026-present https://github.com/<<GitHubUsername>>
#

# Captures every engine error and warning (push_error, push_warning, script errors) while the
# integration tests run, so that tests can assert on them and unexpected errors fail the test.
# Loggers may be called from any thread, hence the mutex.
class_name ErrorCollector extends Logger

var _mutex: Mutex = Mutex.new()
var _errors: PackedStringArray = []
var _warnings: PackedStringArray = []


func _log_error(
	_function: String,
	a_file: String,
	a_line: int,
	a_code: String,
	a_rationale: String,
	_editor_notify: bool,
	a_error_type: int,
	_script_backtraces: Array[ScriptBacktrace]
) -> void:
	var __message: String = a_code if a_rationale.is_empty() else "%s: %s" % [a_code, a_rationale]
	__message = "%s (%s:%d)" % [__message, a_file, a_line]
	_mutex.lock()
	if a_error_type == ERROR_TYPE_WARNING:
		_warnings.append(__message)
	else:
		_errors.append(__message)
	_mutex.unlock()


func _log_message(_message: String, _error: bool) -> void:
	pass


## Returns and clears all errors collected since the last call.
func take_errors() -> PackedStringArray:
	_mutex.lock()
	var __errors: PackedStringArray = _errors
	_errors = []
	_warnings = []
	_mutex.unlock()
	return __errors
