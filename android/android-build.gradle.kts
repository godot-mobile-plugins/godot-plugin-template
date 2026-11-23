//
// © 2024-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

import com.android.build.gradle.internal.api.LibraryVariantOutputImpl
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpxTask
import org.w3c.dom.Element
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("base-conventions")
    alias(libs.plugins.android.library)
    alias(libs.plugins.undercouch.download)
    alias(libs.plugins.openrewrite)
    alias(libs.plugins.node)
}

// -- Load config data classes --------------------------------------------------

val pluginConfig = loadPluginConfig()
val godotConfig = loadGodotConfig()

// -- Test-report helper functions ----------------------------------------------
//
// Reading coverage is Android-specific and lives here. The JUnit XML reader
// (parseSuiteResults, safeXmlBuilder) is shared with the integration tests in
// build-logic (JUnitResults.kt). The SuiteResult/CoverageRow types, the
// TEST RESULTS / CODE COVERAGE printers and failBuildIfTestsFailed() are shared
// with iOS in build-logic (TestReporting.kt).

/**
 * Parses the top-level `<counter>` elements from a JaCoCo XML report, which
 * represent project-wide totals. Returns one [CoverageRow] for each of the five
 * counters we display: INSTRUCTION, BRANCH, LINE, METHOD, CLASS.
 */
fun parseCoverage(reportXml: File): List<CoverageRow> {
    if (!reportXml.exists()) return emptyList()
    return runCatching {
        val root = safeXmlBuilder().parse(reportXml).documentElement
        val labels =
            linkedMapOf(
                "INSTRUCTION" to "Instructions",
                "BRANCH" to "Branches",
                "LINE" to "Lines",
                "METHOD" to "Methods",
                "CLASS" to "Classes",
            )
        val order = labels.keys.toList()
        // Collect only direct <counter> children of the root <report> element;
        // nested counters belong to individual packages/classes and are skipped.
        (0 until root.childNodes.length)
            .mapNotNull { root.childNodes.item(it) as? Element }
            .filter { it.tagName == "counter" }
            .mapNotNull { el ->
                val type = el.getAttribute("type").takeIf { it in order } ?: return@mapNotNull null
                val covered = el.getAttribute("covered").toLongOrNull() ?: 0L
                val missed = el.getAttribute("missed").toLongOrNull() ?: 0L
                type to CoverageRow(labels.getValue(type), covered, covered + missed)
            }.sortedBy { order.indexOf(it.first) }
            .map { it.second }
    }.getOrDefault(emptyList())
}

// -- OpenRewrite ---------------------------------------------------------------

configure<org.openrewrite.gradle.RewriteExtension> {
    activeRecipe(
        "org.openrewrite.java.format.TabsAndIndents",
        "org.openrewrite.java.RemoveUnusedImports",
        "org.openrewrite.java.format.AutoFormat",
        "org.openrewrite.java.format.EmptyNewlineAtEndOfFile",
        "org.openrewrite.java.format.RemoveTrailingWhitespace",
        "org.openrewrite.staticanalysis.NeedBraces",
        "org.openrewrite.staticanalysis.WhileInsteadOfFor",
    )
    activeStyle("org.godotengine.plugin.JavaStyle")
    configFile = projectDir.resolve("config/rewrite.yml")
    exclusion("**/*.kt", "**/*.kts")
}

// -- Android configuration -----------------------------------------------------

base {
    archivesName.set(pluginConfig.pluginName)
}

android {
    namespace = pluginConfig.pluginPackageName
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        manifestPlaceholders["godotPluginName"] = pluginConfig.pluginName
        manifestPlaceholders["godotPluginPackageName"] = pluginConfig.pluginPackageName
        buildConfigField("String", "GODOT_PLUGIN_NAME", "\"${pluginConfig.pluginName}\"")

        // Packaged into the AAR as proguard.txt so that consuming Godot 4.8+ exports built
        // with the "gradle_build/minification" option keep what this plugin needs.
        consumerProguardFiles("config/consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildTypes {
        // enableUnitTestCoverage instruments testDebugUnitTest with the JaCoCo
        // agent and wires up the createDebugUnitTestCoverageReport task, which
        // produces the XML report that printTestSummary reads.
        debug {
            enableUnitTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            // Return 0 / false / null for every un-mocked Android framework call
            // (Log.d, Log.e, Log.i, Settings.Secure, etc.) instead of throwing.
            isReturnDefaultValues = true
        }
    }
}

