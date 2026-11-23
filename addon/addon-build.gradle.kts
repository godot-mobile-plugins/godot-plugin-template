//
// © 2024-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

import org.gradle.api.file.FileSystemOperations
import org.gradle.process.ExecOperations
import javax.inject.Inject

plugins {
    id("base-conventions")
    alias(libs.plugins.undercouch.download)
}

// -- Load config data classes --------------------------------------------------
//
// base-conventions puts typed loaders (loadPluginConfig, loadIosConfig, …) on
// the compilation classpath, so all config values are accessed via the typed
// objects rather than project.extra casts.

val pluginConfig = loadPluginConfig()
val iosConfig = loadIosConfig()
val godotConfig = loadGodotConfig()
val buildConfig = loadBuildConfig()

interface Injected {
    @get:Inject
    val fsOps: FileSystemOperations

    @get:Inject
    val execOps: ExecOperations
}

// -- Collect all catalog library aliases (used in @androidDependencies@ token) -

val androidDependencies =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .run {
            libraryAliases
                .filter { it.startsWith("runtime.") }
                .map { findLibrary(it).get().get() }
        }

// -- Helpers -------------------------------------------------------------------

/** Wraps each item in a [List] in double-quotes and joins with ", ". */
fun List<String>.toQuotedString(): String = joinToString(", ") { "\"$it\"" }

/**
 * Formats a list of [SpmDependency] items into the GDScript dictionary literal
 * format used as the body of a `const SPM_DEPENDENCIES: Array = [ @spmDependencies@ ]`
 * token substitution.
 *
 * Each dependency becomes a GDScript dictionary with [StringName] keys (the `&"key"`
 * syntax), e.g.:
 * ```
 * {&"url": "https://github.com/owner/repo", &"version": "1.2.3", &"products": ["ProductA", "ProductB"]}
 * ```
 *
 * Multiple entries are joined with `, `. The surrounding square brackets are
 * intentionally omitted because they are already present in the GDScript constant
 * declaration that hosts the `@spmDependencies@` token.
 */
fun List<SpmDependency>.toGdscriptFormat(): String =
    joinToString(", ") { dep ->
        val products = dep.products.joinToString(", ") { "\"$it\"" }
        """{&"url": "${dep.url}", &"version": "${dep.version}", &"products": [$products]}"""
    }

