//
// © 2026-present https://github.com/cengiz-pz
//

import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("spm-conventions")
//    alias(libs.plugins.spotless)
}

// Load configuration from project root
apply(from = "$projectDir/config/ios.gradle.kts")

/*
spotless {
    cpp {
        target("$projectDir/src/**/*.m", "$projectDir/src/**/*.mm", "$projectDir/src/**/*.h")

        clangFormat(libs.versions.clang.format.get()).style("../.github/config/.clang-format")
    }
}
"*/

interface Injected {
    @get:Inject
    val execOps: ExecOperations
}

@Suppress("UNCHECKED_CAST")
val readSpmDependencies =
    project.extra["readSpmDependencies"]
        as (File) -> List<SpmDependency>

tasks {
    val pluginDir: String by project.extra
    val repositoryRootDir: String by project.extra
    val archiveDir: String by project.extra
    val demoDir: String by project.extra

    val godotDir: String by gradle.extra

    register<Exec>("removeGodotDirectory") {
        description = "Removes the directory where Godot sources were downloaded"

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        commandLine("bash", buildScript.absolutePath, "-g")
        environment("INVOKED_BY_GRADLE", "true")
    }

    register<Exec>("downloadGodot") {
        description = "Downloads Godot sources into the configured directory"

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        val godotVersion: String by project.extra
        val godotReleaseType: String by project.extra
        inputs.property("godotVersion", godotVersion)
        inputs.property("godotReleaseType", godotReleaseType)
        inputs.property("godotDir", godotDir)

        val godotDirectory: File = file(godotDir)
        val versionFile = godotDirectory.resolve("GODOT_VERSION")

        // Let Gradle check staleness without creating any directories
        outputs.upToDateWhen {
            versionFile.exists() && versionFile.readText().trim() == godotVersion
        }

        doFirst {
            if (godotDirectory.exists()) {
                if (!versionFile.exists()) {
                    throw GradleException(
                        "ERROR: Godot directory '${godotDirectory.absolutePath}' already exists " +
                            "but contains no GODOT_VERSION file.",
                    )
                } else {
                    val existingVersion = versionFile.readText().trim()
                    if (existingVersion != godotVersion) {
                        throw GradleException(
                            "ERROR: Godot directory '${godotDirectory.absolutePath}' already exists but " +
                                "contains version '$existingVersion', which does not match the " +
                                "configured version '$godotVersion'. " +
                                "Remove the directory (or run 'removeGodotDirectory') before downloading again, " +
                                "or update 'godotVersion' in config/godot.properties.",
                        )
                    }
                    // Version matches — skip silently (upToDateWhen will have already short-circuited in normal runs)
                }
            }
        }

        commandLine("bash", buildScript.absolutePath, "-G")
        environment("INVOKED_BY_GRADLE", "true")
    }

    register<Exec>("generateGodotHeaders") {
        description = "Runs Godot build and terminates after Godot header files have been generated"

        dependsOn(
            "downloadGodot",
        )

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        val godotDirectory: File = file(godotDir)
        val generatedFiles =
            project.fileTree(godotDirectory).matching {
                include("**/*.gen.h")
                include("**/*.gen.cpp")
            }

        val internalBuildFiles =
            project.fileTree(godotDirectory).matching {
                include(".scons*")
            }

        // Inputs: Include everything in the directory EXCEPT the generated files
        inputs.files(project.fileTree(godotDirectory).minus(generatedFiles).minus(internalBuildFiles))

        // Outputs: Use the defined generated files pattern
        outputs.files(generatedFiles)

        commandLine("bash", buildScript.absolutePath, "-H")
        environment("INVOKED_BY_GRADLE", "true")
    }

    register("resetSPMDependencies") {
        description = "Removes SPM dependencies from the Xcode project and cleans up all SPM artifacts"

        inputs.files(fileTree("$projectDir/config"))

        val execOps = objects.newInstance<Injected>().execOps

        doLast {
            val iosConfigFile = file("$projectDir/config/spm_dependencies.json")
            val deps = readSpmDependencies(iosConfigFile)
            val pluginModuleName = project.extra["pluginModuleName"] as String
            val xcodeproj = "$projectDir/plugin.xcodeproj"
            val scriptDir = file("$repositoryRootDir/script")

            if (deps.isEmpty()) {
                println("Warning: No dependencies found for plugin. Skipping SPM dependency removal.")
            } else {
                println("Removing SPM dependencies from project...")
                deps.forEach { dep ->
                    dep.products.forEach { product ->
                        execOps.exec {
                            commandLine(
                                "ruby",
                                "$scriptDir/spm_manager.rb",
                                "-d",
                                xcodeproj,
                                dep.url,
                                dep.version,
                                product,
                            )
                        }
                    }
                }

                // Re-run xcodebuild to regenerate Package.resolved from the updated project state.
                // This ensures transitive dependencies of removed packages are also purged.
                println("Regenerating Package.resolved after dependency removal...")
                execOps.exec {
                    commandLine(
                        "xcodebuild",
                        "-resolvePackageDependencies",
                        "-project",
                        xcodeproj,
                        "-scheme",
                        "${pluginModuleName}_plugin",
                        "-derivedDataPath",
                        "$projectDir/build/DerivedData",
                    )
                    isIgnoreExitValue = true
                }
            }

            val spmDir = file("$xcodeproj/project.xcworkspace/xcshareddata/swiftpm")
            val resolvedFile = spmDir.resolve("Package.resolved")
            if (resolvedFile.exists()) {
                println("Removing ${resolvedFile.path} ...")
                resolvedFile.delete()
            }

            val sourcePackagesDir = file("$projectDir/build/DerivedData/SourcePackages")
            if (sourcePackagesDir.exists()) {
                println("Removing SPM cache directory ${sourcePackagesDir.path} ...")
                sourcePackagesDir.deleteRecursively()
            }
        }
    }

    register("updateSPMDependencies") {
        description = "Adds SPM dependencies from $projectDir/config/spm_dependencies.json into the Xcode project"

        inputs.files(fileTree("$projectDir/config"))
        outputs.dir("$projectDir/plugin.xcodeproj")

        finalizedBy("resolveSPMDependencies")

        val execOps = objects.newInstance<Injected>().execOps

        doLast {
            val iosConfigFile = file("$projectDir/config/spm_dependencies.json")
            val deps = readSpmDependencies(iosConfigFile)
            val xcodeproj = "$projectDir/plugin.xcodeproj"
            val scriptDir = file("$repositoryRootDir/script")

            if (deps.isEmpty()) {
                println("Warning: No dependencies found for plugin. Skipping SPM update.")
                return@doLast
            }

            val totalProducts = deps.sumOf { it.products.size }
            val noun = if (totalProducts == 1) "dependency" else "dependencies"
            println("Found $totalProducts SPM $noun:")
            deps.forEach { dep ->
                dep.products.forEach { product ->
                    println("\t• $product (${dep.url} @ ${dep.version})")
                }
            }
            println()

            // Verify Ruby and the xcodeproj gem are available
            val rubyAvailable =
                execOps
                    .exec {
                        commandLine("which", "ruby")
                        isIgnoreExitValue = true
                    }.exitValue == 0
            if (!rubyAvailable) {
                throw GradleException("Ruby is required to inject SPM dependencies but was not found on PATH.")
            }

            val gemAvailable =
                execOps
                    .exec {
                        commandLine("gem", "list", "-i", "^xcodeproj\$")
                        isIgnoreExitValue = true
                    }.exitValue == 0
            if (!gemAvailable) {
                println("Installing 'xcodeproj' Ruby gem...")
                execOps.exec {
                    commandLine("gem", "install", "xcodeproj", "--user-install")
                }
            }

            println("Updating Xcode project with SPM dependencies...")
            deps.forEach { dep ->
                dep.products.forEach { product ->
                    execOps.exec {
                        commandLine(
                            "ruby",
                            "$scriptDir/spm_manager.rb",
                            "-a",
                            xcodeproj,
                            dep.url,
                            dep.version,
                            product,
                        )
                    }
                }
            }

            println("SPM update completed.")
        }
    }

    register<Exec>("resolveSPMDependencies") {
        description = "Resolves SPM package dependencies via xcodebuild (invoked by build_ios.sh -r)"

        mustRunAfter("updateSPMDependencies")

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        commandLine("bash", buildScript.absolutePath, "-r")
        environment("INVOKED_BY_GRADLE", "true")

        val xcodeproj = "$projectDir/plugin.xcodeproj"
        val resolvedFile = file("$xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved")

        inputs.file("$projectDir/config/spm_dependencies.json")
        inputs.files(
            fileTree(xcodeproj) {
                include("**/*.pbxproj", "**/project.pbxproj")
            },
        )
        inputs.file(buildScript)

        outputs.file(resolvedFile)
        outputs.dir("$projectDir/build/DerivedData/SourcePackages")
    }

    register("validateSwiftVersion") {
        description = "Fails the build with a clear error if swift_version is missing from ios.properties"

        val iosConfigFile = file("$projectDir/config/ios.properties")
        inputs.file(iosConfigFile)

        doLast {
            val props = java.util.Properties()
            iosConfigFile.inputStream().use { props.load(it) }
            val swiftVersion = props.getProperty("swift_version")?.trim()
            if (swiftVersion.isNullOrBlank()) {
                throw GradleException(
                    "ERROR: 'swift_version' is not configured in ${iosConfigFile.absolutePath}.\n" +
                        "Please add it before building, e.g.:\n" +
                        "    swift_version=5.9",
                )
            }
        }
    }

    register<Exec>("buildiOSDebug") {
        dependsOn(
            "validateSwiftVersion",
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("generateiOSConfig"),
            project(":addon").tasks.named("copyAssets"),
            "updateSPMDependencies",
            "resolveSPMDependencies",
            "generateGodotHeaders",
        )

        inputs.files(project(":addon").tasks.named("generateGDScript").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("generateiOSConfig").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("copyAssets").map { it.outputs.files })

        inputs.dir("$projectDir/src")
        inputs.files(fileTree("$rootDir/config"))
        inputs.files(fileTree("$projectDir/config"))

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        outputs.dir("$projectDir/build/framework")

        finalizedBy("copyiOSBuildArtifacts")

        commandLine("bash", buildScript.absolutePath, "-b")
        environment("INVOKED_BY_GRADLE", "true")

        doLast {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = LocalDateTime.now().format(formatter)
            println("iOS build completed at: $current")
        }
    }

    register<Exec>("buildiOSRelease") {
        dependsOn(
            "validateSwiftVersion",
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("generateiOSConfig"),
            project(":addon").tasks.named("copyAssets"),
            "updateSPMDependencies",
            "resolveSPMDependencies",
            "generateGodotHeaders",
        )

        inputs.files(project(":addon").tasks.named("generateGDScript").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("generateiOSConfig").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("copyAssets").map { it.outputs.files })

        inputs.dir("$projectDir/src")
        inputs.files(fileTree("$rootDir/config"))
        inputs.files(fileTree("$projectDir/config"))

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        outputs.dir("$projectDir/build/framework")

        finalizedBy("copyiOSBuildArtifacts")

        commandLine("bash", buildScript.absolutePath, "-B")
        environment("INVOKED_BY_GRADLE", "true")

        doLast {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = LocalDateTime.now().format(formatter)
            println("iOS build completed at: $current")
        }
    }

    register<Exec>("buildiOSDebugSimulator") {
        dependsOn(
            "validateSwiftVersion",
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("generateiOSConfig"),
            project(":addon").tasks.named("copyAssets"),
            "updateSPMDependencies",
            "resolveSPMDependencies",
            "generateGodotHeaders",
        )

        inputs.files(project(":addon").tasks.named("generateGDScript").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("generateiOSConfig").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("copyAssets").map { it.outputs.files })

        inputs.dir("$projectDir/src")
        inputs.files(fileTree("$rootDir/config"))
        inputs.files(fileTree("$projectDir/config"))

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        outputs.dir("$projectDir/build/framework")

        finalizedBy("copyiOSBuildArtifacts")

        commandLine("bash", buildScript.absolutePath, "-bs")
        environment("INVOKED_BY_GRADLE", "true")

        doLast {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = LocalDateTime.now().format(formatter)
            println("iOS build completed at: $current")
        }
    }

    register<Exec>("buildiOSReleaseSimulator") {
        dependsOn(
            "validateSwiftVersion",
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("generateiOSConfig"),
            project(":addon").tasks.named("copyAssets"),
            "updateSPMDependencies",
            "resolveSPMDependencies",
            "generateGodotHeaders",
        )

        inputs.files(project(":addon").tasks.named("generateGDScript").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("generateiOSConfig").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("copyAssets").map { it.outputs.files })

        inputs.dir("$projectDir/src")
        inputs.files(fileTree("$rootDir/config"))
        inputs.files(fileTree("$projectDir/config"))

        val buildScript = file("$repositoryRootDir/script/build_ios.sh")
        inputs.file(buildScript)

        outputs.dir("$projectDir/build/framework")

        finalizedBy("copyiOSBuildArtifacts")

        commandLine("bash", buildScript.absolutePath, "-Bs")
        environment("INVOKED_BY_GRADLE", "true")

        doLast {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val current = LocalDateTime.now().format(formatter)
            println("iOS build completed at: $current")
        }
    }

    register("buildiOS") {
        description = "Builds both debug and release"

        dependsOn("buildiOSDebug")
        dependsOn("buildiOSRelease")
    }

    register<Sync>("copyiOSBuildArtifacts") {
        description = "Copies iOS build artifacts (xcframeworks and addon files) to the plugin directory"

        dependsOn(
            project(":addon").tasks.named("copyAssets"),
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("generateiOSConfig"),
        )
        mustRunAfter(
            "buildiOSDebug",
            "buildiOSDebugSimulator",
            "buildiOSRelease",
            "buildiOSReleaseSimulator",
        )

        val pluginName = project.extra["pluginName"] as String
        val buildDir = file(projectDir).resolve("build")
        val frameworkDir = buildDir.resolve("framework")
        val libDir = buildDir.resolve("lib")

        val destDir = file(pluginDir).resolve("ios")
        destinationDir = destDir

        doFirst {
            // Make sure existing framework cache is writable before the sync overwrites it
            val frameworkCache = destDir.resolve("ios/framework")
            if (frameworkCache.exists()) {
                frameworkCache.walkBottomUp().forEach { it.setWritable(true) }
            }

            val execOps = objects.newInstance<Injected>().execOps
            frameworkDir.mkdirs()

            // Combine per-slice archives into a single xcframework per variant.
            // Any slice whose .a file does not exist (i.e. was not built) is silently skipped,
            // so partial builds (debug-only, release-only, device-only, etc.) still work.
            fun createXcframework(
                variantName: String,
                archiveNames: List<String>,
            ) {
                val availableLibs =
                    archiveNames.mapNotNull { archiveName ->
                        val lib = libDir.resolve("$archiveName/Products/usr/local/lib/$pluginName.a")
                        if (lib.exists()) lib else null
                    }
                if (availableLibs.isEmpty()) {
                    println("Skipping $pluginName.$variantName.xcframework: no build artifacts found.")
                    return
                }
                val output = frameworkDir.resolve("$pluginName.$variantName.xcframework")
                if (output.exists()) output.deleteRecursively()

                val args = mutableListOf("xcodebuild", "-create-xcframework")
                availableLibs.forEach { lib -> args += listOf("-library", lib.absolutePath) }
                args += listOf("-output", output.absolutePath)

                println(
                    "Creating $pluginName.$variantName.xcframework " +
                        "from ${availableLibs.size} slice(s): ${availableLibs.map {
                            it.parentFile.parentFile.parentFile.name
                        }}",
                )
                execOps.exec { commandLine(args) }
            }

            // Debug xcframework: debug device + debug simulator
            createXcframework("debug", listOf("ios_debug.xcarchive", "sim_debug.xcarchive"))
            // Release xcframework: release device + release simulator
            createXcframework("release", listOf("ios_release.xcarchive", "sim_release.xcarchive"))
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        // Search ALL DerivedData folders so it works for debug AND release builds
        val derivedDataDir = buildDir.resolve("DerivedData")
        inputs.dir(derivedDataDir).optional(true)
        inputs.dir(frameworkDir).optional(true)

        outputs.dir(destDir)

        // Third-party xcframeworks (from SPM) – works even if only debug or release exists
        from(fileTree(derivedDataDir) { include("**/artifacts/**/*.xcframework/**") }) {
            includeEmptyDirs = false
            eachFile {
                val segs = relativePath.segments
                val xcfwIdx = segs.indexOfFirst { it.endsWith(".xcframework", ignoreCase = true) }
                if (xcfwIdx >= 0) {
                    relativePath = RelativePath(true, "ios", "framework", *segs.drop(xcfwIdx).toTypedArray())
                } else {
                    exclude()
                }
            }
        }

        // Plugin xcframeworks: one combined debug, one combined release
        into("ios/plugins") {
            from(frameworkDir) {
                include("$pluginName.debug.xcframework/**")
                include("$pluginName.release.xcframework/**")
            }
        }

        from("$repositoryRootDir/addon/build/output") {
            include("addons/${project.extra["pluginName"]}/**")
            include("ios/plugins/*.gdip")
        }
    }

    register<Copy>("installToDemoiOS") {
        description = "Copies the assembled iOS plugin to demo application's addons directory"

        dependsOn(
            "buildiOSDebug",
            "copyiOSBuildArtifacts",
        )

        val destDir = file(demoDir)
        destinationDir = destDir

        doFirst {
            val frameworkCache = destDir.resolve("ios/framework")
            if (frameworkCache.exists()) {
                frameworkCache.walkBottomUp().forEach { it.setWritable(true) }
            }
        }

        duplicatesStrategy = DuplicatesStrategy.WARN

        into(".") {
            from("$pluginDir/ios")
        }

        outputs.dir(destinationDir)
    }

    register<Delete>("uninstalliOS") {
        description = (
            "Keep .uid and .import files and delete the rest inside demo app's plugin directory. " +
                "Delete plugin files inside demo ios directory."
        )

        delete(
            fileTree("$demoDir/addons/${project.extra["pluginName"]}") {
                include("**/*")
                exclude("**/*.uid")
                exclude("**/*.import")
            },
        )

        // iOS plugins cleanup (catches .gdip + .xcframework + .framework)
        val pluginName = project.extra["pluginName"] as String
        val pluginsDir = file("$demoDir/ios/plugins")

        // Delete every file/folder that belongs to this plugin
        delete(
            pluginsDir.listFiles()?.filter { it.name.startsWith("$pluginName.") }.orEmpty(),
        )
    }

    register<Delete>("cleaniOSBuild") {
        group = "clean"
        description = "Cleans iOS build outputs"

        val iosBuildDir =
            provider {
                project.file("$projectDir/build")
            }

        delete(iosBuildDir)

        doLast {
            val dir = iosBuildDir.get()
            if (dir.exists()) {
                logger.lifecycle("Removed iOS build directory: ${dir.absolutePath}")
            } else {
                logger.lifecycle("iOS build directory did not exist (already clean): ${dir.absolutePath}")
            }
        }
    }

    register<Zip>("createiOSArchive") {
        dependsOn("buildiOS", "copyiOSBuildArtifacts")

        val archiveName = project.extra["pluginArchiveiOS"] as String
        val outputDir = project.extra["archiveDir"] as String
        val sourceDir = "$pluginDir/ios"

        archiveFileName.set(archiveName)
        destinationDirectory.set(layout.projectDirectory.dir(outputDir))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        into("res") {
            from(layout.projectDirectory.dir(sourceDir)) {
                includeEmptyDirs = false
            }
        }

        doLast {
            println("iOS zip archive created at: ${archiveFile.get().asFile.path}")
        }
    }

    register<Exec>("checkIosFormat") {
        description = "Checks clang-format compliance of iOS source files (dry-run, no changes written)"
        group = "formatting"

        val iosSrcDir = file("$projectDir/src")
        workingDir = iosSrcDir

        doFirst {
            val sourceFiles =
                fileTree(iosSrcDir) {
                    include("**/*.mm", "**/*.m", "**/*.h")
                }.files
                    .map { it.relativeTo(iosSrcDir).path }
                    .sorted()

            if (sourceFiles.isEmpty()) {
                throw GradleException("checkIosFormat: no source files found under ${iosSrcDir.absolutePath}")
            }

            commandLine(
                buildList {
                    add("clang-format")
                    add("--style=file:../../.github/config/.clang-format")
                    add("--dry-run")
                    add("--Werror")
                    addAll(sourceFiles)
                },
            )
        }
    }

    register<Exec>("formatIosSource") {
        description = "Formats iOS source files in-place using clang-format"
        group = "formatting"

        val iosSrcDir = file("$projectDir/src")
        workingDir = iosSrcDir

        doFirst {
            val sourceFiles =
                fileTree(iosSrcDir) {
                    include("**/*.mm", "**/*.m", "**/*.h")
                }.files
                    .map { it.relativeTo(iosSrcDir).path }
                    .sorted()

            if (sourceFiles.isEmpty()) {
                throw GradleException("formatIosSource: no source files found under ${iosSrcDir.absolutePath}")
            }

            commandLine(
                buildList {
                    add("clang-format")
                    add("--style=file:../../.github/config/.clang-format")
                    add("-i")
                    addAll(sourceFiles)
                },
            )
        }
    }
}
