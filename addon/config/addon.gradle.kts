//
// © 2024-present https://github.com/cengiz-pz
//

import java.io.FileInputStream
import java.util.Properties

val pluginProperties =
    Properties().apply {
        load(FileInputStream("$rootDir/config/plugin.properties"))
    }

val iosProperties =
    Properties().apply {
        load(FileInputStream("$rootDir/../ios/config/ios.properties"))
    }

// Apply extra gradle build files that are configured to be applied
pluginProperties.forEach { entry ->
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
    // Set extra properties from config
    pluginProperties.forEach { entry ->
        val key = entry.key.toString()
        if (key.startsWith("extra.")) {
            val propertyName = key.removePrefix("extra.")
            val propertyValue = entry.value.toString()
            set(propertyName, propertyValue)
            println("[CONFIG] Set extra property: $propertyName to $propertyValue")
        }
    }

    set("templateDir", "$projectDir/src")
    set("buildDir", "$projectDir/build")
    set("outputDir", "${get("buildDir")}/output")

    // Plugin details
    set("pluginNodeName", pluginProperties.getProperty("pluginNodeName"))
    set("pluginName", "${get("pluginNodeName")}Plugin")
    set("pluginPackageName", pluginProperties.getProperty("pluginPackage"))
    set("pluginVersion", pluginProperties.getProperty("pluginVersion"))

    // iOS
    set("iosPlatformVersion", iosProperties.getProperty("platform_version"))
    set("iosFrameworks", iosProperties.getProperty("frameworks"))
    set("iosEmbeddedFrameworks", iosProperties.getProperty("embedded_frameworks"))
    set("iosLinkerFlags", iosProperties.getProperty("flags"))
    set("iosInitializationMethod", "${pluginProperties.getProperty("pluginModuleName")}_plugin_init")
    set("iosDeinitializationMethod", "${pluginProperties.getProperty("pluginModuleName")}_plugin_deinit")
}
