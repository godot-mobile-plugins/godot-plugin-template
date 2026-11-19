#
# Copyright (c) 2026-present https://github.com/<<GitHubUsername>>
#
# android/config/consumer-rules.pro - R8/ProGuard rules shipped inside the plugin AAR.
#
# Godot 4.8 adds the "gradle_build/minification" export option, which runs R8 on release
# Gradle builds. The engine's own rules already keep every GodotPlugin subclass and every
# member annotated with @UsedByGodot, so the plugin's methods and signals survive shrinking
# without any rules here. These consumer rules cover what the engine does not know about:
# anything this plugin reaches through reflection, JNI, or serialization beyond @UsedByGodot.
#

# Keep the plugin's own public API and its signal registration intact under obfuscation.
-keep class * extends org.godotengine.godot.plugin.GodotPlugin { *; }

# Keep @UsedByGodot members in any class, not only in GodotPlugin subclasses, so helper
# classes exposed to GDScript keep their names and signatures.
-keepattributes *Annotation*
-keepclassmembers class * {
	@org.godotengine.godot.plugin.UsedByGodot *;
}

# Keep native method names in this plugin (JNI looks them up by name).
-keepclasseswithmembernames class * {
	native <methods>;
}

# TODO: Add keep rules for third-party libraries this plugin uses via reflection
# (e.g. Gson/Moshi models, service loaders, Kotlin serialization), for example:
# -keep class com.example.sdk.model.** { *; }
