//
// © 2026-present https://github.com/cengiz-pz
//

#import "AdmobTestFixtures.h"

@implementation AdmobTestFixtures

// -- Generic helpers ---------------------------------------------------------

+ (Array)makeGodotStringArray:(NSArray<NSString *> *)strings {
	Array array;
	for (NSString *s in strings) {
		array.push_back(Variant(String([s UTF8String])));
	}
	return array;
}

@end
