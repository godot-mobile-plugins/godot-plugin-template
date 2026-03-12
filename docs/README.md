<p align="center">
	<img width="128" height="128" src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/demo/assets/plugin-template-android.png">
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	<img width="128" height="128" src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/demo/assets/plugin-template-ios.png">
</p>

<br>

<div align="center">
	<a href="https://github.com/godot-mobile-plugins/godot-plugin-template"><img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-plugin-template?label=Stars&style=plastic" height="40"/></a>
	<img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-plugin-template?label=Latest%20Release&style=plastic" height="40"/>
	<img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-plugin-template/latest/total?label=Downloads&style=plastic" height="40"/>
	<img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-plugin-template/total?label=Total%20Downloads&style=plastic" height="40"/>
</div>

<br>

<!--TO-BE-DELETED-AFTER-INIT-BEGIN-->
</br>

---
# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="24"> Template Initialization
---

**Run the `init.sh` script in order to initialize the repository.**

</br></br>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> How To Run `init.sh`

**Use the `-n` option to rename plugin:**

```
$ godot-plugin-template > ./script/init.sh -n NameOfYourPlugin
```
or optionally specify the name of the plugin author and author's GitHub username:
```
$ godot-plugin-template > ./script/init.sh -n NameOfYourPlugin -a "Author Name" -g "github-username"
```

</br>

**Example:**

```
$ godot-plugin-template > ./script/init.sh -n GameBooster
```
or:
```
$ godot-plugin-template > ./script/init.sh -n GameBooster -a "Maria Wang" -g "mariawang"
```

</br></br>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Dry Run `init.sh`

**Use the `-d` option to dry-run `init.sh`:**

```
$ godot-plugin-template > ./script/init.sh -n NameOfYourPlugin -d
```
or:
```
$ godot-plugin-template > ./script/init.sh -n NameOfYourPlugin -a "Author Name" -g "github-username" -d
```

**The dry-run will show what would be done without making changes.**

---
---

</br></br></br></br></br></br></br></br></br></br>
<!--TO-BE-DELETED-AFTER-INIT-END-->

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="24"> Godot Plugin Template Plugin

A Godot plugin that provides a unified GDScript interface for getting information on plugin templates on **Android** and **iOS**.

**Key Features:**
- Get information about all available plugin templates
- Know when a template is ready
- ...

