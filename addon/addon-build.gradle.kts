//
// © 2024-present https://github.com/cengiz-pz
//

plugins {
    id("base-conventions")
}

apply(from = "$projectDir/config/addon.gradle.kts")

// Collect all catalog library aliases (used in the @androidDependencies@ template token)
val androidDependencies =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .run { libraryAliases.map { findLibrary(it).get().get() } }

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Splits a comma-separated string, trims each item, and wraps non-blank items in double-quotes. */
fun String.toQuotedList(): String =
    split(",").map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ") { "\"$it\"" }

/**
 * Registers a GDScript format task (check or in-place fix).
 *
 * Both [checkGdscriptFormat] and [formatGdscriptSource] share identical source-file
 * discovery, gdformatrc lifecycle, and working directory — only the gdformat flag differs.
 *
 * If a `src/shared/` sibling directory exists alongside the src/main directory, its GDScript
 * files are included in formatting as well, and the `.gdformatrc` config is temporarily
 * copied there so that gdformat can locate it when traversing up from shared file paths.
 */
fun TaskContainerScope.registerGdscriptFormatTask(
    name: String,
    description: String,
    check: Boolean,
) {
    val addonSrcDir = file(project.extra["templateDir"] as String)
    val sharedSrcDir = file(project.extra["sharedTemplateDir"] as String)
    val gdformatrcSource = file("$projectDir/../.github/config/.gdformatrc")
    val gdformatrcDest = addonSrcDir.resolve(".gdformatrc")
    val sharedGdformatrcDest = sharedSrcDir.resolve(".gdformatrc")
    val excludePatterns = listOf("**/*Plugin.gd")

    register<Exec>(name) {
        this.description = description
        if (check) {
            this.group = "verification"
        } else {
            this.group = "formatting"
        }

        workingDir = addonSrcDir

        doFirst {
            copy {
                from(gdformatrcSource)
                into(addonSrcDir)
            }

            // Copy .gdformatrc into the shared directory so gdformat can locate it when
            // resolving config for files outside the main source tree.
            if (sharedSrcDir.exists()) {
                copy {
                    from(gdformatrcSource)
                    into(sharedSrcDir)
                }
            }

            val sourceFiles =
                buildList {
                    addAll(
                        fileTree(addonSrcDir) {
                            include("**/*.gd")
                            excludePatterns.forEach { exclude(it) }
                        }.files,
                    )
                    if (sharedSrcDir.exists()) {
                        addAll(fileTree(sharedSrcDir) { include("**/*.gd") }.files)
                    }
                    addAll(
                        fileTree("${rootProject.projectDir}/../demo") {
                            include("**/*.gd")
                            exclude("addons/**")
                        }.files,
                    )
                }.map { it.relativeTo(addonSrcDir).path }
                    .sorted()

            if (sourceFiles.isEmpty()) {
                throw GradleException("$name: no source files found under ${addonSrcDir.absolutePath}")
            }

            commandLine(
                buildList {
                    add("gdformat")
                    if (check) add("--check")
                    addAll(sourceFiles)
                },
            )
        }

        doLast {
            if (gdformatrcDest.exists()) gdformatrcDest.delete()
            if (sharedGdformatrcDest.exists()) sharedGdformatrcDest.delete()
        }
    }
}

// ── Tasks ─────────────────────────────────────────────────────────────────────

