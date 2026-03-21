//
// © 2024-present https://github.com/cengiz-pz
//

import java.util.Properties

apply(plugin = "base-conventions")

val loadProperties:         (String)     -> Properties by extra
val applyGradleScripts:     (Project, Properties) -> Unit by extra
val forwardExtraProperties: (Project, Properties) -> Unit by extra

apply(from = rootProject.file("config/common.gradle.kts"))

val buildProperties = loadProperties("$projectDir/config/ios-build.properties")

applyGradleScripts(project, buildProperties)

extra.apply {
    forwardExtraProperties(project, buildProperties)

    set("pluginArchiveiOS", "${get("pluginName")}-iOS-v${get("pluginVersion")}.zip")
}
