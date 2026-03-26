#!/bin/bash
#
# © 2024-present https://github.com/cengiz-pz
#

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(realpath "$SCRIPT_DIR"/..)
IOS_DIR=$ROOT_DIR/ios
COMMON_DIR=$ROOT_DIR/common

LOCAL_PROPERTIES_FILE="$COMMON_DIR/local.properties"

# Resolve GODOT_DIR: use godot.dir from local.properties if set, otherwise default to $IOS_DIR/godot
GODOT_DIR=$IOS_DIR/godot
if [[ -f "$LOCAL_PROPERTIES_FILE" ]]; then
	_godot_dir_prop=$("$SCRIPT_DIR"/get_config_property.sh -f "$LOCAL_PROPERTIES_FILE" godot.dir)
	if [[ -n "$_godot_dir_prop" ]]; then
		GODOT_DIR=$(eval echo "$_godot_dir_prop")
	fi
	unset _godot_dir_prop
fi

# increase this value using -t option if device is not able to generate all headers before godot build is killed
BUILD_TIMEOUT=40

do_clean=false
do_reset_spm=false
do_remove_godot=false
do_download_godot=false
do_generate_headers=false
do_update_spm=false
do_resolve_spm_dependencies=false
do_debug_build=false
do_release_build=false
do_simulator_build=false
do_create_archive=false
do_uninstall=false
do_install=false


function display_help()
{
	echo
	"$SCRIPT_DIR"/echocolor.sh -y "The " -Y "$0 script" -y " builds the plugin, generates library archives, and"
	echo_yellow "creates a zip file containing all libraries and configuration."
	echo
	echo_yellow "If plugin version is not set with the -z option, then Godot version will be used."
	echo
	"$SCRIPT_DIR"/echocolor.sh -Y "Syntax:"
	echo_yellow "	$0 [-a|A|b|B|c|d|D|g|G|h|H|p|P|r|R|s|t <timeout>]"
	echo
	"$SCRIPT_DIR"/echocolor.sh -Y "Options:"
	echo_yellow "	a	generate godot headers and build plugin"
	echo_yellow "	A	download configured godot version, generate godot headers, and"
	echo_yellow "	 	build plugin"
	echo_yellow "	b	build debug variant of plugin (device); combine with -s for simulator"
	echo_yellow "	B	build release variant of plugin (device); combine with -s for simulator"
	echo_yellow "	c	remove any existing plugin build"
	echo_yellow "	d	uninstall iOS plugin from demo app"
	echo_yellow "	D	install iOS plugin to demo app"
	echo_yellow "	g	remove godot directory"
	echo_yellow "	G	download the configured godot version into godot directory"
	echo_yellow "	h	display usage information"
	echo_yellow "	H	generate godot headers"
	echo_yellow "	p	remove SPM packages and build artifacts"
	echo_yellow "	P	add SPM packages from configuration"
	echo_yellow "	r	resolve SPM dependencies"
	echo_yellow "	R	create iOS release archive"
	echo_yellow "	s	simulator build; use with -b for simulator debug, -B for simulator release"
	echo_yellow "	t	change timeout value for godot build"
	echo
	"$SCRIPT_DIR"/echocolor.sh -Y "Examples:"
	echo_yellow "	* clean existing build, remove godot, and rebuild all"
	echo_yellow "		$> $0 -cgA"
	echo_yellow "		$> $0 -cgpGHPb"
	echo
	echo_yellow "	* clean existing build, remove SPM packages, and rerun debug build"
	echo_yellow "		$> $0 -cpPb"
	echo
	echo_yellow "	* clean existing build and rebuild plugin"
	echo_yellow "		$> $0 -ca"
	echo
	echo_yellow "	* clean existing build and rebuild plugin and create release archive"
	echo_yellow "		$> $0 -R"
	echo
	echo_yellow "	* clean existing build and rebuild plugin with custom build-header timeout"
	echo_yellow "		$> $0 -cHbt 15"
	echo
}


function echo_yellow()
{
	"$SCRIPT_DIR"/echocolor.sh -y "$1"
}


function echo_blue()
{
	"$SCRIPT_DIR"/echocolor.sh -B "$1"
}


function echo_green()
{
	"$SCRIPT_DIR"/echocolor.sh -g "$1"
}


function display_status()
{
	echo
	"$SCRIPT_DIR"/echocolor.sh -c "********************************************************************************"
	"$SCRIPT_DIR"/echocolor.sh -c "* $1"
	"$SCRIPT_DIR"/echocolor.sh -c "********************************************************************************"
	echo
}


function display_progress()
{
	echo_green "$1"
	echo
}


