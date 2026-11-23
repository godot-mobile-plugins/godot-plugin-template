#
# Copyright (c) 2026-present https://github.com/<<GitHubUsername>>
#

# Checks that the generated addon files load in headless Godot without errors.
extends IntegrationTest

const ADDON_DIR: String = "res://addons/PluginTemplatePlugin"
const SHARED_DIR: String = "res://addons/GMPShared"
const PLUGIN_NAME: String = "PluginTemplatePlugin"
const TOKEN_PATTERN: String = "@[A-Za-z]+@"


func test_plugin_cfg_points_to_editor_plugin_script() -> void:
	var __config: ConfigFile = ConfigFile.new()
	assert_eq(__config.load(ADDON_DIR.path_join("plugin.cfg")), OK, "plugin.cfg must be readable")
	assert_eq(__config.get_value("plugin", "script", ""), "%s.gd" % PLUGIN_NAME)
	assert_true(
		FileAccess.file_exists(ADDON_DIR.path_join(__config.get_value("plugin", "script", ""))),
		"script referenced by plugin.cfg must exist"
	)


func test_generated_files_have_no_unreplaced_tokens() -> void:
	var __regex: RegEx = RegEx.create_from_string(TOKEN_PATTERN)
	for __dir in [ADDON_DIR, SHARED_DIR]:
		for __file in _list_files(__dir):
			if __file.get_extension() in ["gd", "cfg", "json"]:
				var __match: RegExMatch = __regex.search(FileAccess.get_file_as_string(__file))
				assert_true(__match == null, "%s contains token %s" % [__file, __match.get_string() if __match else ""])


func test_plugin_classes_are_registered() -> void:
	var __classes: Array[String] = []
	for __entry in ProjectSettings.get_global_class_list():
		__classes.append(__entry["class"])
	assert_true("PluginTemplate" in __classes, "class_name PluginTemplate must be registered")
	assert_true("GmpLogger" in __classes, "class_name GmpLogger must be registered")


func test_node_script_loads_without_errors() -> void:
	var __script: GDScript = load(ADDON_DIR.path_join("PluginTemplate.gd"))
	assert_not_null(__script, "PluginTemplate.gd must load")
	assert_true(__script.can_instantiate(), "PluginTemplate.gd must compile")
	assert_eq(__script.get_script_constant_map().get("PLUGIN_SINGLETON_NAME"), PLUGIN_NAME)


func test_editor_plugin_script_loads_without_errors() -> void:
	var __script: GDScript = load(ADDON_DIR.path_join("%s.gd" % PLUGIN_NAME))
	assert_not_null(__script, "%s.gd must load" % PLUGIN_NAME)
	var __constants: Dictionary = __script.get_script_constant_map()
	assert_eq(__constants.get("PLUGIN_NAME"), PLUGIN_NAME)
	assert_true(__constants.get("ANDROID_DEPENDENCIES") is Array, "ANDROID_DEPENDENCIES must be an Array")
	assert_false(str(__constants.get("IOS_PLATFORM_VERSION", "")).is_empty(), "IOS_PLATFORM_VERSION must be set")
	assert_true(__constants.has("AndroidExportPlugin"), "AndroidExportPlugin inner class must exist")
	assert_true(__constants.has("IosExportPlugin"), "IosExportPlugin inner class must exist")


func test_shared_scripts_load_without_errors() -> void:
	for __file in _list_files(SHARED_DIR):
		if __file.get_extension() == "gd":
			assert_not_null(load(__file), "%s must load" % __file)


func _list_files(a_dir: String) -> PackedStringArray:
	var __files: PackedStringArray = []
	for __file in DirAccess.get_files_at(a_dir):
		__files.append(a_dir.path_join(__file))
	for __sub_dir in DirAccess.get_directories_at(a_dir):
		__files.append_array(_list_files(a_dir.path_join(__sub_dir)))
	return __files
