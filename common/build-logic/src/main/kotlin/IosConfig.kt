//
// © 2026-present https://github.com/cengiz-pz
//

import java.io.File
import java.util.Properties

/**
 * Immutable value object holding every setting from `ios/config/ios.properties`.
 *
 * Obtain an instance via [Project.loadIosConfig] (defined in `ProjectExtensions.kt`),
 * which is available in any project build script that applies `id("base-conventions")`:
 *
 * ```kotlin
 * plugins { id("base-conventions") }
 *
 * val iosConfig = loadIosConfig()
 * println(iosConfig.platformVersion)   // "14.3"
 * println(iosConfig.swiftVersion)      // "5.9"
 * println(iosConfig.frameworks)        // "Foundation.framework,Network.framework"
 * ```
 *
 * ## Source file
 *
 * `ios/config/ios.properties` at the repository root (one level above `gradle/`).
 *
 * ## Properties reference
 *
 * | Property key          | Kotlin field            | Description                                             |
 * |-----------------------|-------------------------|---------------------------------------------------------|
 * | `platform_version`    | [platformVersion]       | Minimum iOS deployment target, e.g. `14.3`              |
 * | `swift_version`       | [swiftVersion]          | Swift language version used by Xcode, e.g. `5.9`        |
 * | `frameworks`          | [frameworks]            | System framework names to link, e.g. `["Foundation.framework"]` |
 * | `embedded_frameworks` | [embeddedFrameworks]    | Frameworks to embed in the app bundle (may be empty)    |
 * | `flags`               | [linkerFlags]           | Extra linker flags, e.g. `["-ObjC"]`                   |
 *
 * Comma-separated properties (`frameworks`, `embedded_frameworks`, `flags`) are
 * split into [List]s at load time - blank entries are dropped - so consumers never
 * need to parse delimiters themselves.
 */
data class IosConfig(
    /** Minimum iOS deployment target, e.g. `14.3`. */
    val platformVersion: String,
    /**
     * Swift language version required by the plugin, e.g. `5.9`.
     *
     * May be an empty string if `swift_version` is not set in `ios.properties`.
     * Use [Project.loadIosConfig] together with the `validateSwiftVersion` Gradle task
     * to surface a clear error at build time rather than relying on a non-null guarantee here.
     */
    val swiftVersion: String,
    /**
     * System frameworks to link against, e.g. `["Foundation.framework", "Network.framework"]`.
     *
     * Parsed from the comma-separated `frameworks` key in `ios.properties`.
     * Empty when no frameworks are specified.
     */
    val frameworks: List<String>,
    /**
     * Frameworks to embed in the app bundle.
     *
     * Parsed from the comma-separated `embedded_frameworks` key in `ios.properties`.
     * Empty when no frameworks need embedding.
     */
    val embeddedFrameworks: List<String>,
    /**
     * Extra linker flags passed to `xcodebuild`, e.g. `["-ObjC"]`.
     *
     * Parsed from the comma-separated `flags` key in `ios.properties`.
     * Empty when no extra flags are required.
     */
    val linkerFlags: List<String>,
) {
    companion object {
        /**
         * Loads an [IosConfig] from `ios/config/ios.properties`.
         *
         * @param gradleRootDir `rootProject.rootDir` - the `gradle/` directory.
         *   The repository root is resolved as `gradleRootDir.parentFile`, and the
         *   properties file is expected at `<repoRoot>/ios/config/ios.properties`.
         */
        fun load(gradleRootDir: File): IosConfig {
            val file = gradleRootDir.parentFile.resolve("ios/config/ios.properties")
            check(file.exists()) { "iOS properties file not found: ${file.absolutePath}" }
            val props = Properties().also { it.load(file.inputStream()) }
            return IosConfig(
                platformVersion    = props.require("platform_version"),
                swiftVersion       = props.getProperty("swift_version")?.trim() ?: "",
                frameworks         = props.splitList("frameworks"),
                embeddedFrameworks = props.splitList("embedded_frameworks"),
                linkerFlags        = props.splitList("flags"),
            )
        }
    }
}

private fun Properties.require(key: String): String =
    getProperty(key)?.trim()
        ?: error("Required key '$key' is missing from ios/config/ios.properties.")

/** Splits a comma-separated property value into a trimmed, non-blank list. */
private fun Properties.splitList(key: String): List<String> =
    getProperty(key)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
