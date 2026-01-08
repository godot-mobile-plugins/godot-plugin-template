#!/usr/bin/env bash
#
# © 2026-present https://github.com/cengiz-pz
#

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(realpath "$SCRIPT_DIR/..")

plugin_node_name=""
dry_run=false


function display_help()
{
	echo
	$SCRIPT_DIR/echocolor.sh -y "The " -Y "$0 script" -y " renames plugin template files and content"
	echo
	$SCRIPT_DIR/echocolor.sh -Y "Syntax:"
	$SCRIPT_DIR/echocolor.sh -y "	$0 [-h|n <plugin node name>|-d]"
	echo
	$SCRIPT_DIR/echocolor.sh -Y "Options:"
	$SCRIPT_DIR/echocolor.sh -y "	h	display usage information"
	$SCRIPT_DIR/echocolor.sh -y "	n	specify name of the plugin node (eg. ConnectionState)"
	$SCRIPT_DIR/echocolor.sh -y "	d	dry-run mode; show what would be done without making changes"
	echo
	$SCRIPT_DIR/echocolor.sh -Y "Examples:"
	$SCRIPT_DIR/echocolor.sh -y "	* Create a OneStopShop plugin"
	$SCRIPT_DIR/echocolor.sh -y "		$> $0 -n OneStopShop"
	$SCRIPT_DIR/echocolor.sh -y "	* Dry-run for OneStopShop plugin"
	$SCRIPT_DIR/echocolor.sh -y "		$> $0 -n OneStopShop -d"
	echo
}


function display_status()
{
	echo
	$SCRIPT_DIR/echocolor.sh -c "********************************************************************************"
	$SCRIPT_DIR/echocolor.sh -c "* $1"
	$SCRIPT_DIR/echocolor.sh -c "********************************************************************************"
	echo
}


function display_error()
{
	$SCRIPT_DIR/echocolor.sh -r "Error: $1"
}


function split_caps() {
	local input="$1"
	local formatted

	# 1. Insert spaces between lowercase followed by uppercase
	# 2. Insert spaces between uppercase followed by Uppercase+lowercase (start of new word)
	formatted=$(sed -E 's/([[:lower:]])([[:upper:]])/\1 \2/g; s/([[:upper:]])([[:upper:]][[:lower:]])/\1 \2/g' <<< "$input")

	echo "$formatted"
}


while getopts "hn:d" option; do
	case $option in
		h)
			display_help
			exit;;
		n)
			plugin_node_name=$OPTARG
			;;
		d)
			dry_run=true
			;;
		\?)
			display_error "Invalid option: $option"
			echo
			display_help
			exit;;
	esac
done

if [[ -z "$plugin_node_name" ]]; then
	display_error "Plugin node name not specified"
	display_help
	exit 1
fi

if ! [[ "$plugin_node_name" =~ ^[A-Za-z][A-Za-z0-9]*$ ]]; then
    display_error "Plugin node name must start with a letter and contain only alphanumeric characters"
    exit 1
fi

if [[ "$OSTYPE" == "darwin"* ]]; then
	sed_i=(sed -i "") # For macOS
else
	sed_i=(sed -i)    # For GNU/Linux
fi

read -ra node_name_parts <<< "$(split_caps "$plugin_node_name")"

if $dry_run; then
	display_status "Dry-run mode enabled: No changes will be made"
fi

display_status "Replacing 'PluginTemplate' with '$plugin_node_name'"

if ! $dry_run; then
	find "$ROOT_DIR" -type f \
		-not -path "$ROOT_DIR/.git/*" \
		-not -path "$ROOT_DIR/script/*" \
		-not -path "$ROOT_DIR/ios/godot/*" \
		-not -path "$ROOT_DIR/ios/Pods/*" \
		-not -iname "*.png" \
		-not -iname "*.jar" \
		-not -iname "*.zip" \
		-not -iname ".DS_Store" \
		-exec env LC_ALL=C "${sed_i[@]}" -e "s/PluginTemplate/${plugin_node_name}/g" {} +
else
	echo "Would replace in the following files:"
	find "$ROOT_DIR" -type f -not -path "$ROOT_DIR/.git/*" -exec grep -l -F "PluginTemplate" {} + || true
fi

echo

if ! $dry_run; then
	find "$ROOT_DIR" -depth -name "*PluginTemplate*" -not -path "$ROOT_DIR/.git/*" -execdir bash -c \
		'mv -v "$1" "${1//PluginTemplate/$2}"' -- {} "$plugin_node_name" \;
else
	find "$ROOT_DIR" -depth -name "*PluginTemplate*" -not -path "$ROOT_DIR/.git/*" -execdir bash -c \
		'echo "Would rename \"$1\" to \"${1//PluginTemplate/$2}\""' -- {} "$plugin_node_name" \;
fi

lowercase_plugin_node_name=$(printf '%s' "$plugin_node_name" | tr '[:upper:]' '[:lower:]')

display_status "Replacing 'plugintemplate' with '$lowercase_plugin_node_name'"

