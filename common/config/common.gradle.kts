//
// © 2024-present https://github.com/cengiz-pz
//

import java.io.FileInputStream
import java.util.Properties

val pluginProperties =
    Properties().apply {
        load(FileInputStream("$rootDir/config/plugin.properties"))
    }

val godotProperties =
    Properties().apply {
        load(FileInputStream("$rootDir/config/godot.properties"))
    }

extra.apply {
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
