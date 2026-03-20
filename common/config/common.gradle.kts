//
// © 2024-present https://github.com/cengiz-pz
//

import java.io.FileInputStream
import java.util.Properties

val buildProperties =
    Properties().apply {
        load(FileInputStream("$rootDir/config/build.properties"))
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

val pluginProperties =
    Properties().apply {
        load(FileInputStream("$rootDir/config/plugin.properties"))
    }

val godotProperties =
    Properties().apply {
        load(FileInputStream("$rootDir/config/godot.properties"))
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

    // Plugin details
    set("pluginNodeName", pluginProperties.getProperty("pluginNodeName"))
    set("pluginName", "${get("pluginNodeName")}Plugin")
    set("pluginModuleName", "${pluginProperties.getProperty("pluginModuleName")}")
    set("pluginPackageName", pluginProperties.getProperty("pluginPackage"))
    set("pluginVersion", pluginProperties.getProperty("pluginVersion"))

    // Godot
    set("godotVersion", godotProperties.getProperty("godotVersion"))
    set("godotReleaseType", godotProperties.getProperty("godotReleaseType"))

    // Project directories
    set("pluginDir", "$rootDir/build/plugin")
    set("repositoryRootDir", "$rootDir/..")
    set("archiveDir", "${get("repositoryRootDir")}/release")
    set("demoDir", "${get("repositoryRootDir")}/demo")

    // Release archive
    set("pluginArchiveMulti", "${get("pluginName")}-Multi-v${get("pluginVersion")}.zip")
}
