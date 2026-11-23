//
// © 2026-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

import java.io.File

// -- Godot editor download ------------------------------------------------------
//
// Describes the Godot editor build used to run the headless integration tests
// (:addon:downloadGodotEditor / :addon:runIntegrationTests). The version comes
// from config/godot.properties via [GodotConfig]; nothing here is hard-coded to a
// particular Godot release.

/**
 * Host platform of the machine running the build, expressed as the suffix used in
 * Godot editor release archive names (`Godot_v<version>-<releaseType>_<suffix>.zip`).
 */
enum class GodotEditorPlatform(
    val archiveSuffix: String,
) {
    LINUX_X86_64("linux.x86_64"),
    LINUX_ARM64("linux.arm64"),
    MACOS_ARM64("macos.arm64"),
    MACOS_X86_64("macos.x86_64"),
    ;

    val isMacOS: Boolean
        get() = this == MACOS_ARM64 || this == MACOS_X86_64

    companion object {
        /** Detects the current host. Fails with a clear message on unsupported hosts (e.g. Windows). */
        fun current(): GodotEditorPlatform {
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch").lowercase()
            val isArm = arch == "aarch64" || arch == "arm64"
            return when {
                os.contains("mac") -> {
                    if (isArm) MACOS_ARM64 else MACOS_X86_64
                }

                os.contains("linux") -> {
                    if (isArm) LINUX_ARM64 else LINUX_X86_64
                }

                else -> {
                    error(
                        "Godot integration tests support macOS and Linux only " +
                            "(detected os.name='$os', os.arch='$arch').",
                    )
                }
            }
        }
    }
}

/** `<version>-<releaseType>`, e.g. `4.8-beta4`. Used as the release tag and cache key. */
val GodotConfig.godotVersionString: String
    get() = "$godotVersion-$godotReleaseType"

/** Editor archive name for [platform], e.g. `Godot_v4.8-beta4_linux.x86_64.zip`. */
fun GodotConfig.godotEditorZip(platform: GodotEditorPlatform): String =
    "Godot_v${godotVersionString}_${platform.archiveSuffix}.zip"

/**
 * Download URL for the Godot editor archive on GitHub Releases.
 *
 * Pattern:
 * ```
 * https://github.com/godotengine/godot-builds/releases/download/<version>-<releaseType>/
 *     Godot_v<version>-<releaseType>_<platform>.zip
 * ```
 */
fun GodotConfig.godotEditorUrl(platform: GodotEditorPlatform): String =
    "https://github.com/godotengine/godot-builds/releases/download/" +
        "$godotVersionString/${godotEditorZip(platform)}"

/**
 * Locates the Godot editor executable inside an extracted editor archive.
 *
 * macOS archives contain an application bundle (`<name>.app/Contents/MacOS/Godot`);
 * Linux archives contain a single `Godot_v…` executable. Returns `null` when no
 * executable is found.
 */
fun findGodotEditorExecutable(
    editorDir: File,
    platform: GodotEditorPlatform,
): File? {
    if (!editorDir.isDirectory) return null
    return if (platform.isMacOS) {
        editorDir
            .listFiles { f -> f.isDirectory && f.name.endsWith(".app") }
            ?.map { File(it, "Contents/MacOS/Godot") }
            ?.firstOrNull { it.isFile }
    } else {
        editorDir
            .listFiles { f -> f.isFile && f.name.startsWith("Godot_v") && !f.name.endsWith(".zip") }
            ?.firstOrNull()
    }
}
