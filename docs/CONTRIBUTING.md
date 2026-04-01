![Android Build](https://github.com/godot-mobile-plugins/godot-plugin-template/actions/workflows/android-build.yml/badge.svg)
![iOS Build](https://github.com/godot-mobile-plugins/godot-plugin-template/actions/workflows/ios-build.yml/badge.svg)

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="28"> Contributing

Thank you for your interest in contributing to the Godot PluginTemplate Plugin! This guide will help you understand the project structure, build processes, and development workflows.

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Table of Contents

- [Project Structure](#-project-structure)
- [Build System Architecture](#-build-system-architecture)
- [Prerequisites](#-prerequisites)
- [Configuration](#-configuration)
- [Development Workflow](#-development-workflow)
- [Building](#-building)
- [Code Formatting](#-code-formatting)
- [Testing](#-testing)
- [Creating Releases](#-creating-releases)
- [Installation](#-installation)
- [Troubleshooting](#-troubleshooting)
- [Contributing Guidelines](#-contributing-guidelines)

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Project structure

```text
.
├── addon/                               # GDScript interface module
│   ├── addon-build.gradle.kts             # Gradle build configuration for addon module
│   ├── ?.gradle.kts                       # Any extra addon-specific Gradle configuration (configured in
│   │                                      # addon/config/addon-build.properties) for the plugin goes here
│   ├── build/
│   │   └── output/                        # Generated GDScript code
│   │
│   ├── config/
│   │   └── addon-build.properties         # Gradle build customization for addon module
│   │
│   └── src/
│       ├── main                           # Main GDScript templates
│       └── shared                         # GDScript templates in common with other plugins, if any
│
├── android/                             # Android platform module
│   ├── android-build.gradle.kts           # Android build configuration
│   ├── ?.gradle.kts                       # Any extra Android-specific Gradle configuration (configured in
│   │                                      # android/config/android-build.properties) for the plugin goes here
│   │
│   ├── build/
│   │   └── outputs/                       # Generated Android AAR files
│   │
│   ├── config/
│   │   └── android-build.properties       # Gradle build customization for android module
│   │
│   ├── libs/                              # Godot library for Android (default location; configurable via local.properties)
│   └── src/main/                          # Android source code
│
├── common/                              # Gradle root - shared build configuration
│   ├── build.gradle.kts                   # Root build configuration
│   ├── ?.gradle.kts                       # Any extra Gradle configuration (configured in
│   │                                      # common/config/build.properties) for the plugin goes here
│   │
│   ├── gradle.properties                  # Gradle properties
│   ├── local.properties                   # Local machine config (gitignored)
│   ├── settings.gradle.kts                # Gradle settings
│   ├── build/
│   │   ├── archive/                       # Generated archives
│   │   ├── plugin/                        # Built plugin files
│   │   └── reports/                       # Build reports
│   │
│   ├── build-logic/                       # Convention plugin (precompiled script plugins)
│   │   ├── build.gradle.kts
│   │   ├── settings.gradle.kts
│   │   └── src/main/kotlin/
│   │       ├── base-conventions.gradle.kts  # Core convention plugin - applied by every module
│   │       ├── BuildConfig.kt               # Reads build.properties + per-module *-build.properties
│   │       ├── GodotConfig.kt               # Reads godot.properties
│   │       ├── IosConfig.kt                 # Reads ios/config/ios.properties
│   │       ├── PluginConfig.kt              # Reads plugin.properties
│   │       ├── ProjectExtensions.kt         # loadPluginConfig(), loadGodotConfig(), loadIosConfig(), loadBuildConfig()
│   │       └── SpmDependency.kt             # Data class for spm_dependencies.json entries
│   │
│   ├── config/
│   │   ├── build.properties               # Build-related property configuration & customization
│   │   ├── godot.properties               # Godot version configuration
│   │   └── plugin.properties              # Plugin configuration
│   │
│   └── gradle/                            # Gradle wrapper and version catalogs
│       └── libs.versions.toml             # Dependencies and versions
│
├── demo/                                # Demo application
│   ├── addons/                            # Installed plugin files
│   ├── ios/                               # iOS-specific demo files
│   └── *.gd                               # Demo app scripts
│
├── ios/                                 # iOS platform module
│   ├── ios-build.gradle.kts               # iOS build configuration
│   ├── ?.gradle.kts                       # Any extra iOS-specific Gradle configuration (configured in
│   │                                      # ios/config/ios-build.properties) for the plugin goes here
│   │
│   ├── src/                               # iOS platform code
│   ├── plugin.xcodeproj/                  # Xcode project
│   ├── build/                             # iOS build outputs
│   │
│   ├── config/
│   │   ├── ios.properties                 # iOS configuration
│   │   ├── ios-build.properties           # Gradle build customization for ios module
│   │   ├── spm_dependencies.json          # SPM dependency configuration
│   │   └── *.gdip                         # Godot iOS plugin config
│   │
│   └── godot/                             # Downloaded Godot source (default location; configurable via local.properties)
│
├── script/                              # Build and utility scripts
│   ├── build.sh                           # Main build script
│   ├── build_android.sh                   # Android build script
│   ├── build_ios.sh                       # iOS build script
│   ├── install.sh                         # Plugin installation script
│   ├── run_gradle_task.sh                 # Gradle task runner
│   ├── get_config_property.sh             # Configuration reader
│   └── spm_manager.rb                     # Ruby script for managing SPM dependencies in Xcode project
│
├── docs/                                # Documentation
│
└── release/                             # Final release archives
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Build System Architecture

The build system is centred on a **convention plugin** living in `common/build-logic/`. This is an [included build](https://docs.gradle.org/current/userguide/composite_builds.html) whose compiled output is available on the classpath of every module build script that declares `plugins { id("base-conventions") }`.

### Config Data Classes

All plugin, Godot, iOS, and build settings are loaded once into typed immutable data classes. Project build scripts access them through `Project` extension functions - the same call pattern used throughout the Kotlin ecosystem:

| Extension function   | Data class    | Source file                                    |
|----------------------|---------------|------------------------------------------------|
| `loadPluginConfig()` | `PluginConfig` | `common/config/plugin.properties`             |
| `loadGodotConfig()`  | `GodotConfig`  | `common/config/godot.properties`              |
| `loadIosConfig()`    | `IosConfig`    | `ios/config/ios.properties` + `ios/config/spm_dependencies.json` |
| `loadBuildConfig()`  | `BuildConfig`  | `common/config/build.properties` + all four `*-build.properties` |

Usage in any module build script:

```kotlin
plugins { id("base-conventions") }

val pluginConfig = loadPluginConfig()
val godotConfig  = loadGodotConfig()
val iosConfig    = loadIosConfig()

println(pluginConfig.pluginName)        // "PluginTemplatePlugin"
println(godotConfig.godotAarUrl)        // full GitHub download URL
println(iosConfig.frameworks)           // List<String> - already parsed
```

### `base-conventions` Convention Plugin

Applying `id("base-conventions")` in a module build script:

1. Loads all four config data classes.
2. Bridges every scalar config value onto `project.extra` (for compatibility with `apply(from = …)` scripts that cannot reference build-logic types directly).
3. Sets shared directory-layout extras (`pluginDir`, `repositoryRootDir`, `archiveDir`, `demoDir`).
4. Applies the per-module user-defined extra properties and extra Gradle scripts from `BuildConfig`, scoped by `project.path` - so `:android` only receives `BuildConfig.androidExtraProperties` / `androidExtraGradle`, `:ios` only receives the iOS equivalents, and so on.

### `IosConfig` List Fields

The `frameworks`, `embeddedFrameworks`, and `linkerFlags` fields on `IosConfig` are `List<String>`. The comma-separated values in `ios/config/ios.properties` are split and trimmed at load time, so consumers never need to parse delimiters:

```kotlin
val iosConfig = loadIosConfig()
iosConfig.frameworks         // ["Foundation.framework", "Network.framework"]
iosConfig.embeddedFrameworks // [] when empty
iosConfig.linkerFlags        // ["-ObjC"]
```

### `IosConfig` SPM Dependencies

`IosConfig` also exposes a `spmDependencies: List<SpmDependency>` field, decoded at load time from `ios/config/spm_dependencies.json`. Each `SpmDependency` entry carries three fields:

| Field      | Type           | Description                                      |
|------------|----------------|--------------------------------------------------|
| `url`      | `String`       | Git repository URL of the Swift package          |
| `version`  | `String`       | Minimum version requirement                      |
| `products` | `List<String>` | SPM product names to link against                |

```kotlin
val iosConfig = loadIosConfig()
iosConfig.spmDependencies   // [SpmDependency(url="https://...", version="1.2.3", products=["ProductA"])]
```

`base-conventions` bridges this list onto `project.extra["iosSpmDependencies"]` so it is accessible from any task lambda that cannot reference `IosConfig` by type directly.

The `addon-build.gradle.kts` `generateGDScript` and `generateSharedGDScript` tasks expose the list via the `@spmDependencies@` token. Each dependency is rendered as a GDScript dictionary literal using [StringName](https://docs.godotengine.org/en/stable/classes/class_stringname.html) key syntax (`&"key"`), and multiple entries are joined with `, ` — without outer brackets, because they are supplied by the surrounding GDScript constant:

```gdscript
# Template source:
const SPM_DEPENDENCIES: Array = [ @spmDependencies@ ]

# After token replacement (two dependencies):
const SPM_DEPENDENCIES: Array = [ {&"url": "https://github.com/owner/repo", &"version": "1.2.3", &"products": ["ProductA", "ProductB"]}, {&"url": "https://github.com/other/pkg", &"version": "2.0.0", &"products": ["ProductC"]} ]
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Prerequisites

### General Requirements
- **Git** - For version control
- **Bash** - For running build scripts (macOS/Linux native, Windows via WSL or Git Bash)

### Android Development
- **Java Development Kit (JDK)** - Version 17 or higher
- **Android SDK** - With the following components:
  - Android SDK Platform Tools
  - Android SDK Build Tools (version specified in gradle)
  - Android SDK Platform (API level specified in gradle)
  - Android NDK (if building native code)

Your Android SDK directory should contain:

```text
android-sdk/
├── build-tools/
├── cmdline-tools/
├── licenses/
├── ndk/
├── platform-tools/
├── platforms/
└── tools/
```

- Create `local.properties` file inside `./common` directory that locates the Android SDK installation directory

Sample `local.properties` on Windows:
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

Sample `local.properties` on Unix-like command-line:
```properties
sdk.dir=/usr/lib/android-sdk
```

Optionally, set `godot.dir` to use a Godot source tree at a custom location instead of the default `ios/godot/`:
```properties
godot.dir=/path/to/your/shared/godot
```

### iOS Development (macOS only)
- **Xcode** - Latest stable version recommended
- **Xcode Command Line Tools** - Install via: `xcode-select --install`
- **Ruby** - Required for SPM dependency management via `spm_manager.rb` (macOS system Ruby is sufficient)
- **xcodeproj gem** - Installed automatically by the build system if missing, or manually via: `gem install xcodeproj --user-install`

### Developer Tools (Optional - required for format checking)

These tools are needed when running `checkFormat` or `applyFormat` tasks:

- **ktlint** - Kotlin/KTS formatter: `brew install ktlint`
- **shellcheck** - Shell script linter: `brew install shellcheck`
- **editorconfig-checker** - EditorConfig compliance: `brew install editorconfig-checker`
- **clang-format** - ObjC/C++ formatter: `brew install clang-format` (iOS only)
- **swiftlint** - Swift linter/formatter: `brew install swiftlint` (iOS only)
- **gdformat** - GDScript formatter: install via the Godot toolchain

### Verifying Prerequisites

```bash
# Check Java version
java -version

# macOS/iOS only
xcodebuild -version
ruby --version
gem list xcodeproj
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Configuration

The build files are static and shared across all GMP plugins. Any plugin-specific build customization is handled through the following configuration files:

```text
.
├── addon/
│   ├── ?.gradle.kts                       # Any extra addon-specific Gradle configuration (configured in
│   │                                      # addon/config/addon-build.properties) for the plugin goes here
│   └── config/
│       └── addon-build.properties         # Gradle build customization for addon module
│
├── android/
│   ├── android-build.gradle.kts           # Android build configuration
│   ├── ?.gradle.kts                       # Any extra Android-specific Gradle configuration (configured in
│   │                                      # android/config/android-build.properties) for the plugin goes here
│   └── config/
│       └── android-build.properties       # Gradle build customization for android module
│
├── common/
│   ├── config/
│   │   ├── build.properties               # Build-related property configuration & customization
│   │   ├── godot.properties               # Godot version configuration
│   │   └── plugin.properties              # Plugin configuration
│   │
│   └── gradle/
│       └── libs.versions.toml             # Android dependencies and versions
│
└── ios/
    └── config/
        ├── ios.properties                 # iOS configuration
        ├── ios-build.properties           # Gradle build customization for ios module
        └── spm_dependencies.json          # SPM dependency configuration
```

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> Common Configuration

The `common/config/plugin.properties` file contains core plugin settings:

```properties
# Plugin identification
pluginNodeName=...                # Name of the plugin node in Godot (e.g. MyPlugin)
pluginModuleName=...              # Snake-case module name for native symbols (e.g. my_plugin)
pluginPackage=...                 # Fully-qualified Java/Kotlin package (e.g. org.godotengine.plugin.myplugin)
pluginVersion=1.0                 # Plugin version
```

The `common/config/godot.properties` file contains core Godot version settings:

```properties
# Godot configuration
godotVersion=4.6                  # Target Godot version
godotReleaseType=stable           # Release type: stable, dev6, beta3, rc1, etc.
```

The `common/config/build.properties` file contains Gradle build-related property settings. The `gradleProjectName` key is required. Extra properties and Gradle scripts that apply only to the **root project** use a `root.` prefix:

```properties
gradleProjectName=godot-*-plugin

# Extra properties set on the root project only
root.extra.anotherProperty=property value

# Extra Gradle scripts applied to the root project only
root.gradle.another=another.gradle.kts
```

Per-module extra properties and scripts are configured in each module's own `*-build.properties` file (see [Build Customization](#-build-customization) below).

**Key Properties:**
- `pluginNodeName` - The name of the main plugin node used in Godot
- `pluginVersion` - Semantic version for releases
- `godotVersion` - Must match your target Godot version
- `godotReleaseType` - Determines which Godot binary to download

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> Build Customization

Plugin-specific build customizations can be configured in the following files.

`common/config/build.properties` for root-project customizations. The `root.` prefix scopes each entry to the root project only:

```properties
# Set plugin-specific extra properties on the root project
#root.extra.myProperty=value

# Configure plugin-specific Gradle scripts for the root project
#root.gradle.extraGradle=extra.gradle.kts
```

`addon/config/addon-build.properties` for addon-module build customizations:

```properties
# Set plugin-specific extra properties for addon module
#extra.myProperty=value

# Configure plugin-specific Gradle scripts for addon module
#gradle.extraGradle=extra.gradle.kts
```

`android/config/android-build.properties` for android-module build customizations:

```properties
# Set plugin-specific extra properties for android module
#extra.myProperty=value

# Configure plugin-specific Gradle scripts for android module
#gradle.extraGradle=extra.gradle.kts
```

`ios/config/ios-build.properties` for ios-module build customizations:

```properties
# Set plugin-specific extra properties for ios module
#extra.myProperty=value

# Configure plugin-specific Gradle scripts for ios module
#gradle.extraGradle=extra.gradle.kts
```

Each `extra.*` key sets a Gradle extra property on the corresponding module's project. Each `gradle.*` key applies the named Gradle script file to that module via `project.apply(from = …)`. Extra scripts are resolved relative to the repository root.

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> Local Configuration

Create `common/local.properties` to configure machine-specific paths. This file is gitignored and must be created locally.

#### Android SDK Location

```properties
# Windows
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk

# macOS/Linux
sdk.dir=/Users/YourUsername/Library/Android/sdk

# Linux (alternate)
sdk.dir=/usr/lib/android-sdk
```

#### Godot Directory (iOS - optional)

By default, the iOS build scripts download and use the Godot source from `ios/godot/` inside the project. If you want to use a Godot source tree located elsewhere on your machine (e.g. to share it across multiple plugin projects), set `godot.dir` in `local.properties`:

```properties
# Use a shared Godot source directory outside the project
godot.dir=/path/to/your/shared/godot
```

When `godot.dir` is not set, the build uses the `ios/godot/` directory. The path supports `~` and environment variable expansion.

#### Godot Android Library (AAR - optional)

By default, the Godot Android AAR libary file is expected to be placed inside `android/libs/` directory inside the project. If you want to use a location elsewhere on your machine (e.g. to share it across multiple plugin projects), set `lib.dir` in `local.properties`:

```properties
# Use a shared Godot AAR library directory outside the project
lib.dir=/path/to/your/shared/aar
```

When `lib.dir` is not set, the build uses the `android/libs/` directory. The path supports `~` and environment variable expansion.

**Note:** The Godot headers directory must contain a `GODOT_VERSION` file whose content matches the `godotVersion` property in `common/config/godot.properties`. The `downloadGodotHeaders` Gradle task creates this file automatically when it downloads the headers. If the directory already exists but contains a different version, the build will fail with a clear error message - run `./script/build_ios.sh -gG` to remove the old directory and re-download the correct version.

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> iOS Configuration

The `ios/config/ios.properties` file contains iOS-specific settings:

```properties
# iOS deployment target
platform_version=14.3

# Swift language version (required - must match your Xcode project)
swift_version=5.9

# iOS system framework dependencies (comma-separated)
frameworks=Foundation.framework,...

# Embedded iOS external framework dependencies (comma-separated; may be empty)
# Use this for vendored or prebuilt xcframeworks that are NOT managed by SPM.
# SPM packages should be declared in spm_dependencies.json instead.
embedded_frameworks=res://ios/framework/*.xcframework,...

# Linker flags (comma-separated; may be empty)
flags=-ObjC,-Wl,...
```

The `frameworks`, `embedded_frameworks`, and `flags` values are comma-separated lists. The build system parses them into typed lists at configuration time (`IosConfig.kt`) - blank entries are ignored. Values are used as-is for token replacement in GDScript templates and passed directly to `xcodebuild`.

GDScript templates may reference the following tokens for iOS values set in `ios.properties` and `spm_dependencies.json`:

| Token                    | Source                        | GDScript type  |
|--------------------------|-------------------------------|----------------|
| `@iosFrameworks@`        | `frameworks` (ios.properties) | quoted strings |
| `@iosEmbeddedFrameworks@`| `embedded_frameworks`         | quoted strings |
| `@iosLinkerFlags@`       | `flags`                       | quoted strings |
| `@spmDependencies@`      | `spm_dependencies.json`       | GDScript dicts |

The `@spmDependencies@` token produces GDScript dictionary literals with StringName keys and no outer brackets (see [`IosConfig` SPM Dependencies](#iosconfig-spm-dependencies) for the exact format).

SPM dependencies are configured in the `ios/config/spm_dependencies.json` file in the following format:

```json
[
  {
    "url": "https://github.com/Alamofire/Alamofire",
    "version": "5.8.1",
    "products": [
      "Alamofire",
      "AlamofireImage"
    ]
  },
  {
    "url": "https://github.com/kishikawakatsumi/KeychainAccess",
    "version": "4.2.2",
    "products": [
      "KeychainAccess"
    ]
  }
]
```

If the plugin has no SPM dependencies:

```json
[

]
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Development Workflow

### Initial Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/godot-mobile-plugins/godot-plugin-template.git
   cd godot-plugin-template
   ```

2. **Configure Android SDK:**
   ```bash
   echo "sdk.dir=/path/to/your/android-sdk" > common/local.properties
   ```

3. **First build:**
   ```bash
   # Android only
   ./script/build.sh -a -- -b

   # iOS only (macOS) - downloads Godot automatically
   ./script/build.sh -i -- -A
   ```

### Making Changes

1. **Edit source code:**
   - Android: `android/src/main/`
   - iOS: `ios/src/`
   - GDScript templates: `addon/src/`

2. **Build and test:**
   ```bash
   # Quick Android build
   ./script/build.sh -a -- -b

   # Install to demo app
   ./script/build.sh -D

   # Run demo in Godot to test
   cd demo
   godot project.godot
   ```

3. **Iterate:**
   - Make changes
   - Rebuild with `./script/build.sh -a -- -cb` or  `./script/build.sh -i -- -cb`
   - Test in demo app
   - Repeat until tests pass

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Building

There are three main build scripts located in the `script` directory.

- `build.sh` - the main build script
- `build_android.sh` - build script for Android platform
- `build_ios.sh` - build script for iOS platform

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> Cross-Platform Builds

Cross-platform builds with the `build.sh` script.

#### Build Options

| Option | Description |
|--------|-------------|
| `-a` | Build plugin for Android platform (`-a -- -h` for all options) |
| `-i` | Build plugin for iOS platform (`-i -- -h` for all options) |
| `-c` | Remove existing builds |
| `-C` | Remove existing builds and archives |
| `-d` | Uninstall plugin from demo app |
| `-D` | Install plugin to demo app |
| `-f` | Fix source code format issues |
| `-A` | Create Android release archive |
| `-I` | Create iOS release archive |
| `-M` | Create multi-platform release archive |
| `-R` | Create all release archives |
| `-v` | Verify source code format compliance |

#### Output Locations

- **GDScript code:** `addon/build/output/`
- **Debug AAR:** `android/build/outputs/aar/*-debug.aar`
- **Release AAR:** `android/build/outputs/aar/*-release.aar`
- **Built plugin:** `common/build/plugin/`
- **Release archive:** `release/PluginTemplatePlugin-*-v*.zip`

---

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> Android Builds

#### Quick Reference

```bash
# Clean and build Android debug
./script/build.sh -a -- -cb

**Note:** Options after `--` are passed to `build_android.sh`

# Clean and build Android release
./script/build.sh -a -- -cbr

# Install Android plugin to demo app
./script/build_android.sh -D

# Uninstall Android plugin from demo app
./script/build_android.sh -d

# Create Android release archive
./script/build_android.sh -R
```

#### Build Options

| Option | Description |
|--------|-------------|
| `-b` | Build plugin for Android platform (debug build variant by default) |
| `-c` | Clean Android build |
| `-d` | Uninstall Android plugin from demo app |
| `-D` | Install Android plugin to demo app |
| `-h` | Display script usage information |
| `-r` | Build Android plugin with release build variant |
| `-R` | Create Android release archive |

#### Android Studio

If using Android Studio, make sure to open the root Gradle project from the `common` directory.

---

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> iOS Builds

#### Quick Reference

```bash
# Clean and run iOS debug build
./script/build.sh -i -- -cb

**Note:** Options after `--` are passed to `build_ios.sh`

# Full build (first time - downloads Godot headers automatically)
./script/build_ios.sh -A

# Clean and rebuild (reuses existing Godot headers)
./script/build_ios.sh -ca

# Full clean rebuild (removes Godot headers directory first)
./script/build_ios.sh -cgA

# Clean, build and create archive
./script/build_ios.sh -cR

# Debug build for simulator
./script/build_ios.sh -bs

# Release build for simulator
./script/build_ios.sh -Bs

# Install iOS plugin to demo app
./script/build_ios.sh -D

# Uninstall iOS plugin from demo app
./script/build_ios.sh -d

# Resolve SPM dependencies only
./script/build_ios.sh -r
```

#### Build Options

| Option | Description |
|--------|-------------|
| `-a` | Update SPM packages and build both debug and release variants |
| `-A` | Download Godot headers, update SPM packages, and build both debug and release variants |
| `-b` | Run debug build (device); combine with `-s` for simulator |
| `-B` | Run release build (device); combine with `-s` for simulator |
| `-c` | Clean existing build |
| `-d` | Uninstall iOS plugin from demo app |
| `-D` | Install iOS plugin to demo app |
| `-g` | Remove Godot headers directory |
| `-G` | Download Godot headers |
| `-h` | Display help |
| `-p` | Remove SPM packages and build artifacts |
| `-P` | Add SPM packages from configuration |
| `-r` | Resolve SPM dependencies |
| `-R` | Create release archive |
| `-s` | Simulator build; use with `-b` for simulator debug, `-B` for simulator release |

#### Build Process Explained

The iOS build process involves several steps that are orchestrated automatically:

1. **Download Godot Headers** (if needed):
   - Downloads a pre-built Godot headers archive from `github.com/godot-mobile-plugins/godot-headers`
   - Version is determined by `godotVersion` and `godotReleaseType` in `godot.properties`
   - Extracted to `ios/godot/` by default, or to the path set by `godot.dir` in `common/local.properties`
   - The download is skipped if the correct version is already present (checked via a `GODOT_VERSION` file)
   - If the directory exists but contains a different version, the build fails with a clear error - run `./script/build_ios.sh -gG` to switch versions

2. **Validate Swift Version**:
   - Reads `swift_version` from `ios/config/ios.properties`
   - Fails early with a clear error if the property is missing or blank
   - Syncs the version into `plugin.xcodeproj/project.pbxproj` automatically

3. **Validate Godot Version**:
   - Confirms the `GODOT_VERSION` file in the Godot headers directory matches `godotVersion` in `godot.properties`

4. **Update & Resolve SPM Packages**:
   - Reads dependency definitions from `ios/config/spm_dependencies.json`
   - Injects package references into the Xcode project via `script/spm_manager.rb` (requires Ruby and the `xcodeproj` gem)
   - Resolves the packages with `xcodebuild -resolvePackageDependencies`

5. **Build XCFrameworks**:
   - Builds up to four variants via `xcodebuild archive`:
     - `buildiOSDebug` - device (arm64), debug
     - `buildiOSRelease` - device (arm64), release
     - `buildiOSDebugSimulator` - simulator (arm64/x86_64), debug
     - `buildiOSReleaseSimulator` - simulator (arm64/x86_64), release
   - The `-s` flag selects simulator variants; without it, device variants are built
   - Archives are created as `.xcarchive` bundles under `ios/build/lib/`
   - XCFrameworks combining device and simulator slices are assembled in `ios/build/framework/`
   - **Only the plugin's own xcframeworks** (`PluginName.debug.xcframework`, `PluginName.release.xcframework`) are copied into the plugin directory and included in release archives
   - SPM dependency xcframeworks produced in `ios/build/DerivedData/` are **not** bundled in the archive; they are resolved by Xcode at Godot iOS export time using the `Package.resolved` file that is committed alongside the Xcode project

#### Output Locations

- **Godot headers:** `ios/godot/` (default) or path set by `godot.dir` in `common/local.properties`
- **Build artifacts:** `ios/build/`
- **xcarchives:** `ios/build/lib/ios_debug.xcarchive`, `ios_release.xcarchive`, `sim_debug.xcarchive`, `sim_release.xcarchive`
- **Plugin XCFrameworks:** `ios/build/framework/PluginTemplatePlugin.debug.xcframework`, `PluginTemplatePlugin.release.xcframework`
- **Release archive:** `release/PluginTemplatePlugin-iOS-v*.zip`

> **Note:** Release archives (iOS and Multi) contain only the plugin's own xcframeworks. SPM dependency xcframeworks are intentionally excluded — they are fetched and linked by Xcode at Godot iOS export time using the `Package.resolved` committed with the Xcode project.

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Code Formatting

The project enforces consistent formatting across all source languages. Two aggregate tasks are available via the main build script:

```bash
# Verify all source code format compliance
./script/build.sh -v

# Fix all source code format issues
./script/build.sh -f
```

These delegate to the following per-language Gradle sub-tasks:

| Check task | Fix task | Language | Tool | Module |
|------------|----------|----------|------|--------|
| `checkGdscriptFormat` | `formatGdscriptSource` | GDScript | gdformat | addon |
| `checkJavaFormat` | `rewriteRun` | Java | Checkstyle / OpenRewrite | android |
| `checkXmlFormat` | `formatXml` | XML | Prettier | android |
| `checkObjCFormat` | `formatObjCSource` | ObjC / C++ | clang-format | ios |
| `checkSwiftFormat` | `formatSwiftSource` | Swift | swiftlint | ios |
| `checkKtsFormat` | `formatKtsSource` | Gradle KTS | ktlint | common |
| `checkBashScriptFormat` | `applyBashScriptFormat` | Bash | shellcheck | common |
| `checkEditorConfig` | _(n/a)_ | All files | editorconfig-checker | common |

Sub-tasks can also be run individually. For example, to check only GDScript formatting:

```bash
cd common
./gradlew :addon:checkGdscriptFormat
```

Sub-tasks that require external tools (`ktlint`, `shellcheck`, `editorconfig-checker`, `clang-format`, `swiftlint`, `gdformat`) will fail with a clear error if the tool is not found on `PATH`. See [Prerequisites](#-prerequisites) for installation instructions.

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Testing

### Testing in Demo App

1. **Install plugin to demo:**
   ```bash
   ./script/build.sh -D
   ```

2. **Open demo project:**
   ```bash
   cd demo
   godot project.godot
   ```

3. **Run and test features:**

### Android Testing

```bash
# Build and install
./script/build.sh -caD

# Export Android build from Godot
# Install on device/emulator
adb install demo/export/android/demo.apk

# View logs
adb logcat | grep -i PluginTemplate
```

### iOS Testing (macOS only)

```bash
# Build and install
./script/build.sh -I -D

# Open in Xcode
cd demo
open ios/demo.xcodeproj

# Build and run on simulator/device from Xcode
```

### Automated Testing

Consider adding:
- Unit tests for native code
- UI tests for demo app
- CI/CD pipeline (GitHub Actions)

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Creating Releases

### Full Multi-Platform Release

```bash
# Create all release archives
./script/build.sh -R
```

This creates:
- `release/PluginTemplatePlugin-Android-v*.zip`
- `release/PluginTemplatePlugin-iOS-v*.zip`
- `release/PluginTemplatePlugin-Multi-v*.zip` (combined)

### Platform-Specific Releases

```bash
# Create all release archives
./script/build.sh -R

# Create only Android release archive
./script/build.sh -A

# Create only iOS release archive
./script/build.sh -I

# Create only multi-platform release archive
./script/build.sh -M
```

### Release Checklist

- [ ] Update version in `common/config/plugin.properties` (`pluginVersion`)
- [ ] Update versions in issue templates (`.github/ISSUE_TEMPLATE`)
- [ ] Test on both platforms
- [ ] Build release archives
- [ ] Create GitHub release
- [ ] Upload archives to release & publish
- [ ] Close GitHub milestone
- [ ] Post GitHub announcement
- [ ] Update Asset Library listing
- [ ] Update Asset Store listing

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Installation

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> Installing to Demo App

```bash
# Install both platforms
./script/build.sh -D

# Uninstall
./script/build.sh -d
```

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="20"> Installing to Your Project

```bash
# Using install script
./script/install.sh -t /path/to/your/project -z /path/to/PluginTemplatePlugin-*.zip

# Example
./script/install.sh -t ~/MyGame -z release/PluginTemplatePlugin-Multi-v6.0.zip
```

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Troubleshooting

### Common Build Issues

#### Android

**Problem:** Gradle version mismatch
```bash
# Solution: Use Gradle wrapper
cd common
./gradlew --version
./gradlew clean build
```

**Problem:** Dependency resolution failures
```bash
# Solution: Clear Gradle cache
rm -rf ~/.gradle/caches/
./gradlew clean build --refresh-dependencies
```

#### iOS

**Problem:** SPM package resolution fails
```bash
# Solution: Clear SPM cache and re-resolve
./script/build_ios.sh -pP
```

**Problem:** Xcode build fails
```bash
# Solution: Clean derived data and rebuild
rm -rf ios/build/DerivedData
./script/build_ios.sh -cb
```

**Problem:** Godot version mismatch (headers directory contains the wrong version)
```bash
# The GODOT_VERSION file in the headers directory must match
# the godotVersion property in common/config/godot.properties.
# Solution: remove the existing headers directory and re-download
./script/build_ios.sh -gG
```

**Problem:** Build cannot find Godot headers after setting a custom `godot.dir`
```bash
# Verify the path is set correctly in common/local.properties:
#   godot.dir=/your/custom/path
# Then download the headers into that directory:
./script/build_ios.sh -G
```

**Problem:** Build fails with "swift_version not configured"
```bash
# Solution: add swift_version to ios/config/ios.properties, e.g.:
#   swift_version=5.9
# Then retry the build.
```

**Problem:** "No such module" errors
```bash
# Solution: Ensure packages are added and resolved
./script/build_ios.sh -pP
```

**Problem:** `xcodeproj` gem missing (Ruby gem required for SPM management)
```bash
# Solution: install the gem manually
gem install xcodeproj --user-install
# The build system will also install it automatically if Ruby is available.
```

### Getting Help

- Check existing [GitHub Issues](https://github.com/godot-mobile-plugins/godot-plugin-template/issues)
- Check existing [GitHub Discussions](https://github.com/godot-mobile-plugins/godot-plugin-template/discussions)
- Review [Godot documentation](https://docs.godotengine.org/)

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Contributing Guidelines

### Code Style

- **GDScript:** Follow [GDScript style guide](https://docs.godotengine.org/en/stable/tutorials/scripting/gdscript/gdscript_styleguide.html)
- **Java:** Follow [Google Java style guide](https://google.github.io/styleguide/javaguide.html)
- **Kotlin:** Follow [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide)
- **Objective-C:** Follow [Google Objective-C style guide](https://google.github.io/styleguide/objcguide.html)
- **Swift:** Follow [Swift style guide](https://www.swift.org/documentation/api-design-guidelines/)
- **Ruby:** Follow [Ruby style guide](https://rubystyle.guide/) (used in `script/spm_manager.rb`)
- **Shell:** Follow [Google Shell style guide](https://google.github.io/styleguide/shellguide.html); all scripts are checked with `shellcheck`

### Commit Messages

Use conventional commits format:

```
type(scope): subject

body

footer
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
```
feat(android): add support for native ads
fix(ios): resolve banner positioning issue
docs: update installation instructions
```

### Pull Request Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Test on both platforms
5. Commit with descriptive messages
6. Push to your fork
7. Open a Pull Request with:
   - Clear description of changes
   - Related issue numbers
   - Testing performed
   - Screenshots (if UI changes)

### Reporting Issues

Include:
- Plugin version
- Godot version
- Platform (Android/iOS)
- Device/OS version
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs

---

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/main/icon.png" width="24"> Additional Resources

- [Godot Engine Documentation](https://docs.godotengine.org/)
- [Android Developer Documentation](https://developer.android.com/)
- [iOS Developer Documentation](https://developer.apple.com/documentation/)
- [Gradle Documentation](https://docs.gradle.org/)

---
