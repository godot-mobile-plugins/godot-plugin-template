//
// © 2026-present https://github.com/<<GitHubUsername>>
//

#ifndef plugin_template_plugin_h
#define plugin_template_plugin_h

#import <Foundation/Foundation.h>

#include "core/object/class_db.h"
#include "core/object/object.h"

@class PluginTemplate;

extern const String TEMPLATE_READY_SIGNAL;
// TODO: Declare all signals

// Notes for plugin authors targeting Godot 4.8+:
// - GDCLASS types are no longer copy-assignable (the macro deletes operator=).
//   Keep engine objects behind pointers, as this singleton does; never assign one
//   Object-derived instance to another.
// - The memdelete_notnull() macro was removed. memdelete() now ignores nullptr,
//   so guard with a plain null check or call memdelete() directly.
class PluginTemplatePlugin : public Object {
	GDCLASS(PluginTemplatePlugin, Object);

private:
	static PluginTemplatePlugin *instance; // Singleton instance

	static void _bind_methods();

public:
	Array get_plugin_template();
	// TODO: Declare all methods

	PluginTemplatePlugin();
	~PluginTemplatePlugin();
};

#endif /* plugin_template_plugin_h */
