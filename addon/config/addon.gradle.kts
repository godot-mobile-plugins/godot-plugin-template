//
// © 2024-present https://github.com/cengiz-pz
//

import java.util.Properties

apply(plugin = "base-conventions")

val loadProperties:         (String)     -> Properties by extra
val applyGradleScripts:     (Project, Properties) -> Unit by extra
val forwardExtraProperties: (Project, Properties) -> Unit by extra

val buildProperties  = loadProperties("$projectDir/config/addon-build.properties")
val pluginProperties = loadProperties("$rootDir/config/plugin.properties")
val iosProperties    = loadProperties("$rootDir/../ios/config/ios.properties")

applyGradleScripts(project, buildProperties)

extra.apply {
    forwardExtraProperties(project, buildProperties)

    set("templateDir", "$projectDir/src/main")
    set("sharedTemplateDir", "$projectDir/src/shared")
    set("buildDir",    "$projectDir/build")
    set("outputDir",   "${get("buildDir")}/output")

    // Plugin identity
    set("pluginNodeName",    pluginProperties.getProperty("pluginNodeName"))
    set("pluginName",        "${get("pluginNodeName")}Plugin")
    set("pluginPackageName", pluginProperties.getProperty("pluginPackage"))
    set("pluginVersion",     pluginProperties.getProperty("pluginVersion"))

    // iOS parameters
    set("iosPlatformVersion",    iosProperties.getProperty("platform_version"))
    set("iosFrameworks",         iosProperties.getProperty("frameworks"))
    set("iosEmbeddedFrameworks", iosProperties.getProperty("embedded_frameworks"))
    set("iosLinkerFlags",        iosProperties.getProperty("flags"))
    val moduleName = pluginProperties.getProperty("pluginModuleName")
    set("iosInitializationMethod",   "${moduleName}_plugin_init")
    set("iosDeinitializationMethod", "${moduleName}_plugin_deinit")
}
