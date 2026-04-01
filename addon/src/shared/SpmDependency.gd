#
# © 2026-present https://github.com/<<GitHubUsername>>
#

class_name SpmDependency extends RefCounted

const URL_PROPERTY:= &"url"
const VERSION_PROPERTY:= &"version"
const PRODUCTS_PROPERTY:= &"products"

var _data: Dictionary


func _init(a_data: Dictionary):
    _data = a_data


func get_url() -> String:
    return _data[URL_PROPERTY]


func get_version() -> String:
    return _data[VERSION_PROPERTY]


func get_products() -> Array:
    return _data[PRODUCTS_PROPERTY]


func format_to_string() -> String:
    var __result: String = ""

    for __product in get_products():
        __result += "%s@%s (%s) " % [__product, get_version(), get_url()]

    return __result
