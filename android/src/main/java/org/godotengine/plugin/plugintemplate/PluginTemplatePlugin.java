//
// © 2026-present https://github.com/<<GitHubUsername>>
//

package org.godotengine.plugin.plugintemplate;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.godotengine.godot.Godot;
import org.godotengine.godot.Dictionary;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;


/**
 * Godot Android plugin entry point.
 *
 * <p>Notes for plugin authors targeting Godot 4.8+:
 * <ul>
 *   <li>Lifecycle: {@code onMainCreate}, {@code onMain*} and the activity callbacks run on the
 *       host (UI) thread; {@code onGodotSetupCompleted}, {@code onGodotMainLoopStarted} and
 *       {@code onGodotTerminating} run on the render thread. Use {@code runOnHostThread} /
 *       {@code runOnRenderThread} to switch.</li>
 *   <li>{@code Godot.alert()} (and {@code OS.alert()} from GDScript) now blocks the render thread
 *       until the dialog is dismissed. Do not call it from the render thread, and expect frame
 *       rendering to pause while it is shown.</li>
 *   <li>Remote dependencies returned by the editor plugin's {@code _get_android_dependencies()}
 *       are resolved inside the export project's {@code fusedDepsLib} module
 *       (com.android.fused-library), not in the app module. Only {@code platform()} /
 *       {@code enforcedPlatform()} entries stay in the app module.</li>
 *   <li>With the "gradle_build/minification" export option, R8 keeps every GodotPlugin subclass
 *       and every {@code @UsedByGodot} member. Anything else reached via reflection needs a rule
 *       in {@code android/config/consumer-rules.pro}.</li>
 * </ul>
 */
public class PluginTemplatePlugin extends GodotPlugin {
	public static final String CLASS_NAME = PluginTemplatePlugin.class.getSimpleName();
	static final String LOG_TAG = "godot::" + CLASS_NAME;


	static final String TEMPLATE_READY_SIGNAL = "template_ready";

	// TODO: Define all signals

	public PluginTemplatePlugin(Godot godot) {
		super(godot);
	}

	@Override
	public String getPluginName() {
		return CLASS_NAME;
	}

	@Override
	public Set<SignalInfo> getPluginSignals() {
		Set<SignalInfo> signals = new HashSet<>();
		signals.add(new SignalInfo(TEMPLATE_READY_SIGNAL, Dictionary.class));

		// TODO: Register all signals

		return signals;
	}

	@Override
	public View onMainCreate(Activity activity) {
		// TODO: Godot activity is ready

		return super.onMainCreate(activity);
	}

	@Override
	public void onGodotSetupCompleted() {
		super.onGodotSetupCompleted();

		// TODO: Godot is ready
	}

	@UsedByGodot
	public Object[] get_plugin_template() {
		Log.d(LOG_TAG, "get_plugin_template() invoked");

		List<Dictionary> resultList = new ArrayList<>();

		// TODO: Plugin method

		return resultList.toArray();
	}

	@Override
	public void onMainDestroy() {
		// TODO: Plugin cleanup
	}
}
