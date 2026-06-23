//
// © 2026-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

// base-conventions - precompiled script plugin
// (build-logic/src/main/kotlin/base-conventions.gradle.kts)
//
// This is the single entry point for all shared build configuration.
// Applying id("base-conventions") in any project build script:
//
//   1. Makes PluginConfig, GodotConfig, IosConfig, BuildConfig, and the
//      Project extension functions (loadPluginConfig, loadIosConfig, …)
//      available on the build script's compilation classpath, so submodule
//      build scripts can call loadPluginConfig() / loadIosConfig() / … directly.
//
//   2. Sets shared directory-layout extras on project.extra used across all
//      modules.  Config data values (PluginConfig, GodotConfig, IosConfig) are
//      NOT bridged to project.extra; every submodule accesses them via the
//      typed loader functions instead.
//
//   3. Applies the per-module user-defined extra properties and extra Gradle
//      scripts from BuildConfig, keyed by project.path.
//
// The four apply(from = …) scripts that previously held this logic
// (addon.gradle.kts, android.gradle.kts, common.gradle.kts, ios.gradle.kts)
// have been deleted; their logic lives here.

// -- Load BuildConfig ----------------------------------------------------------
//
// Only BuildConfig is loaded here; PluginConfig, GodotConfig, and IosConfig are
// loaded directly by each submodule via loadPluginConfig() / loadGodotConfig() /
// loadIosConfig() from ProjectExtensions.kt.

val buildConfig = BuildConfig.load(rootProject.rootDir)

// -- Shared directory layout (replaces common.gradle.kts) ---------------------
//
// rootProject.rootDir == gradle/
// rootProject.rootDir.parentFile == repo root

val repoRoot = rootProject.rootDir.parentFile

project.extra["pluginDir"]         = "${rootProject.rootDir}/build/plugin"
project.extra["repositoryRootDir"] = "$repoRoot"
project.extra["archiveDir"]        = "$repoRoot/release"
project.extra["demoDir"]           = "$repoRoot/demo"

// -- Addon source / output layout ----------------------------------------------
//
// projectDir resolves to the correct module directory for each sub-project
// (e.g. addon/, android/, ios/) so templateDir / outputDir are always right.
// Setting these for every project is harmless - android and ios tasks never
// read templateDir or outputDir.

project.extra["templateDir"]       = "$projectDir/src/main"
project.extra["sharedTemplateDir"] = "$projectDir/src/shared"
project.extra["outputDir"]         = "$projectDir/build/output"

// -- Per-module user-defined extras -------------------------------------------
//
// Conditional on project.path so each module only receives its own extras.
// The root project receives rootExtraProperties / rootExtraGradle.

when (project.path) {
    ":" ->
        applyBuildConfigExtras(buildConfig.rootExtraProperties, buildConfig.rootExtraGradle)

    ":addon" ->
        applyBuildConfigExtras(buildConfig.addonExtraProperties, buildConfig.addonExtraGradle)

    ":android" ->
        applyBuildConfigExtras(buildConfig.androidExtraProperties, buildConfig.androidExtraGradle)

    ":ios" ->
        applyBuildConfigExtras(buildConfig.iosExtraProperties, buildConfig.iosExtraGradle)
}
