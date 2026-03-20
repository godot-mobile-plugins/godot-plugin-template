//
// © 2024-present https://github.com/cengiz-pz
//

import java.io.FileInputStream
import java.util.Properties

apply(from = rootProject.file("config/common.gradle.kts"))

val buildProperties =
    Properties().apply {
        load(FileInputStream("$projectDir/config/android-build.properties"))
    }

// Apply extra gradle build files that are configured to be applied
buildProperties.forEach { entry ->
    val key = entry.key.toString()
    if (key.startsWith("gradle.")) {
        val fileName = entry.value.toString().trim()
        if (fileName.isNotBlank()) {
            val relativePath = if (fileName.startsWith("/")) fileName else "./$fileName"
            apply(from = relativePath)
            println("[CONFIG] Applied extra script: $fileName (from property $key)")
        }
    }
}

extra.apply {
    // Set extra properties from build config
    buildProperties.forEach { entry ->
        val key = entry.key.toString()
        if (key.startsWith("extra.")) {
            val propertyName = key.removePrefix("extra.")
            val propertyValue = entry.value.toString()
            set(propertyName, propertyValue)
            println("[CONFIG] Set extra property: $propertyName to $propertyValue")
        }
    }

    // AAR
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
