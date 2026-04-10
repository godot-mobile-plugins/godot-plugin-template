//
// © 2026-present https://github.com/<<GitHubUsername>>
//

#pragma once

#import <Foundation/Foundation.h>

#include "core/variant/variant.h"

/// Factory methods that construct Godot Dictionary / Array values so every
/// unit-test file shares one canonical source of test data instead of
/// duplicating boilerplate.
@interface PluginTemplateTestFixtures : NSObject

// -- Generic helpers ---------------------------------------------------------

/// Builds a Godot String array from an NSArray of NSString objects.
+ (Array)makeGodotStringArray:(NSArray<NSString *> *)strings;

@end
