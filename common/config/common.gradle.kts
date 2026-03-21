//
// © 2024-present https://github.com/cengiz-pz
//

import java.util.Properties

apply(plugin = "base-conventions")

val loadProperties:         (String)     -> Properties by extra
val applyGradleScripts:     (Project, Properties) -> Unit by extra
val forwardExtraProperties: (Project, Properties) -> Unit by extra

val buildProperties  = loadProperties("$rootDir/config/build.properties")
val pluginProperties = loadProperties("$rootDir/config/plugin.properties")
val godotProperties  = loadProperties("$rootDir/config/godot.properties")

applyGradleScripts(project, buildProperties)

extra.apply {
    forwardExtraProperties(project, buildProperties)

    // Plugin identity
    set("pluginNodeName",    pluginProperties.getProperty("pluginNodeName"))
    set("pluginName",        "${get("pluginNodeName")}Plugin")
    set("pluginModuleName",  pluginProperties.getProperty("pluginModuleName"))
    set("pluginPackageName", pluginProperties.getProperty("pluginPackage"))
    set("pluginVersion",     pluginProperties.getProperty("pluginVersion"))

    // Godot runtime
    set("godotVersion",     godotProperties.getProperty("godotVersion"))
    set("godotReleaseType", godotProperties.getProperty("godotReleaseType"))

    // Directory layout
    set("pluginDir",         "$rootDir/build/plugin")
    set("repositoryRootDir", "$rootDir/..")
    set("archiveDir",        "${get("repositoryRootDir")}/release")
    set("demoDir",           "${get("repositoryRootDir")}/demo")

    // Archive name
    set("pluginArchiveMulti", "${get("pluginName")}-Multi-v${get("pluginVersion")}.zip")
}