tasks {
    val addonSrcDir = file(project.extra["templateDir"] as String)
    val sharedSrcDir = file(project.extra["sharedTemplateDir"] as String)

    register<Delete>("cleanOutput") {
        group = "clean"
        delete(
            fileTree(project.extra["outputDir"] as String) {
                include("**/*.gd", "**/*.cfg", "**/*.png", "**/*.gdip")
            },
        )
        // Also clean the ios/plugins output directory where .gdip files are written
        delete(
            fileTree("${project.extra["outputDir"]}/ios/plugins") {
                include("**/*")
            },
        )
    }

    register<Copy>("copyAssets") {
        description = "Copies plugin assets such as PNG images to the output directory"
        group = "generate"
        from(addonSrcDir)
        into("${project.extra["outputDir"]}/addons/${project.extra["pluginName"]}")
        include("**/*.png")
        inputs.files(fileTree(addonSrcDir) { include("**/*.png") })
    }

    register<Copy>("generateSharedGDScript") {
        description = "Copies shared GDScript templates to the GMPShared output directory and replaces tokens"
        group = "generate"
        onlyIf("shared source directory contains GDScript or config files") {
            sharedSrcDir.exists() &&
                fileTree(sharedSrcDir) { include("**/*.gd", "**/*.cfg") }.files.isNotEmpty()
        }

        from(sharedSrcDir)
        into("${project.extra["outputDir"]}/addons/GMPShared")
        include("**/*.gd", "**/*.cfg")

        eachFile { println("[DEBUG] Processing shared file $relativePath") }

        // Identical token map to generateGDScript — shared scripts may reference any token.
        val allTokens: Map<String, String> =
            buildMap {
                project.extra.properties.forEach { (k, v) -> put(k, v.toString()) }
                put("androidDependencies", androidDependencies.joinToString(", ") { "\"$it\"" })
                put("iosFrameworks", (project.extra["iosFrameworks"] as String).toQuotedList())
                put("iosEmbeddedFrameworks", (project.extra["iosEmbeddedFrameworks"] as String).toQuotedList())
                put("iosLinkerFlags", (project.extra["iosLinkerFlags"] as String).toQuotedList())
            }

        filter { line: String ->
            allTokens.entries.fold(line) { acc, (key, value) ->
                val token = "@$key@"
                if (acc.contains(token)) {
                    println("\t[DEBUG] Replacing token $token with: $value")
                    acc.replace(token, value)
                } else {
                    acc
                }
            }
        }

        // Inputs are declared only when the directory exists so that Gradle does not
        // warn about a missing input directory on projects that have no shared sources.
        if (sharedSrcDir.exists()) {
            inputs.dir(sharedSrcDir)
        }
        inputs.files(
            rootProject.file("config/plugin.properties"),
            rootProject.file("../ios/config/ios.properties"),
        )
        inputs.property("pluginName", project.extra["pluginName"])
        inputs.property("pluginNodeName", project.extra["pluginNodeName"])
        inputs.property("pluginVersion", project.extra["pluginVersion"])
        inputs.property("pluginPackage", project.extra["pluginPackageName"])
        inputs.property("androidDependencies", androidDependencies.joinToString())
        inputs.property("iosPlatformVersion", project.extra["iosPlatformVersion"])
        inputs.property("iosFrameworks", project.extra["iosFrameworks"])
        inputs.property("iosEmbeddedFrameworks", project.extra["iosEmbeddedFrameworks"])
        inputs.property("iosLinkerFlags", project.extra["iosLinkerFlags"])

        outputs.dir("${project.extra["outputDir"]}/addons/GMPShared")
    }

    register<Copy>("generateGDScript") {
        description = "Copies the GDScript templates and plugin config to the output directory and replaces tokens"
        group = "generate"
        dependsOn("generateSharedGDScript")
        finalizedBy("copyAssets")

        from(addonSrcDir)
        into("${project.extra["outputDir"]}/addons/${project.extra["pluginName"]}")
        include("**/*.gd", "**/*.cfg")

        eachFile { println("[DEBUG] Processing file: $relativePath") }

        // Build a single merged token map: project.extra values first, then explicit
        // overrides that apply comma-list formatting where needed.
        val allTokens: Map<String, String> =
            buildMap {
                project.extra.properties.forEach { (k, v) -> put(k, v.toString()) }
                put("androidDependencies", androidDependencies.joinToString(", ") { "\"$it\"" })
                put("iosFrameworks", (project.extra["iosFrameworks"] as String).toQuotedList())
                put("iosEmbeddedFrameworks", (project.extra["iosEmbeddedFrameworks"] as String).toQuotedList())
                put("iosLinkerFlags", (project.extra["iosLinkerFlags"] as String).toQuotedList())
            }

        filter { line: String ->
            allTokens.entries.fold(line) { acc, (key, value) ->
                val token = "@$key@"
                if (acc.contains(token)) {
                    println("\t[DEBUG] Replacing token $token with: $value")
                    acc.replace(token, value)
                } else {
                    acc
                }
            }
        }

        inputs.dir(addonSrcDir)
        inputs.files(
            rootProject.file("config/plugin.properties"),
            rootProject.file("../ios/config/ios.properties"),
        )
        inputs.property("pluginName", project.extra["pluginName"])
        inputs.property("pluginNodeName", project.extra["pluginNodeName"])
        inputs.property("pluginVersion", project.extra["pluginVersion"])
        inputs.property("pluginPackage", project.extra["pluginPackageName"])
        inputs.property("androidDependencies", androidDependencies.joinToString())
        inputs.property("iosPlatformVersion", project.extra["iosPlatformVersion"])
        inputs.property("iosFrameworks", project.extra["iosFrameworks"])
        inputs.property("iosEmbeddedFrameworks", project.extra["iosEmbeddedFrameworks"])
        inputs.property("iosLinkerFlags", project.extra["iosLinkerFlags"])

        outputs.dir("${project.extra["outputDir"]}/addons/${project.extra["pluginName"]}")
    }

    register<Copy>("generateiOSConfig") {
        description = "Copies the iOS plugin config to the output directory and replaces tokens"
        group = "generate"

        // Must run after generateGDScript so addon files are already in place
        mustRunAfter("generateGDScript")

        from("${rootProject.projectDir}/../ios/config")
        into("${project.extra["outputDir"]}/ios/plugins")
        include("**/*.gdip")

        eachFile { println("[DEBUG] Processing file: $relativePath") }

        val tokens =
            mapOf(
                "pluginName" to (project.extra["pluginName"] as String),
                "iosInitializationMethod" to (project.extra["iosInitializationMethod"] as String),
                "iosDeinitializationMethod" to (project.extra["iosDeinitializationMethod"] as String),
            )

        filter { line: String ->
            tokens.entries.fold(line) { acc, (key, value) ->
                val token = "@$key@"
                if (acc.contains(token)) {
                    println("\t[DEBUG] Replacing token $token with: $value")
                    acc.replace(token, value)
                } else {
                    acc
                }
            }
        }

        inputs.files(
            rootProject.file("config/plugin.properties"),
            rootProject.file("../ios/config/ios.properties"),
        )
        inputs.property("pluginName", project.extra["pluginName"])
        inputs.property("iosInitializationMethod", project.extra["iosInitializationMethod"])
        inputs.property("iosDeinitializationMethod", project.extra["iosDeinitializationMethod"])

        outputs.dir("${project.extra["outputDir"]}/ios/plugins")
    }

    registerGdscriptFormatTask(
        name = "checkGdscriptFormat",
        description = "Checks gdscript-formatter compliance of GDScript source files (dry-run)",
        check = true,
    )

    registerGdscriptFormatTask(
        name = "formatGdscriptSource",
        description = "Formats GDScript source files in-place using gdscript-formatter",
        check = false,
    )
}
