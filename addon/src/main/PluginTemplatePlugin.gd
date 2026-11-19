#
# Copyright (c) 2026-present https://github.com/<<GitHubUsername>>
#

@tool
extends EditorPlugin

const PLUGIN_NAME: String = "@pluginName@"
const ANDROID_DEPENDENCIES: Array = [ @androidDependencies@ ]
const IOS_PLATFORM_VERSION: String = "@iosPlatformVersion@"
const IOS_FRAMEWORKS: Array = [ @iosFrameworks@ ]
const IOS_EMBEDDED_FRAMEWORKS: Array = [ @iosEmbeddedFrameworks@ ]
const IOS_LINKER_FLAGS: Array = [ @iosLinkerFlags@ ]
const IOS_BUNDLE_FILES: Array = [ @iosBundleFiles@ ]
const SPM_DEPENDENCIES: Array = [ @spmDependencies@ ]

var android_export_plugin: AndroidExportPlugin
var ios_export_plugin: IosExportPlugin


func _enter_tree() -> void:
	android_export_plugin = AndroidExportPlugin.new()
	add_export_plugin(android_export_plugin)
	ios_export_plugin = IosExportPlugin.new()
	add_export_plugin(ios_export_plugin)


func _exit_tree() -> void:
	remove_export_plugin(android_export_plugin)
	android_export_plugin = null
	remove_export_plugin(ios_export_plugin)
	ios_export_plugin = null


class AndroidExportPlugin extends EditorExportPlugin:

	func _supports_platform(platform: EditorExportPlatform) -> bool:
		return platform is EditorExportPlatformAndroid


	func _get_android_libraries(_platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray(["%s/bin/debug/%s-debug.aar" % [PLUGIN_NAME, PLUGIN_NAME]])

		return PackedStringArray(["%s/bin/release/%s-release.aar" % [PLUGIN_NAME, PLUGIN_NAME]])


	func _get_name() -> String:
		return PLUGIN_NAME


	func _get_android_dependencies(_platform: EditorExportPlatform, _debug: bool) -> PackedStringArray:
		return PackedStringArray(ANDROID_DEPENDENCIES)


class IosExportPlugin extends EditorExportPlugin:
	var _spm_dependencies: Array[SpmDependency] = []


	func _supports_platform(platform: EditorExportPlatform) -> bool:
		return platform is EditorExportPlatformIOS


	func _get_name() -> String:
		return PLUGIN_NAME


	func _export_begin(_features: PackedStringArray, _is_debug: bool, _path: String,
			_flags: int) -> void:
		if _supports_platform(get_export_platform()):
			for __framework in IOS_FRAMEWORKS:
				add_apple_embedded_platform_framework(__framework)

			for __framework in IOS_EMBEDDED_FRAMEWORKS:
				add_apple_embedded_platform_embedded_framework(__framework)

			for __flag in IOS_LINKER_FLAGS:
				add_apple_embedded_platform_linker_flags(__flag)

			for __bundle_file in IOS_BUNDLE_FILES:
				add_apple_embedded_platform_bundle_file(__bundle_file)

			_spm_dependencies.clear()
			for __spm_dep in SPM_DEPENDENCIES:
				_spm_dependencies.append(SpmDependency.new(__spm_dep))
			_spm_dependencies.append_array(_get_extra_dependencies())

			if _spm_dependencies.is_empty():
				GmpLogger.log_info("No SPM dependencies to add. Skipping.")
			else:
				GmpLogger.log_info("Adding %d SPM dependencies." % _spm_dependencies.size())
				for __spm_dep: SpmDependency in _spm_dependencies:
					add_apple_embedded_platform_spm_package(__spm_dep.get_url(),
							__spm_dep.get_version(),
							PackedStringArray(__spm_dep.get_products()))
					GmpLogger.log_info("Added SPM dependency %s" % __spm_dep.format_to_string())


	func _end_generate_apple_embedded_project(path: String, will_build_archive: bool) -> void:
		GmpLogger.log_info("Apple export project generated at: %s. Will build archive: %s"
				% [path, str(will_build_archive)])


	func _get_extra_dependencies() -> Array[SpmDependency]:
		var __extra_dependencies:= [] as Array[SpmDependency]

		# Add any extra SPM dependencies here.

		return __extra_dependencies