/**
 * Registers a GDScript format task (check or in-place fix).
 *
 * Both [checkGdscriptFormat] and [formatGdscriptSource] share identical source-file
 * discovery, gdformatrc lifecycle, and working directory - only the gdformat flag differs.
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
    // templateDir and sharedTemplateDir are computed directory-layout paths set by
    // base-conventions; they are accessed via project.extra because they are not
    // derivable from the typed config objects.
    val addonSrcDir = file(extra["templateDir"] as String)
    val sharedSrcDir = file(extra["sharedTemplateDir"] as String)
    val gdformatrcSource = file("$projectDir/../.github/config/.gdformatrc")
    val gdformatrcDest = addonSrcDir.resolve(".gdformatrc")
    val sharedGdformatrcDest = sharedSrcDir.resolve(".gdformatrc")
    val excludePatterns = listOf("**/*Plugin.gd", "**/MediationNetwork.gd")

    val fsOps = project.objects.newInstance<Injected>().fsOps

    register<Exec>(name) {
        this.description = description
        this.group = if (check) "verification" else "formatting"

        workingDir = addonSrcDir

        doFirst {
            fsOps.copy {
                from(gdformatrcSource)
                into(addonSrcDir)
            }
            if (sharedSrcDir.exists()) {
                fsOps.copy {
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
                            exclude("android/**")
                            exclude("ios/**")
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

// -- Tasks ---------------------------------------------------------------------

tasks {
    // Directory-layout paths are computed by base-conventions and stored in
    // project.extra; capture them here at TaskContainerScope level to avoid the
    // task-receiver scoping trap where bare extra["key"] inside a register { }
    // lambda resolves to the task's own (empty) ExtraPropertiesExtension rather
    // than the project's.  Config data values (pluginConfig, iosConfig, …) are
    // top-level vals captured from the build script closure and are not subject
    // to this trap.
    val addonSrcDir = file(extra["templateDir"] as String)
    val sharedSrcDir = file(extra["sharedTemplateDir"] as String)
    val outputDir = extra["outputDir"] as String

    register<Delete>("cleanOutput") {
        group = "clean"
        delete(
            fileTree(outputDir) {
                include("**/*.gd", "**/*.cfg", "**/*.png", "**/*.gdip")
            },
        )
        delete(
            fileTree("$outputDir/ios/plugins") {
                include("**/*")
            },
        )
    }

    register<Copy>("copyAssets") {
        description = "Copies plugin assets such as PNG images to the output directory"
        group = "generate"
        from(addonSrcDir)
        into("$outputDir/addons/${pluginConfig.pluginName}")
        include("**/*.png")
        inputs.files(fileTree(addonSrcDir) { include("**/*.png") })
    }

    val allTokens: Map<String, String> by lazy {
        buildMap {
            // Custom tokens from project.extra (comma-separated values are quoted)
            project.extra.properties.forEach { (k, v) ->
                val raw = v.toString()
                put(
                    k,
                    if (raw.contains(",")) {
                        raw.split(",").joinToString(", ") { "\"${it.trim()}\"" }
                    } else {
                        raw
                    },
                )
            }

            // PluginConfig scalars
            put("pluginNodeName", pluginConfig.pluginNodeName)
            put("pluginName", pluginConfig.pluginName)
            put("pluginPackageName", pluginConfig.pluginPackageName)
            put("pluginVersion", pluginConfig.pluginVersion)
            put("pluginModuleName", pluginConfig.pluginModuleName)
            put("iosInitializationMethod", pluginConfig.iosInitializationMethod)
            put("iosDeinitializationMethod", pluginConfig.iosDeinitializationMethod)
            // GodotConfig scalars
            put("godotVersion", godotConfig.godotVersion)
            put("godotReleaseType", godotConfig.godotReleaseType)
            put("godotAarUrl", godotConfig.godotAarUrl)
            put("godotAarFile", godotConfig.godotAarFile)
            // IosConfig scalars
            put("iosPlatformVersion", iosConfig.platformVersion)
            put("iosSwiftVersion", iosConfig.swiftVersion)
            // Formatted collections
            put("androidDependencies", androidDependencies.joinToString(", ") { "\"$it\"" })
            put("iosFrameworks", iosConfig.frameworks.toQuotedString())
            put("iosEmbeddedFrameworks", iosConfig.embeddedFrameworks.toQuotedString())
            put("iosLinkerFlags", iosConfig.linkerFlags.toQuotedString())
            put("iosBundleFiles", iosConfig.bundleFiles.toQuotedString())
            put("spmDependencies", iosConfig.spmDependencies.toGdscriptFormat())
        }
    }

    register<Copy>("generateSharedGDScript") {
        description = "Copies shared GDScript templates to the GMPShared output directory and replaces tokens"
        group = "generate"
        onlyIf("shared source directory contains GDScript or config files") {
            sharedSrcDir.exists() &&
                fileTree(sharedSrcDir) { include("**/*.gd", "**/*.cfg", "**/*.json") }.files.isNotEmpty()
        }

        from(sharedSrcDir)
        into("$outputDir/addons/GMPShared")
        include("**/*.gd", "**/*.cfg", "**/*.json")

        eachFile { println("[DEBUG] Processing shared file $relativePath") }

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

        if (sharedSrcDir.exists()) inputs.dir(sharedSrcDir)
        inputs.files(
            rootProject.file("config/plugin.properties"),
            rootProject.file("../addon/config/addon-build.properties"),
            rootProject.file("../ios/config/ios.properties"),
            rootProject.file("../ios/config/spm_dependencies.json"),
        )
        inputs.property("pluginName", pluginConfig.pluginName)
        inputs.property("pluginNodeName", pluginConfig.pluginNodeName)
        inputs.property("pluginVersion", pluginConfig.pluginVersion)
        inputs.property("pluginPackage", pluginConfig.pluginPackageName)
        inputs.property("androidDependencies", androidDependencies.joinToString())
        inputs.property("iosPlatformVersion", iosConfig.platformVersion)
        inputs.property("iosFrameworks", iosConfig.frameworks.joinToString())
        inputs.property("iosEmbeddedFrameworks", iosConfig.embeddedFrameworks.joinToString())
        inputs.property("iosLinkerFlags", iosConfig.linkerFlags.joinToString())
        inputs.property("iosBundleFiles", iosConfig.bundleFiles.joinToString())
        inputs.property("iosSpmDependencies", iosConfig.spmDependencies.joinToString())

        outputs.dir("$outputDir/addons/GMPShared")
    }

    register<Copy>("generateGDScript") {
        description = "Copies the GDScript templates and plugin config to the output directory and replaces tokens"
        group = "generate"
        dependsOn("generateSharedGDScript")
        finalizedBy("copyAssets")

        from(addonSrcDir)
        into("$outputDir/addons/${pluginConfig.pluginName}")
        include("**/*.gd", "**/*.cfg")

        eachFile { println("[DEBUG] Processing file: $relativePath") }

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
            rootProject.file("../addon/config/addon-build.properties"),
            rootProject.file("../ios/config/ios.properties"),
            rootProject.file("../ios/config/spm_dependencies.json"),
        )
        inputs.property("pluginName", pluginConfig.pluginName)
        inputs.property("pluginNodeName", pluginConfig.pluginNodeName)
        inputs.property("pluginVersion", pluginConfig.pluginVersion)
        inputs.property("pluginPackage", pluginConfig.pluginPackageName)
        inputs.property("androidDependencies", androidDependencies.joinToString())
        inputs.property("iosPlatformVersion", iosConfig.platformVersion)
        inputs.property("iosFrameworks", iosConfig.frameworks.joinToString())
        inputs.property("iosEmbeddedFrameworks", iosConfig.embeddedFrameworks.joinToString())
        inputs.property("iosLinkerFlags", iosConfig.linkerFlags.joinToString())
        inputs.property("iosBundleFiles", iosConfig.bundleFiles.joinToString())
        inputs.property("iosSpmDependencies", iosConfig.spmDependencies.joinToString())

        outputs.dir("$outputDir/addons/${pluginConfig.pluginName}")
    }

    register<Copy>("generateiOSConfig") {
        description = "Copies the iOS plugin config to the output directory and replaces tokens"
        group = "generate"
        mustRunAfter("generateGDScript")

        from("${rootProject.projectDir}/../ios/config")
        into("$outputDir/ios/plugins")
        include("**/*.gdip")

        eachFile { println("[DEBUG] Processing file: $relativePath") }

        val tokens =
            mapOf(
                "pluginName" to pluginConfig.pluginName,
                "iosInitializationMethod" to pluginConfig.iosInitializationMethod,
                "iosDeinitializationMethod" to pluginConfig.iosDeinitializationMethod,
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
        inputs.property("pluginName", pluginConfig.pluginName)
        inputs.property("iosInitializationMethod", pluginConfig.iosInitializationMethod)
        inputs.property("iosDeinitializationMethod", pluginConfig.iosDeinitializationMethod)

        outputs.dir("$outputDir/ios/plugins")
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

    // -- Headless Godot integration tests --------------------------------------
    //
    // downloadGodotEditor   -> fetches the Godot editor for config/godot.properties
    //                          into godotEditorDir (cached; skipped when present)
    // prepareIntegrationTestProject -> assembles a throw-away Godot project from
    //                          src/test/integration/ plus the generated addon files
    // runIntegrationTests   -> imports the project, runs the GDScript runner in
    //                          headless Godot and prints the TEST RESULTS table

    val editorPlatform = runCatching { GodotEditorPlatform.current() }
    val godotEditorDir =
        file(
            "${gradle.extra["godotEditorDir"]}/${godotConfig.godotVersionString}/" +
                (editorPlatform.getOrNull()?.archiveSuffix ?: "unsupported"),
        )
    val integrationTestSrcDir = file("$projectDir/src/test/integration")
    val integrationTestProjectDir = layout.buildDirectory.dir("integration-test/project")
    val integrationTestResultsDir = layout.buildDirectory.dir("test-results/integrationTest")

    register<de.undercouch.gradle.tasks.download.Download>("downloadGodotEditor") {
        description = "Downloads the Godot editor for the configured Godot version (used by integration tests)"
        group = "setup"

        val expectedVersionString = godotConfig.godotVersionString
        val versionFile = godotEditorDir.resolve("GODOT_VERSION")
        // Placeholder platform only used for the URL on unsupported hosts; doFirst fails before downloading.
        val platform = editorPlatform.getOrDefault(GodotEditorPlatform.LINUX_X86_64)
        val archiveFile = godotEditorDir.parentFile.resolve(godotConfig.godotEditorZip(platform))
        val execOps = objects.newInstance<Injected>().execOps

        inputs.property("godotVersion", godotConfig.godotVersion)
        inputs.property("godotReleaseType", godotConfig.godotReleaseType)
        inputs.property("godotEditorDir", godotEditorDir.absolutePath)

        onlyIf("Godot editor is not already cached") {
            val executable = findGodotEditorExecutable(godotEditorDir, platform)
            if (versionFile.exists() && versionFile.readText().trim() == expectedVersionString && executable != null) {
                logger.lifecycle(
                    "Godot editor {} already present at {}. Skipping download.",
                    expectedVersionString,
                    executable.absolutePath,
                )
                return@onlyIf false
            }
            true
        }

        doFirst {
            editorPlatform.getOrThrow()
            archiveFile.parentFile.mkdirs()
        }

        src(godotConfig.godotEditorUrl(platform))
        dest(archiveFile)
        overwrite(false)
        tempAndMove(true)

        doLast {
            // unzip (rather than zipTree) keeps executable bits and the macOS .app bundle intact
            delete(godotEditorDir)
            godotEditorDir.mkdirs()
            execOps.exec {
                commandLine("unzip", "-q", "-o", archiveFile.absolutePath, "-d", godotEditorDir.absolutePath)
            }

            val executable =
                findGodotEditorExecutable(godotEditorDir, platform)
                    ?: throw GradleException(
                        "Extraction succeeded but no Godot editor executable was found in " +
                            godotEditorDir.absolutePath,
                    )
            executable.setExecutable(true)

            archiveFile.delete()
            versionFile.writeText(expectedVersionString)

            println(
                "Godot editor $expectedVersionString successfully downloaded and extracted to " +
                    executable.absolutePath,
            )
        }
    }

    register<Sync>("prepareIntegrationTestProject") {
        description = "Assembles the headless Godot project used by the integration tests"
        group = "verification"
        dependsOn("copyAssets", "generateGDScript", "generateSharedGDScript")

        from(integrationTestSrcDir)
        from("$outputDir/addons") { into("addons") }
        into(integrationTestProjectDir)
        // Keep Godot's import cache between runs; --import refreshes it.
        preserve { include(".godot/**") }
    }

    register("runIntegrationTests") {
        description = "Runs the GDScript integration tests in headless Godot and prints a TEST RESULTS table"
        group = "verification"

        if (!buildConfig.integrationTestsEnabled) {
            doLast {
                logger.lifecycle(
                    "Integration tests are disabled. Set integrationTests.enabled=true in " +
                        "common/config/build.properties to run them.",
                )
            }
            return@register
        }

        dependsOn("downloadGodotEditor", "prepareIntegrationTestProject")

        val execOps = objects.newInstance<Injected>().execOps
        val logDir = layout.buildDirectory.dir("integration-test")

        doLast {
            val platform = editorPlatform.getOrThrow()
            val godot =
                findGodotEditorExecutable(godotEditorDir, platform)
                    ?: throw GradleException("Godot editor not found in ${godotEditorDir.absolutePath}")
            val projectPath = integrationTestProjectDir.get().asFile.absolutePath
            val resultsDir = integrationTestResultsDir.get().asFile
            delete(resultsDir)
            resultsDir.mkdirs()

            // 1. Import the project so global classes (class_name) and resources are registered
            val importLog = logDir.get().file("import.log").asFile
            val importResult =
                importLog.outputStream().use { log ->
                    execOps.exec {
                        commandLine(godot.absolutePath, "--headless", "--path", projectPath, "--import")
                        standardOutput = log
                        errorOutput = log
                        isIgnoreExitValue = true
                    }
                }
            if (importResult.exitValue != 0) {
                throw GradleException(
                    "Godot project import failed (exit code ${importResult.exitValue}). See ${importLog.absolutePath}",
                )
            }

            // 2. Run the tests; the runner writes one JUnit XML file per suite into resultsDir
            val runResult =
                execOps.exec {
                    commandLine(
                        godot.absolutePath,
                        "--headless",
                        "--path",
                        projectPath,
                        "--script",
                        "res://runner/IntegrationTestRunner.gd",
                        "--",
                        "--junit-dir=${resultsDir.absolutePath}",
                    )
                    isIgnoreExitValue = true
                }

            val suites = parseSuiteResults(resultsDir)
            printTestResultsTable(
                suites,
                emptyNotice =
                    listOf(
                        "No test results found.",
                        "Expected location: build/test-results/integrationTest/",
                    ),
            )
            failBuildIfTestsFailed(suites)

            if (runResult.exitValue != 0) {
                throw GradleException(
                    "Godot exited with code ${runResult.exitValue} while running the integration tests - " +
                        "see the Godot output above.",
                )
            }
        }
    }
}