function display_warning()
{
	echo_yellow "Warning: $1"
	echo
}


function display_error()
{
	"$SCRIPT_DIR"/echocolor.sh -r "Error: $1"
}


function generate_godot_headers()
{
	if [[ ! -d "$GODOT_DIR" ]]
	then
		display_error "$GODOT_DIR directory does not exist. Can't generate headers."
		exit 1
	fi

	display_status "Starting Godot build to generate Godot headers..."

	"$SCRIPT_DIR"/run_with_timeout.sh -t "$BUILD_TIMEOUT" -c "scons platform=ios target=template_release" \
		-d "$GODOT_DIR" || true

	display_status "Terminated Godot build after $BUILD_TIMEOUT seconds..."
}


while getopts "aAbBcdDgGhHpPrRst:" option; do
	case $option in
		h)
			display_help
			exit;;
		a)
			do_generate_headers=true
			do_update_spm=true
			do_debug_build=true
			do_release_build=true
			;;
		A)
			do_download_godot=true
			do_generate_headers=true
			do_update_spm=true
			do_debug_build=true
			do_release_build=true
			;;
		b)
			do_debug_build=true
			;;
		B)
			do_release_build=true
			;;
		c)
			do_clean=true
			;;
		d)
			do_uninstall=true
			;;
		D)
			do_install=true
			;;
		g)
			do_remove_godot=true
			;;
		G)
			do_download_godot=true
			;;
		H)
			do_generate_headers=true
			;;
		p)
			do_reset_spm=true
			;;
		P)
			do_update_spm=true
			;;
		r)
			do_resolve_spm_dependencies=true
			;;
		R)
			do_create_archive=true
			;;
		s)
			do_simulator_build=true
			;;
		t)
			regex='^[0-9]+$'
			if ! [[ $OPTARG =~ $regex ]]
			then
				display_error "The argument for the -t option should be an integer. Found $OPTARG."
				echo
				display_help
				exit 1
			else
				BUILD_TIMEOUT=$OPTARG
			fi
			;;
		\?)
			display_error "invalid option"
			echo
			display_help
			exit;;
	esac
done


if [[ "$do_uninstall" == true ]]
then
	display_status "Uninstalling iOS plugin from demo app"
	"$SCRIPT_DIR"/run_gradle_task.sh "uninstalliOS"
fi

if [[ "$do_clean" == true ]]
then
	"$SCRIPT_DIR"/run_gradle_task.sh "cleaniOSBuild"
fi

if [[ "$do_reset_spm" == true ]]
then
	"$SCRIPT_DIR"/run_gradle_task.sh "resetSPMDependencies"
fi

if [[ "$do_remove_godot" == true ]]
then
	"$SCRIPT_DIR"/run_gradle_task.sh "removeGodotDirectory"
fi

if [[ "$do_download_godot" == true ]]
then
	"$SCRIPT_DIR"/run_gradle_task.sh "downloadGodot"
fi

if [[ "$do_generate_headers" == true ]]
then
	if [[ "${INVOKED_BY_GRADLE:-}" == "true" ]]; then
		generate_godot_headers
	else
		"$SCRIPT_DIR"/run_gradle_task.sh "generateGodotHeaders"
	fi
fi

if [[ "$do_update_spm" == true ]]
then
	"$SCRIPT_DIR"/run_gradle_task.sh "updateSPMDependencies"
fi

if [[ "$do_resolve_spm_dependencies" == true ]]
then
	"$SCRIPT_DIR"/run_gradle_task.sh "resolveSPMDependencies"
fi

if [[ "$do_debug_build" == true ]]
then
	if [[ "$do_simulator_build" == true ]]; then
		"$SCRIPT_DIR"/run_gradle_task.sh "buildiOSDebugSimulator"
	else
		"$SCRIPT_DIR"/run_gradle_task.sh "buildiOSDebug"
	fi
fi

if [[ "$do_release_build" == true ]]
then
	if [[ "$do_simulator_build" == true ]]; then
		"$SCRIPT_DIR"/run_gradle_task.sh "buildiOSReleaseSimulator"
	else
		"$SCRIPT_DIR"/run_gradle_task.sh "buildiOSRelease"
	fi
fi

if [[ "$do_create_archive" == true ]]
then
	display_status "Creating iOS archive"
	"$SCRIPT_DIR"/run_gradle_task.sh "createiOSArchive"
fi

if [[ "$do_install" == true ]]
then
	display_status "Installing iOS plugin to demo app"
	"$SCRIPT_DIR"/run_gradle_task.sh "installToDemoiOS"
fi
