#
# Copyright (c) 2026-present https://github.com/<<GitHubUsername>>
#

# Checks how the PluginTemplate node behaves when the mobile (Android/iOS) part of the plugin is
# missing, as on a desktop build: it must report the missing singleton and fail safely.
extends IntegrationTest

const SINGLETON_NAME: String = "PluginTemplatePlugin"

var _node: PluginTemplate


func before_each() -> void:
	_node = PluginTemplate.new()


func after_each() -> void:
	if is_instance_valid(_node):
		_node.free()
	_node = null


func test_native_singleton_is_not_available() -> void:
	assert_false(Engine.has_singleton(SINGLETON_NAME), "no native plugin is expected in headless desktop Godot")


func test_ready_reports_missing_singleton() -> void:
	tree.root.add_child(_node)
	await wait_frames()
	expect_error("%s singleton not found on this platform!" % SINGLETON_NAME)


func test_get_plugin_template_returns_empty_array() -> void:
	tree.root.add_child(_node)
	await wait_frames()
	expect_error("singleton not found")
	assert_eq(_node.get_plugin_template(), [])
	expect_error("%s plugin not initialized" % SINGLETON_NAME)


func test_application_resumed_retries_singleton_lookup() -> void:
	tree.root.add_child(_node)
	await wait_frames()
	expect_error("singleton not found")
	_node.notification(Node.NOTIFICATION_APPLICATION_RESUMED)
	expect_error("%s singleton not found on this platform!" % SINGLETON_NAME)


func test_template_ready_signal_is_declared() -> void:
	assert_true(_node.has_signal("template_ready"), "template_ready signal must exist")
	var __args: Array = []
	for __signal in _node.get_signal_list():
		if __signal.name == "template_ready":
			__args = __signal.args
	assert_eq(__args.size(), 1, "template_ready must have one argument")
	if __args.size() == 1:
		assert_eq(__args[0].type, TYPE_DICTIONARY, "template_ready argument must be a Dictionary")
