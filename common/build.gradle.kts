//
// © 2026-present https://github.com/cengiz-pz
//

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.undercouch.download) apply false
    alias(libs.plugins.openrewrite) apply false
    alias(libs.plugins.node) apply false
//    alias(libs.plugins.spotless) apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

// Load configuration from project root
apply(from = "$rootDir/config.gradle.kts")

tasks {
    val pluginDir: String by project.extra
    val repositoryRootDir: String by project.extra
    val archiveDir: String by project.extra
    val demoDir: String by project.extra

    register<Copy>("buildAndroidDebug") {
        description = "Copies the generated GDScript and debug AAR binary to the plugin directory"

        dependsOn(
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("copyAssets"),
            project(":android").tasks.named("assembleDebug")
        )

        into("${project.extra["pluginDir"]}/android")

        from("$rootDir/../addon/build/output") {
            include("addons/${project.extra["pluginName"]}/**")
        }

        from("$rootDir/../android/build/outputs/aar") {
            include("${project.extra["pluginName"]}-debug.aar")
            into("addons/${project.extra["pluginName"]}/bin/debug")
        }

        doLast {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = LocalDateTime.now().format(formatter)
            println("Android debug build completed at: $current")
        }

        outputs.dir("${project.extra["pluginDir"]}/android")
    }

    register<Copy>("buildAndroidRelease") {
        description = "Copies the generated GDScript and release AAR binary to the plugin directory"

        dependsOn(
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("copyAssets"),
            project(":android").tasks.named("assembleRelease")
        )

        into("${project.extra["pluginDir"]}/android")

        from("$rootDir/../addon/build/output") {
            include("addons/${project.extra["pluginName"]}/**")
        }

        from("$rootDir/../android/build/outputs/aar") {
            include("${project.extra["pluginName"]}-release.aar")
            into("addons/${project.extra["pluginName"]}/bin/release")
        }

        doLast {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = LocalDateTime.now().format(formatter)
            println("Android release build completed at: $current")
        }

        outputs.dir("${project.extra["pluginDir"]}/android")
    }

    register("buildAndroid") {
        description = "Builds both debug and release"

        dependsOn(
            "buildAndroidDebug",
            "buildAndroidRelease"
        )
    }

    register("build") {
        description = "Builds both Android and iOS"

        dependsOn(
            "buildAndroid",
            project(":ios").tasks.named("buildiOS")
        )
    }

    register<Copy>("installToDemoAndroid") {
        description = "Copies the assembled Android plugin to demo application's addons directory"

        dependsOn(
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("copyAssets"),
            "buildAndroidDebug"
        )

        destinationDir = file(demoDir)

        duplicatesStrategy = DuplicatesStrategy.WARN

        into(".") {
            from("${project.extra["pluginDir"]}/android")
        }

        outputs.dir(destinationDir)
    }

    register<Copy>("installToDemo") {
        description = "Installs both the Android and iOS plugins to demo app"

        dependsOn(
            "installToDemoAndroid",
            project(":ios").tasks.named("installToDemoiOS")
        )
    }

    register<Delete>("uninstallAndroid") {
        description = "Keep demo app's plugin directory and delete everything inside except for .uid and .import files"
        delete(
            fileTree("$demoDir/addons/${project.extra["pluginName"]}").apply {
                include("**/*")
                exclude("**/*.uid")
                exclude("**/*.import")
            },
        )
    }

    register("uninstall") {
        description = "Cleans all build outputs"

        dependsOn(
            "uninstallAndroid",
            project(":ios").tasks.named("uninstalliOS")
        )
    }

    register<Delete>("clean") {
        description = "Cleans all build outputs"

        dependsOn(
            ":android:clean",
            ":addon:cleanOutput",
            project(":ios").tasks.named("cleaniOSBuild")
        )
    }

    register<Zip>("createAndroidArchive") {
        dependsOn("buildAndroidDebug", "buildAndroidRelease")

        val archiveName = project.extra["pluginArchiveAndroid"] as String
        val sourceDir = "${project.extra["pluginDir"] as String}/android"

        archiveFileName.set(archiveName)
        destinationDirectory.set(layout.projectDirectory.dir(archiveDir))

        into("res") {
            from(layout.projectDirectory.dir(sourceDir)) {
                includeEmptyDirs = false
            }
        }

        doLast {
            println("Android zip archive created at: ${archiveFile.get().asFile.path}")
        }
    }

    register<Zip>("createMultiArchive") {
        dependsOn(
            "buildAndroidDebug",
            "buildAndroidRelease",
            project(":ios").tasks.named("buildiOS"),
            project(":ios").tasks.named("copyiOSBuildArtifacts")
        )

        val archiveName = project.extra["pluginArchiveMulti"] as String
        val androidDir = "${project.extra["pluginDir"] as String}/android"
        val iosDir = "${project.extra["pluginDir"] as String}/ios"

        archiveFileName.set(archiveName)
        destinationDirectory.set(layout.projectDirectory.dir(archiveDir))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        into("res") {
            from(layout.projectDirectory.dir(androidDir)) {
                includeEmptyDirs = false
            }

            from(layout.projectDirectory.dir(iosDir)) {
                includeEmptyDirs = false
            }
        }

        doLast {
            println("Multi zip archive created at: ${archiveFile.get().asFile.path}")
        }
    }

    register("createArchives") {
        description = "Creates both the Android and iOS zip archives"
        dependsOn(
            "createAndroidArchive",
            project(":ios").tasks.named("createiOSArchive"),
            "createMultiArchive"
        )
    }

    register<Exec>("checkKtsFormat") {
        description = "Checks ktlint compliance of Gradle Kotlin DSL files (dry-run, no changes written)"
        group = "formatting"

        workingDir = file(repositoryRootDir)

        doFirst {
            val sourceFiles =
                listOf("addon", "android", "common", "ios")
                    .flatMap { dir ->
                        fileTree("$repositoryRootDir/$dir") {
                            include("*.gradle.kts")
                        }.files
                    }.map { it.relativeTo(file(repositoryRootDir)).path }
                    .sorted()

            if (sourceFiles.isEmpty()) {
                throw GradleException("checkKtsFormat: no *.gradle.kts files found under addon/, android/, or common/, "
                    + "or ios/")
            }

            commandLine(
                buildList {
                    add("ktlint")
                    addAll(sourceFiles)
                },
            )
        }
    }

    register<Exec>("formatKtsSource") {
        description = "Formats Gradle Kotlin DSL files in-place using ktlint --format"
        group = "formatting"

        workingDir = file(repositoryRootDir)

        doFirst {
            val sourceFiles =
                listOf("addon", "android", "common", "ios")
                    .flatMap { dir ->
                        fileTree("$repositoryRootDir/$dir") {
                            include("*.gradle.kts")
                        }.files
                    }.map { it.relativeTo(file(repositoryRootDir)).path }
                    .sorted()

            if (sourceFiles.isEmpty()) {
                throw GradleException("formatKtsSource: no *.gradle.kts files found under addon/, android/, common/, or"
                    + " ios/")
            }

            commandLine(
                buildList {
                    add("ktlint")
                    add("--format")
                    addAll(sourceFiles)
                },
            )
        }
    }

    register("checkFormat") {
        description = "Validates format in all source code"

        // Removed "spotlessCheck"
        dependsOn(
            ":android:rewriteDryRun",
            ":android:checkXmlFormat",
            project(":ios").tasks.named("checkIosFormat"),
            "checkKtsFormat",
            ":addon:checkGdscriptFormat",
        )
    }

    register("applyFormat") {
        description = "Formats all source code"

        // Removed "spotlessApply"
        dependsOn(
            ":android:rewriteRun",
            ":android:formatXml",
            project(":ios").tasks.named("formatIosSource"),
            "formatKtsSource",
            ":addon:formatGdscriptSource",
        )
    }
}
