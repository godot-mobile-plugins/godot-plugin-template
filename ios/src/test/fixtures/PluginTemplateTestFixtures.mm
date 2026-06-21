//
// © 2026-present https://github.com/<<GitHubUsername>>
//

#import "PluginTemplateTestFixtures.h"

@implementation PluginTemplateTestFixtures

// -- Generic helpers ---------------------------------------------------------

+ (Array)makeGodotStringArray:(NSArray<NSString *> *)strings {
	Array array;
	for (NSString *s in strings) {
		array.push_back(Variant(String([s UTF8String])));
	}
	return array;
}

@end
