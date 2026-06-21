//
// © 2026-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

// Included build: provides shared convention plugins and build-logic classes
// to all subprojects. Declared in the root settings.gradle.kts via:
//   includeBuild("build-logic")
//

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    // Reuse the root project's version catalog so plugin versions stay in sync
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
rootProject.buildFileName = "logic.gradle.kts"
