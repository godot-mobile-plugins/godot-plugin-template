//
// © 2024-present https://github.com/cengiz-pz
//

import java.util.Properties

apply(plugin = "base-conventions")

val loadProperties:         (String)     -> Properties by extra
val applyGradleScripts:     (Project, Properties) -> Unit by extra
val forwardExtraProperties: (Project, Properties) -> Unit by extra

apply(from = rootProject.file("config/common.gradle.kts"))

val buildProperties = loadProperties("$projectDir/config/android-build.properties")

applyGradleScripts(project, buildProperties)

extra.apply {
    forwardExtraProperties(project, buildProperties)

    // Godot AAR
    set(
        "godotAarUrl",
        "https://github.com/godotengine/godot-builds/releases/download/" +
            "${get("godotVersion")}-${get("godotReleaseType")}/" +
            "godot-lib.${get("godotVersion")}.${get("godotReleaseType")}.template_release.aar",
    )
    set("godotAarFile", "godot-lib-${get("godotVersion")}.${get("godotReleaseType")}.aar")

    // Release archive
    set("pluginArchiveAndroid", "${get("pluginName")}-Android-v${get("pluginVersion")}.zip")
}