if ! $dry_run; then
	find "$ROOT_DIR" -type f \
		-not -path "$ROOT_DIR/.git/*" \
		-not -path "$ROOT_DIR/script/*" \
		-not -path "$ROOT_DIR/ios/godot/*" \
		-not -path "$ROOT_DIR/ios/Pods/*" \
		-not -iname "*.png" \
		-not -iname "*.jar" \
		-not -iname "*.zip" \
		-not -iname ".DS_Store" \
		-exec env LC_ALL=C "${sed_i[@]}" -e "s/plugintemplate/${lowercase_plugin_node_name}/g" {} +
else
	echo "Would replace in the following files:"
	find "$ROOT_DIR" -type f -not -path "$ROOT_DIR/.git/*" -exec grep -l -F "plugintemplate" {} + || true
fi


joined_string=$(IFS=_; echo "${node_name_parts[*]}")
lowercase_joined_string=$(printf '%s' "$joined_string" | tr '[:upper:]' '[:lower:]')

display_status "Replacing 'plugin_template' with '$lowercase_joined_string'"

if ! $dry_run; then
	find "$ROOT_DIR" -type f \
		-not -path "$ROOT_DIR/.git/*" \
		-not -path "$ROOT_DIR/script/*" \
		-not -path "$ROOT_DIR/ios/godot/*" \
		-not -path "$ROOT_DIR/ios/Pods/*" \
		-not -iname "*.png" \
		-not -iname "*.jar" \
		-not -iname "*.zip" \
		-not -iname ".DS_Store" \
		-exec env LC_ALL=C "${sed_i[@]}" -e "s/plugin_template/${lowercase_joined_string}/g" {} +
else
	echo "Would replace in the following files:"
	find "$ROOT_DIR" -type f -not -path "$ROOT_DIR/.git/*" -exec grep -l -F "plugin_template" {} + || true
fi

echo

if ! $dry_run; then
	find "$ROOT_DIR" -depth -name "*plugin_template*" -not -path "$ROOT_DIR/.git/*" -execdir bash -c \
		'mv -v "$1" "${1//plugin_template/$2}"' -- {} "$lowercase_joined_string" \;
else
	find "$ROOT_DIR" -depth -name "*plugin_template*" -not -path "$ROOT_DIR/.git/*" -execdir bash -c \
		'echo "Would rename \"$1\" to \"${1//plugin_template/$2}\""' -- {} "$lowercase_joined_string" \;
fi


joined_string=$(IFS=-; echo "${node_name_parts[*]}")
lowercase_joined_string=$(printf '%s' "$joined_string" | tr '[:upper:]' '[:lower:]')

display_status "Replacing 'plugin-template' with '$lowercase_joined_string'"

if ! $dry_run; then
	find "$ROOT_DIR" -type f \
		-not -path "$ROOT_DIR/.git/*" \
		-not -path "$ROOT_DIR/script/*" \
		-not -path "$ROOT_DIR/ios/godot/*" \
		-not -path "$ROOT_DIR/ios/Pods/*" \
		-not -iname "*.png" \
		-not -iname "*.jar" \
		-not -iname "*.zip" \
		-not -iname ".DS_Store" \
		-exec env LC_ALL=C "${sed_i[@]}" -e "s/plugin-template/${lowercase_joined_string}/g" {} +
else
	echo "Would replace in the following files:"
	find "$ROOT_DIR" -type f -not -path "$ROOT_DIR/.git/*" -exec grep -l -F "plugin-template" {} + || true
fi

echo

if ! $dry_run; then
	find "$ROOT_DIR" -depth -name "*plugin-template*" \
		-not -path "$ROOT_DIR/.git/*" \
		-not -path "$ROOT_DIR" \
		-execdir bash -c \
		'mv -v "$1" "${1//plugin-template/$2}"' -- {} "$lowercase_joined_string" \;
else
	find "$ROOT_DIR" -depth -name "*plugin-template*" -not -path "$ROOT_DIR/.git/*" -execdir bash -c \
		'echo "Would rename \"$1\" to \"${1//plugin-template/$2}\""' -- {} "$lowercase_joined_string" \;
fi


joined_string=$(IFS=' '; echo "${node_name_parts[*]}")

display_status "Replacing 'Plugin Template' with '$joined_string'"

if ! $dry_run; then
	find "$ROOT_DIR" -type f \
		-not -path "$ROOT_DIR/.git/*" \
		-not -path "$ROOT_DIR/script/*" \
		-not -path "$ROOT_DIR/ios/godot/*" \
		-not -path "$ROOT_DIR/ios/Pods/*" \
		-not -iname "*.png" \
		-not -iname "*.jar" \
		-not -iname "*.zip" \
		-not -iname ".DS_Store" \
		-exec env LC_ALL=C "${sed_i[@]}" -e "s/Plugin Template/${joined_string}/g" {} +
else
	echo "Would replace in the following files:"
	find "$ROOT_DIR" -type f -not -path "$ROOT_DIR/.git/*" -exec grep -l -F "Plugin Template" {} + || true
fi


display_status "Removing initialization section from README doc"

if ! $dry_run; then
	"${sed_i[@]}" '/<!--TO-BE-DELETED-AFTER-INIT-BEGIN-->/,/<!--TO-BE-DELETED-AFTER-INIT-END-->/d' $ROOT_DIR/docs/README.md
else
	echo "Would remove initialization section in $ROOT_DIR/docs/README.md"
fi

display_status "Initialization completed; self-destructing"

if ! $dry_run; then
	rm -v "$0"
else
	echo "Would remove \"$0\""
fi
