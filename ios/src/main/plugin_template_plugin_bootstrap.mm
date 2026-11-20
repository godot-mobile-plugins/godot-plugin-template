//
// © 2026-present https://github.com/<<GitHubUsername>>
//

#import <Foundation/Foundation.h>

#import "plugin_template_logger.h"
#import "plugin_template_plugin.h"
#import "plugin_template_plugin_bootstrap.h"

#import "core/config/engine.h"

// Hooking the iOS application delegate (Godot 4.8+):
// Plugins that need UIApplicationDelegate / UIWindowSceneDelegate callbacks register a
// service object with the engine's delegate. On Godot 4.8 the header and class were
// renamed; include "drivers/apple_embedded/godot_app_delegate_apple_embedded.h" and
// call [GDTAppDelegate addService:myService] (4.7 used "godot_app_delegate.h" and
// GDTApplicationDelegate). Register the service from the init function below.

PluginTemplatePlugin *plugin_template_plugin;

void plugin_template_plugin_init() {
	os_log_debug(plugin_template_log, "PluginTemplatePlugin: Initializing plugin at timestamp: %f",
			[[NSDate date] timeIntervalSince1970]);

	plugin_template_plugin = memnew(PluginTemplatePlugin);
	Engine::get_singleton()->add_singleton(Engine::Singleton("PluginTemplatePlugin", plugin_template_plugin));
	os_log_debug(plugin_template_log, "PluginTemplatePlugin: Singleton registered");
}

void plugin_template_plugin_deinit() {
	os_log_debug(plugin_template_log, "PluginTemplatePlugin: Deinitializing plugin");
	plugin_template_log = NULL; // Prevent accidental reuse

	if (plugin_template_plugin) {
		memdelete(plugin_template_plugin);
		plugin_template_plugin = nullptr;
	}
}
