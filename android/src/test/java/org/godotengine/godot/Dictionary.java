//
// © 2026-present Godot Mobile Plugins (https://github.com/godot-mobile-plugins)
//

package org.godotengine.godot;

import java.util.HashMap;

/**
 * Minimal stub that lets unit tests create and interrogate Dictionary instances
 * without the godot-lib AAR being present on the local JVM test classpath.
 */
public class Dictionary extends HashMap<String, Object> {
	public Dictionary() {
		super();
	}
}