<br>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Table of Contents
- [Installation](#installation)
- [Usage](#usage)
- [Signals](#signals)
- [Methods](#methods)
- [Classes](#classes)
- [Platform-Specific Notes](#platform-specific-notes)
- [Links](#links)
- [All Plugins](#all-plugins)
- [Credits](#credits)
- [Contributing](#contributing)

<br>

<a name="installation"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Installation
_Before installing this plugin, make sure to uninstall any previous versions of the same plugin._

_If installing both Android and iOS versions of the plugin in the same project, then make sure that both versions use the same addon interface version._

There are 2 ways to install the `PluginTemplate` plugin into your project:
- Through the Godot Editor's AssetLib
- Manually by downloading archives from Github

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="18"> Installing via AssetLib
Steps:
- search for and select the `PluginTemplate` plugin in Godot Editor
- click `Download` button
- on the installation dialog...
	- keep `Change Install Folder` setting pointing to your project's root directory
	- keep `Ignore asset root` checkbox checked
	- click `Install` button
- enable the plugin via the `Plugins` tab of `Project->Project Settings...` menu, in the Godot Editor

#### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="16"> Installing both Android and iOS versions of the plugin in the same project
When installing via AssetLib, the installer may display a warning that states "_[x number of]_ files conflict with your project and won't be installed." You can ignore this warning since both versions use the same addon code.

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="18"> Installing manually
Steps:
- download release archive from Github
- unzip the release archive
- copy to your Godot project's root directory
- enable the plugin via the `Plugins` tab of `Project->Project Settings...` menu, in the Godot Editor

<br>

<a name="usage"></a>


## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Usage
Add `PluginTemplate` node to your main scene or an autoload global scene.

- use the `PluginTemplate` node's `get_plugin_template()` method to get information on all available plugin templates
- connect `PluginTemplate` node signals
	- `template_ready(a_template: PluginTemplateInfo)`
	- ...

Example usage:
```
@onready var plugin_template := $PluginTemplate

func _ready():
	plugin_template.template_ready.connect(_on_template_ready)

	var templates: Array[PluginTemplateInfo] = plugin_template.get_plugin_template()
	for template in templates:
		print("Template description: %s" % [template.get_description()])

func _on_template_ready(template: PluginTemplateInfo):
	print("Template ready:", template.get_description())
```

<br>

<a name="signals"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Signals
- register listeners to the following signals of the `PluginTemplate` node:
	- `template_ready(a_template: PluginTemplateInfo)`
	- ...

<br>

<a name="methods"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Methods
- `get_plugin_template() -> Array[PluginTemplateInfo]` - returns an array of `PluginTemplateInfo` objects

<br>

<a name="classes"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Classes

### <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="16"> PluginTemplateInfo
- Encapsulates plugin template information.
- Properties:
	- `description`: description of the template
	- `other`: ...
	- ...

<br>

<a name="platform-specific-notes"></a>

## <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> Platform-Specific Notes

### Android
- Download Android export template and enable gradle build from export settings
- **Troubleshooting:**
- Logs: `adb logcat | grep 'godot'` (Linux), `adb.exe logcat | select-string "godot"` (Windows)
- You may find the following resources helpful:
	- https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_android.html
	- https://developer.android.com/tools/adb
	- https://developer.android.com/studio/debug
	- https://developer.android.com/courses

### iOS
- Follow instructions on [Exporting for iOS](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_ios.html)
- View XCode logs while running the game for troubleshooting.
- See [Godot iOS Export Troubleshooting](https://docs.godotengine.org/en/stable/tutorials/export/exporting_for_ios.html#troubleshooting).

<br>

<a name="links"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="24"> Links

- [AssetLib Entry Android](https://godotengine.org/asset-library/asset/9999)
- [AssetLib Entry iOS](https://godotengine.org/asset-library/asset/8888)

<br>

<a name="all-plugins"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="24"> All Plugins

| ✦ | Plugin | Android | iOS | Latest Release | Downloads | Stars |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| <img src="https://raw.githubusercontent.com/godot-sdk-integrations/godot-admob/main/addon/src/icon.png" width="20"> | [Admob](https://github.com/godot-sdk-integrations/godot-admob) | ✅ | ✅ | <a href="https://github.com/godot-sdk-integrations/godot-admob/releases"><img src="https://img.shields.io/github/release-date/godot-sdk-integrations/godot-admob?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-sdk-integrations/godot-admob?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-sdk-integrations/godot-admob/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-sdk-integrations/godot-admob/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-sdk-integrations/godot-admob?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-connection-state/main/addon/src/icon.png" width="20"> | [Connection State](https://github.com/godot-mobile-plugins/godot-connection-state) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-connection-state/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-connection-state?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-connection-state?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-connection-state/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-connection-state/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-connection-state?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-deeplink/main/addon/src/icon.png" width="20"> | [Deeplink](https://github.com/godot-mobile-plugins/godot-deeplink) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-deeplink/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-deeplink?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-deeplink?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-deeplink/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-deeplink/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-deeplink?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-firebase/main/addon/src/icon.png" width="20"> | [Firebase](https://github.com/godot-mobile-plugins/godot-firebase) | ✅ | ✅ | - <!-- <a href="https://github.com/godot-mobile-plugins/godot-firebase/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-firebase?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-firebase?label=%20" /></a> --> | - <!-- <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-firebase/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-firebase/total?label=%20" /> --> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-firebase?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-inapp-review/main/addon/src/icon.png" width="20"> | [In-App Review](https://github.com/godot-mobile-plugins/godot-inapp-review) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-inapp-review/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-inapp-review?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-inapp-review?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-inapp-review/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-inapp-review/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-inapp-review?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-native-camera/main/addon/src/icon.png" width="20"> | [Native Camera](https://github.com/godot-mobile-plugins/godot-native-camera) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-native-camera/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-native-camera?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-native-camera?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-native-camera/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-native-camera/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-native-camera?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-notification-scheduler/main/addon/src/icon.png" width="20"> | [Notification Scheduler](https://github.com/godot-mobile-plugins/godot-notification-scheduler) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-notification-scheduler/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-notification-scheduler?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-notification-scheduler?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-notification-scheduler/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-notification-scheduler/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-notification-scheduler?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-oauth2/main/addon/src/icon.png" width="20"> | [OAuth 2.0](https://github.com/godot-mobile-plugins/godot-oauth2) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-oauth2/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-oauth2?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-oauth2?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-oauth2/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-oauth2/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-oauth2?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-qr/main/addon/src/icon.png" width="20"> | [QR](https://github.com/godot-mobile-plugins/godot-qr) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-qr/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-qr?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-qr?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-qr/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-qr/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-qr?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-share/main/addon/src/icon.png" width="20"> | [Share](https://github.com/godot-mobile-plugins/godot-share) | ✅ | ✅ | <a href="https://github.com/godot-mobile-plugins/godot-share/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-share?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-share?label=%20" /></a> | <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-share/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-share/total?label=total" /> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-share?style=plastic&label=%20" /> |
| <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="20"> | [Plugin Template](https://github.com/godot-mobile-plugins/godot-plugin-template) | ✅ | ✅ | - <!-- <a href="https://github.com/godot-mobile-plugins/godot-plugin-template/releases"><img src="https://img.shields.io/github/release-date/godot-mobile-plugins/godot-plugin-template?label=%20" /> <img src="https://img.shields.io/github/v/release/godot-mobile-plugins/godot-plugin-template?label=%20" /></a> --> | - <!-- <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-plugin-template/latest/total?label=latest" /> <img src="https://img.shields.io/github/downloads/godot-mobile-plugins/godot-plugin-template/total?label=%20" /> --> | <img src="https://img.shields.io/github/stars/godot-mobile-plugins/godot-plugin-template?style=plastic&label=%20" /> |

<br>

<a name="credits"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="24"> Credits

Developed by [<<AuthorName>>](https://github.com/<<GitHubUsername>>)

Original repository: [Godot Plugin Template Plugin](https://github.com/godot-mobile-plugins/godot-plugin-template)

<br>

<a name="contributing"></a>

# <img src="https://raw.githubusercontent.com/godot-mobile-plugins/godot-plugin-template/main/addon/src/icon.png" width="24"> Contributing

See [our guide](https://github.com/godot-mobile-plugins/godot-plugin-template?tab=contributing-ov-file) if you would like to contribute to this project.

<br>

# 💖 Support the Project

If this plugin has helped you, consider supporting its development! Every bit of support helps keep the plugin updated and bug-free.

| ✦ | Ways to Help | How to do it |
| :--- | :--- | :--- |
|✨⭐| **Spread the Word** | [Star this repo](https://github.com/godot-mobile-plugins/godot-plugin-template/stargazers) to help others find it. |
|💡✨| **Give Feedback** | [Open an issue](https://github.com/godot-mobile-plugins/godot-plugin-template/issues) or [suggest a feature](https://github.com/godot-mobile-plugins/godot-plugin-template/issues/new). |
|🧩| **Contribute** | [Submit a PR](https://github.com/godot-mobile-plugins/godot-plugin-template?tab=contributing-ov-file) to help improve the codebase. |
|❤️| **Buy a Coffee** | Support the maintainers on GitHub Sponsors or other platforms. |

<br>

## ⭐ Star History
[![Star History Chart](https://api.star-history.com/svg?repos=godot-mobile-plugins/godot-plugin-template&type=Date)](https://star-history.com/#godot-mobile-plugins/godot-plugin-template&Date)