node {
    download = true
    version =
        libs.versions.node.env
            .get()
}

// -- Dependencies --------------------------------------------------------------

val runtimeDependencies =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .run {
            libraryAliases
                .filter { it.startsWith("runtime.") }
                .map { findLibrary(it).get().get() }
        }

val testDependencies =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .run {
            libraryAliases
                .filter { it.startsWith("test.") && !it.startsWith("test.runtime.") }
                .map { findLibrary(it).get().get() }
        }

val testRuntimeOnlyDependencies =
    extensions.getByType<VersionCatalogsExtension>().named("libs").run {
        libraryAliases
            .filter { it.startsWith("test.runtime.") }
            .map { findLibrary(it).get().get() }
    }

val artifactType = Attribute.of("artifactType", String::class.java)

dependencies {
    "rewrite"(libs.style.rewrite.static.analysis)
    implementation("godot:godot-lib:${godotConfig.godotVersion}.${godotConfig.godotReleaseType}@aar")

    println("DEBUG: Runtime Dependencies")
    runtimeDependencies.forEach {
        println("DEBUG: Adding to runtime: $it")
        implementation(it)
    }

    println("DEBUG: Test Dependencies")
    testDependencies.forEach { dependency ->
        println("DEBUG: Adding to test: $dependency")
        testImplementation(dependency)
    }

    println("DEBUG: Test Runtime Only Dependencies")
    testRuntimeOnlyDependencies.forEach {
        println("DEBUG: Adding to testRuntimeOnly: $it")
        testRuntimeOnly(it)
    }

    attributesSchema {
        attribute(artifactType) {
            disambiguationRules.add(JarFirstRule::class.java)
        }
    }
}

// Helper class to prioritize JARs when multiple variants match
abstract class JarFirstRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        if (details.candidateValues.contains("jar")) {
            details.closestMatch("jar")
        } else if (details.candidateValues.contains("android-classes-jar")) {
            details.closestMatch("android-classes-jar")
        }
    }
}

// -- Helpers -------------------------------------------------------------------

fun buildTimestamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

/** Registers a Copy task that assembles one build variant of the Android plugin. */
fun TaskContainerScope.registerAndroidBuildVariant(variant: String) {
    val taskName = "buildAndroid${variant.replaceFirstChar { it.uppercase() }}"
    val pluginDir: String by project.extra
    val repositoryRootDir: String by project.extra

    register<Copy>(taskName) {
        description = "Copies the generated GDScript and $variant AAR binary to the plugin directory"
        group = "build"

        dependsOn(
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("copyAssets"),
            project(":android").tasks.named("assemble${variant.replaceFirstChar { it.uppercase() }}"),
        )

        inputs.files(project(":addon").tasks.named("generateGDScript").map { it.outputs.files })
        inputs.files(project(":addon").tasks.named("copyAssets").map { it.outputs.files })
        inputs.files(
            project(":android")
                .tasks
                .named("assemble${variant.replaceFirstChar { it.uppercase() }}")
                .map { it.outputs.files },
        )

        into("$pluginDir/android")

        from("$repositoryRootDir/addon/build/output") {
            include("addons/${pluginConfig.pluginName}/**")
            include("addons/GMPShared/**")
        }

        from("$projectDir/build/outputs/aar") {
            include("${pluginConfig.pluginName}-$variant.aar")
            into("addons/${pluginConfig.pluginName}/bin/$variant")
        }

        doLast { println("Android $variant build completed at: ${buildTimestamp()}") }

        outputs.dir("$pluginDir/android")
    }
}

// -- Tasks ---------------------------------------------------------------------

