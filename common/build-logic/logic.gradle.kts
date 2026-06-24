//
// © 2026-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

// Compiles all convention plugins (precompiled script plugins) in
// src/main/java/*.gradle.kts and makes them - together with their
// runtime dependencies - available to every subproject that applies them.
//
// Versions are kept in sync with gradle/libs.versions.toml via the
// version catalog re-export in settings.gradle.kts:
//   kotlin-android-plugin  ->  kotlin("plugin.serialization")
//   kotlinx-serialization  ->  kotlinx-serialization-json runtime
//

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.serialization)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("baseConventions") {
            id = "base-conventions"
            implementationClass = "BaseConventionsPlugin"
            description = "Godot Mobile Plugins base conventions plugin"
        }
    }
}

val buildLogicDependencies =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .run {
            libraryAliases
                .filter { it.startsWith("build.logic.") }
                .map { findLibrary(it).get().get() }
        }

dependencies {
    implementation(gradleKotlinDsl())

    buildLogicDependencies.forEach {
        implementation(it)
    }
}

// All build-logic source files live in src/main/java (Kotlin files, .gradle.kts
// precompiled script plugins, and data classes).  Declare only that directory so
// that Gradle never fingerprints the non-existent src/main/kotlin as a task
// input – a phantom input that can cause spurious stale-output detections.
//
// kotlin-dsl adds its own generated source roots (kotlin-dsl-plugins,
// kotlin-dsl-accessors, kotlin-dsl-external-plugin-spec-builders) separately via
// task configuration, so the srcDirs() call here does not affect them.
kotlin.sourceSets.getByName("main") {
    kotlin.srcDirs("src/main/java")
}
