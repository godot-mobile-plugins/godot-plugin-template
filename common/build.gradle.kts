//
// © 2026-present https://github.com/cengiz-pz
//

plugins {
    id("base-conventions")
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.undercouch.download) apply false
    alias(libs.plugins.openrewrite) apply false
    alias(libs.plugins.node) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

apply(from = "$rootDir/config/common.gradle.kts")

/** Returns all *.gradle.kts files under addon/, android/, common/, and ios/. */
fun ktsSourceFiles(): List<String> {
    val repositoryRootDir: String by project.extra
    return listOf("addon", "android", "common", "ios")
        .flatMap { dir ->
            fileTree("$repositoryRootDir/$dir") { include("*.gradle.kts") }.files
        }.map { it.relativeTo(file(repositoryRootDir)).path }
        .sorted()
}

tasks {
    val pluginDir: String by project.extra
    val repositoryRootDir: String by project.extra
    val archiveDir: String by project.extra

    register("build") {
        description = "Builds both Android and iOS"
        dependsOn(
            project(":android").tasks.named("buildAndroid"),
            project(":ios").tasks.named("buildiOS"),
        )
    }

    register("installToDemo") {
        description = "Installs both the Android and iOS plugins to demo app"
        dependsOn(
            project(":android").tasks.named("installToDemoAndroid"),
            project(":ios").tasks.named("installToDemoiOS"),
        )
    }

    register("uninstall") {
        description = "Uninstalls all plugins from demo app"
        dependsOn(
            project(":android").tasks.named("uninstallAndroid"),
            project(":ios").tasks.named("uninstalliOS"),
        )
    }

    register("clean") {
        description = "Cleans all build outputs"
        dependsOn(
            project(":addon").tasks.named("cleanOutput"),
            project(":android").tasks.named("clean"),
            project(":ios").tasks.named("cleaniOSBuild"),
        )
    }

    register<Zip>("createMultiArchive") {
        dependsOn(
            project(":android").tasks.named("buildAndroidDebug"),
            project(":android").tasks.named("buildAndroidRelease"),
            project(":ios").tasks.named("buildiOS"),
            project(":ios").tasks.named("copyiOSBuildArtifacts"),
        )

        archiveFileName.set(project.extra["pluginArchiveMulti"] as String)
        destinationDirectory.set(layout.projectDirectory.dir(archiveDir))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        into("res") {
            from(layout.projectDirectory.dir("$pluginDir/android")) { includeEmptyDirs = false }
            from(layout.projectDirectory.dir("$pluginDir/ios")) { includeEmptyDirs = false }
        }

        doLast { println("Multi zip archive created at: ${archiveFile.get().asFile.path}") }
    }

    register("createArchives") {
        description = "Creates both the Android and iOS zip archives"
        dependsOn(
            project(":android").tasks.named("createAndroidArchive"),
            project(":ios").tasks.named("createiOSArchive"),
            "createMultiArchive",
        )
    }

    register<Exec>("checkEditorConfig") {
        description = "Checks editorconfig compliance of all source files"
        group = "formatting"

        workingDir = file(repositoryRootDir)

        val namePatterns =
            listOf(
                "*.gradle.kts",
                "*.properties",
                "*.json",
                "*.gd",
                "*.java",
                "*.kt",
                "*.h",
                "*.m",
                "*.mm",
                "*.swift",
                "*.sh",
                "*.rb",
            ).joinToString(" -o ") { "-name \"$it\"" }

        val excludePatterns =
            listOf("node_modules", ".git", "build", ".gradle", ".idea")
                .joinToString(" ") { "-not -path \"*/$it/*\"" }

        commandLine(
            "sh",
            "-c",
            """
            files=$(find . \( $namePatterns \) $excludePatterns \
                -not -path "./demo/addons/*" \
                -not -name "package.json" \
                -not -name "package-lock.json")
            if [ -z "${'$'}files" ]; then
                echo "checkEditorConfig: no source files found" >&2
                exit 1
            fi
            echo "${'$'}files" | tr '\n' '\0' | xargs -0 editorconfig-checker
            """.trimIndent(),
        )
    }

    register<Exec>("checkKtsFormat") {
        description = "Checks ktlint compliance of Gradle Kotlin DSL files (dry-run)"
        group = "formatting"

        workingDir = file(repositoryRootDir)

        doFirst {
            val sourceFiles = ktsSourceFiles()
            if (sourceFiles.isEmpty()) {
                throw GradleException(
                    "checkKtsFormat: no *.gradle.kts files found under addon/, android/, common/, or ios/",
                )
            }
            commandLine(listOf("ktlint") + sourceFiles)
        }
    }

    register<Exec>("formatKtsSource") {
        description = "Formats Gradle Kotlin DSL files in-place using ktlint --format"
        group = "formatting"

        workingDir = file(repositoryRootDir)

        doFirst {
            val sourceFiles = ktsSourceFiles()
            if (sourceFiles.isEmpty()) {
                throw GradleException(
                    "formatKtsSource: no *.gradle.kts files found under addon/, android/, common/, or ios/",
                )
            }
            commandLine(listOf("ktlint", "--format") + sourceFiles)
        }
    }

    register("checkFormat") {
        description = "Validates format in all source code"
        dependsOn(
            project(":addon").tasks.named("checkGdscriptFormat"),
            project(":android").tasks.named("checkJavaFormat"),
            project(":android").tasks.named("checkXmlFormat"),
            project(":ios").tasks.named("checkObjCFormat"),
            project(":ios").tasks.named("checkSwiftFormat"),
            "checkKtsFormat",
            "checkEditorConfig",
        )
    }

    register("applyFormat") {
        description = "Formats all source code"
        dependsOn(
            project(":addon").tasks.named("formatGdscriptSource"),
            project(":android").tasks.named("rewriteRun"),
            project(":android").tasks.named("formatXml"),
            project(":ios").tasks.named("formatObjCSource"),
            project(":ios").tasks.named("formatSwiftSource"),
            "formatKtsSource",
        )
    }
}