tasks {
    val pluginDir: String by project.extra
    val repositoryRootDir: String by project.extra
    val archiveDir: String by project.extra
    val demoDir: String by project.extra

    registerAndroidBuildVariant("debug")
    registerAndroidBuildVariant("release")

    register("buildAndroid") {
        description = "Builds both debug and release"
        group = "build"
        dependsOn("buildAndroidDebug", "buildAndroidRelease")
    }

    register<Zip>("createAndroidArchive") {
        dependsOn("buildAndroidDebug", "buildAndroidRelease")

        group = "archive"
        archiveFileName.set("${pluginConfig.pluginName}-Android-v${pluginConfig.pluginVersion}.zip")
        destinationDirectory.set(layout.projectDirectory.dir(archiveDir))

        into("res") {
            from(layout.projectDirectory.dir("$pluginDir/android")) { includeEmptyDirs = false }
            from("$repositoryRootDir/LICENSE") {
                into("addons/${pluginConfig.pluginName}")
            }
        }

        doLast { println("Android zip archive created at: ${archiveFile.get().asFile.path}") }
    }

    register<Copy>("installToDemoAndroid") {
        description = "Copies the assembled Android plugin to demo application's addons directory"
        group = "install"

        dependsOn(
            project(":addon").tasks.named("generateGDScript"),
            project(":addon").tasks.named("copyAssets"),
            "buildAndroidDebug",
        )

        inputs.files(project.tasks.named("buildAndroidDebug").map { it.outputs.files })

        destinationDir = file(demoDir)
        duplicatesStrategy = DuplicatesStrategy.WARN

        into(".") { from("$pluginDir/android") }

        outputs.dir(destinationDir)
    }

    register<Delete>("uninstallAndroid") {
        description = "Removes plugin files from demo app (preserves .uid and .import files)"
        group = "uninstall"
        delete(
            fileTree("$demoDir/addons/${pluginConfig.pluginName}") {
                include("**/*")
                exclude("**/*.uid")
                exclude("**/*.import")
            },
        )
    }

    register<NpmTask>("installPrettier") {
        group = "setup"
        args.set(listOf("install", "--save-dev", "prettier", "@prettier/plugin-xml"))
    }

    register<NpxTask>("checkXmlFormat") {
        dependsOn("installPrettier")
        command.set("prettier")
        args.set(
            listOf(
                "--config",
                "../.github/config/prettier.xml.json",
                "--parser",
                "xml",
                "--check",
                "src/**/*.xml",
            ),
        )
    }

    register<NpxTask>("formatXml") {
        dependsOn("installPrettier")
        command.set("prettier")
        args.set(
            listOf(
                "--config",
                "../.github/config/prettier.xml.json",
                "--parser",
                "xml",
                "--write",
                "src/**/*.xml",
            ),
        )
    }

    register<de.undercouch.gradle.tasks.download.Download>("downloadCheckstyleJar") {
        group = "setup"
        val checkstyleVersion = libs.versions.checkstyle.get()
        val destFile = file("${gradle.extra["libDir"]}/checkstyle-$checkstyleVersion-all.jar")

        inputs.property("checkstyleVersion", checkstyleVersion)
        outputs.file(destFile)

        src(
            "https://github.com/checkstyle/checkstyle/releases/download/" +
                "checkstyle-$checkstyleVersion/checkstyle-$checkstyleVersion-all.jar",
        )
        dest(destFile)
        overwrite(false)
    }

    register<JavaExec>("checkJavaFormat") {
        description = "Runs Checkstyle on all Java sources under \$projectDir/src"
        group = "verification"

        dependsOn("downloadCheckstyleJar")

        val checkstyleVersion = libs.versions.checkstyle.get()
        val jarFile = file("${gradle.extra["libDir"]}/checkstyle-$checkstyleVersion-all.jar")

        classpath = files(jarFile)
        mainClass.set("com.puppycrawl.tools.checkstyle.Main")
        args =
            listOf(
                "-c",
                rootProject.file("../.github/config/checkstyle.xml").absolutePath,
                file("$projectDir/src").absolutePath,
            )

        inputs.dir("$projectDir/src")
        inputs.file(rootProject.file("../.github/config/checkstyle.xml"))
        outputs.upToDateWhen { false }
    }

    register<de.undercouch.gradle.tasks.download.Download>("downloadGodotAar") {
        group = "setup"
        val destFile = file("${gradle.extra["libDir"]}/${godotConfig.godotAarFile}")

        inputs.property("godotAarUrl", godotConfig.godotAarUrl)
        outputs.file(destFile)

        src(godotConfig.godotAarUrl)
        dest(destFile)
        overwrite(false)
    }

    val kotlinSourceFiles =
        fileTree("src") {
            include("**/*.kt")
            exclude("**/*Plugin.kt")
        }.files
            .map { it.relativeTo(projectDir).path }
            .sorted()

    register<Exec>("checkKotlinFormat") {
        description = "Checks ktlint compliance of Kotlin source files under \$projectDir/src"
        group = "verification"
        workingDir = projectDir

        doFirst {
            if (kotlinSourceFiles.isNotEmpty()) {
                commandLine(listOf("ktlint") + kotlinSourceFiles)
            } else {
                logger.lifecycle("checkKotlinFormat: No source files found to format.")

                // Set commandLine to something harmless so Exec doesn't fail
                // if it expects a command to be set
                commandLine("true")
            }
        }
    }

    register<Exec>("formatKotlinSource") {
        description = "Formats Kotlin source files under $projectDir/src in-place using ktlint"
        group = "formatting"
        workingDir = projectDir

        doFirst {
            if (kotlinSourceFiles.isNotEmpty()) {
                commandLine(listOf("ktlint", "--format") + kotlinSourceFiles)
            } else {
                logger.lifecycle("formatKotlinSource: No source files found to format.")

                // Set commandLine to something harmless so Exec doesn't fail
                // if it expects a command to be set
                commandLine("true")
            }
        }
    }

    register("checkAndroidFormat") {
        description = "Validates format in all source code"
        group = "verification"
        dependsOn(
            project(":android").tasks.named("checkJavaFormat"),
            project(":android").tasks.named("checkKotlinFormat"),
            project(":android").tasks.named("checkXmlFormat"),
        )
    }

    register("applyAndroidFormat") {
        description = "Formats all source code"
        group = "formatting"
        dependsOn(
            project(":android").tasks.named("rewriteRun"),
            project(":android").tasks.named("formatKotlinSource"),
            project(":android").tasks.named("formatXml"),
        )
    }

    named("preBuild") {
        dependsOn("downloadGodotAar")
    }

    // -- Test summary ----------------------------------------------------------

    register("printTestSummary") {
        group = "verification"
        description = "Runs unit tests + coverage and prints a formatted summary to the console"

        // Explicitly depend on both tasks so the full pipeline always runs
        dependsOn("testDebugUnitTest")
        dependsOn("createDebugUnitTestCoverageReport")

        val buildDirProvider = project.layout.buildDirectory // Capture at configuration time
        doLast {
            val buildDir = buildDirProvider.get().asFile
            val resultsDir = File(buildDir, "test-results/testDebugUnitTest")
            val coverageXml = File(buildDir, "reports/coverage/test/debug/report.xml")
            val coverageDir = coverageXml.parentFile

            val suites = parseSuiteResults(resultsDir)
            val coverageRows = parseCoverage(coverageXml)

            printTestResultsTable(
                suites,
                emptyNotice =
                    listOf(
                        "No test results found.",
                        "Expected location: build/test-results/testDebugUnitTest/",
                    ),
            )
            printCoverageTable(
                nameHeader = "Metric",
                rows = coverageRows,
                headerNotes =
                    if (coverageDir.exists()) {
                        listOf("HTML report: ${File(coverageDir, "index.html").absolutePath}")
                    } else {
                        emptyList()
                    },
                emptyNotice =
                    listOf(
                        "Coverage data not available.",
                        "Expected location: build/reports/coverage/test/debug/report.xml",
                    ),
            )

            failBuildIfTestsFailed(suites, dash = "-")
        }
    }
}

// -- Task wiring ---------------------------------------------------------------

tasks.withType<Test> {
    useJUnitPlatform()
    // Let the full pipeline (coverage + printTestSummary) run even if tests fail.
    // printTestSummary will throw a GradleException if any tests failed.
    ignoreFailures = true
}

// afterEvaluate guarantees the test task exists when configured
afterEvaluate {
    tasks.named<Test>("testDebugUnitTest") {
        // This tells Gradle: coverage report MUST run AFTER the test task finishes
        finalizedBy("createDebugUnitTestCoverageReport")
    }
}
