//
// © 2026-present https://github.com/cengiz-pz
//

// base-conventions
//
// Applying this plugin to a project ensures the build-logic module's compiled
// classes — including Project extension functions such as loadProperties() —
// are on the project's build-script compilation classpath.
//
// Apply it in every project build script whose apply(from = …) script plugins
// need to call those extensions:
//
//   plugins {
//       id("base-conventions")
//       // … other plugins
//   }

import java.util.Properties
import kotlinx.serialization.json.Json

/**
 * Reads SPM dependency entries from an spm_dependencies.json config file.
 *
 * Each entry in the JSON array has the form:
 *   { "url": "<URL>", "version": "<minimumVersion>", "products": ["<ProductName>", ...] }
 *
 * Returns a list of [SpmDependency] objects decoded via kotlinx.serialization.
 */
fun readSpmDependencies(configFile: File): List<SpmDependency> {
    if (!configFile.exists()) return emptyList()
    return Json.decodeFromString<List<SpmDependency>>(configFile.readText())
}

// Expose readSpmDependencies as a project-level extra so consuming build
// scripts can invoke it without redeclaring it.
project.extensions.extraProperties["readSpmDependencies"] =
    ::readSpmDependencies

/**
 * Loads a [Properties] file at [path], resolved relative to this project's directory.
 *
 * Available in every build script and apply(from = …) script plugin whose project
 * has applied a build-logic convention plugin (base-conventions).
 *
 * Usage:
 *   val props = loadProperties("$projectDir/config/foo.properties")
 */
fun loadProperties(path: String): Properties =
    Properties().also { props ->
        file(path).inputStream().use { props.load(it) }
    }

project.extensions.extraProperties["loadProperties"] =
    ::loadProperties

/**
 * Applies any extra Gradle scripts declared as `gradle.*` keys in [properties]
 * to the given [project].
 *
 * Each matching key's value is treated as a file path (absolute or relative to
 * the current project directory) and passed to `project.apply(from = …)`.
 *
 * Usage:
 *   val buildProperties = loadProperties("$projectDir/config/foo.properties")
 *   applyGradleScripts(project, buildProperties)
 */
fun applyGradleScripts(project: Project, properties: Properties) {
    properties.stringPropertyNames().forEach { key ->
        if (key.startsWith("gradle.")) {
            val fileName = properties.getProperty(key).trim()
            if (fileName.isNotBlank()) {
                val path = if (fileName.startsWith("/")) fileName else "./$fileName"
                project.apply(from = path)
                println("[CONFIG] Applied extra script: $fileName (from property $key)")
            }
        }
    }
}

project.extensions.extraProperties["applyGradleScripts"] =
    ::applyGradleScripts

/**
 * Forwards any properties declared as `extra.*` keys in [properties] into the
 * given [project]'s [ExtraPropertiesExtension].
 *
 * The `extra.` prefix is stripped from each key before the value is set, so
 * `extra.myProp=foo` becomes `project.extra["myProp"] = "foo"`.
 *
 * Usage:
 *   val buildProperties = loadProperties("$projectDir/config/foo.properties")
 *   forwardExtraProperties(project, buildProperties)
 */
fun forwardExtraProperties(project: Project, properties: Properties) {
    properties.stringPropertyNames().forEach { key ->
        if (key.startsWith("extra.")) {
            val name  = key.removePrefix("extra.")
            val value = properties.getProperty(key)
            project.extra.set(name, value)
            println("[CONFIG] Set extra property: $name to $value")
        }
    }
}

project.extensions.extraProperties["forwardExtraProperties"] =
    ::forwardExtraProperties
