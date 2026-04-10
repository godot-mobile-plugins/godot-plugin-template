//
// © 2026-present https://github.com/<<GitHubUsername>>
//

package org.godotengine.plugin.plugintemplate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import android.app.Activity;
import android.view.View;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.SignalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class PluginTemplatePluginTest {

	@Mock
	private Godot mockGodot;

	@Mock
	private Activity mockActivity;

	private PluginTemplatePlugin plugin;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	@BeforeEach
	public void setUp() {
		plugin = new PluginTemplatePlugin(mockGodot);
	}

	// -------------------------------------------------------------------------
	// Plugin identity
	// -------------------------------------------------------------------------

	@Test
	public void getPluginName_returnsSimpleClassName() {
		String name = plugin.getPluginName();
		assertEquals("PluginTemplatePlugin", name);
	}

	@Test
	public void getPluginName_matchesClassNameConstant() {
		assertEquals(PluginTemplatePlugin.CLASS_NAME, plugin.getPluginName());
	}

	@Test
	public void logTag_hasExpectedFormat() {
		assertEquals("godot::PluginTemplatePlugin", PluginTemplatePlugin.LOG_TAG);
	}

	// -------------------------------------------------------------------------
	// Signal registration
	// -------------------------------------------------------------------------

	@Test
	public void getPluginSignals_isNotNull() {
		assertNotNull(plugin.getPluginSignals());
	}

	@Test
	public void getPluginSignals_isNotEmpty() {
		assertFalse(plugin.getPluginSignals().isEmpty());
	}

	@Test
	public void getPluginSignals_containsTemplateReadySignal() {
		Set<SignalInfo> signals = plugin.getPluginSignals();
		boolean found = signals.stream()
				.anyMatch(s -> s.getName().equals(PluginTemplatePlugin.TEMPLATE_READY_SIGNAL));
		assertTrue(found, "Expected 'template_ready' signal to be registered");
	}

	@Test
	public void templateReadySignal_nameMatchesConstant() {
		assertEquals("template_ready", PluginTemplatePlugin.TEMPLATE_READY_SIGNAL);
	}

	@Test
	public void templateReadySignal_hasDictionaryParam() throws Exception {
		Set<SignalInfo> signals = plugin.getPluginSignals();
		SignalInfo info = signals.stream()
				.filter(s -> s.getName().equals(PluginTemplatePlugin.TEMPLATE_READY_SIGNAL))
				.findFirst()
				.orElse(null);
		assertNotNull(info, "Signal 'template_ready' not found in registry");

		// getParamTypes() is package-private; access via reflection.
		Method getParamTypes = SignalInfo.class.getDeclaredMethod("getParamTypes");
		getParamTypes.setAccessible(true);
		Class<?>[] paramTypes = (Class<?>[]) getParamTypes.invoke(info);

		assertArrayEquals(new Class<?>[]{Dictionary.class}, paramTypes);
	}

	// -------------------------------------------------------------------------
	// Lifecycle – onMainCreate
	// -------------------------------------------------------------------------

	@Test
	public void onMainCreate_withNullActivity_doesNotThrow() {
		assertDoesNotThrow(() -> plugin.onMainCreate(null));
	}

	@Test
	public void onMainCreate_withMockActivity_returnsViewOrNull() {
		View result = plugin.onMainCreate(mockActivity);
		// Default template returns whatever super returns; assert no unexpected type.
		assertTrue(result == null || result instanceof View);
	}

	// -------------------------------------------------------------------------
	// Lifecycle – onGodotSetupCompleted
	// -------------------------------------------------------------------------

	@Test
	public void onGodotSetupCompleted_doesNotThrow() {
		assertDoesNotThrow(() -> plugin.onGodotSetupCompleted());
	}

	// -------------------------------------------------------------------------
	// Lifecycle – onMainDestroy
	// -------------------------------------------------------------------------

	@Test
	public void onMainDestroy_doesNotThrow() {
		assertDoesNotThrow(() -> plugin.onMainDestroy());
	}

	// -------------------------------------------------------------------------
	// get_plugin_template()
	// -------------------------------------------------------------------------

	@Test
	public void getPluginTemplate_returnsNonNullArray() {
		Object[] result = plugin.get_plugin_template();
		assertNotNull(result);
	}

	@Test
	public void getPluginTemplate_returnsEmptyArrayByDefault() {
		Object[] result = plugin.get_plugin_template();
		assertEquals(
				0,
				result.length,
				"Default template implementation should return an empty array"
		);
	}

	@Test
	public void getPluginTemplate_isIdempotent() {
		Object[] first  = plugin.get_plugin_template();
		Object[] second = plugin.get_plugin_template();
		assertArrayEquals(
				first,
				second,
				"Repeated calls to get_plugin_template() should return equal results"
		);
	}

	@Test
	public void getPluginTemplate_returnsObjectArray() {
		Object[] result = plugin.get_plugin_template();
		assertNotNull(result.getClass().getComponentType());
		assertEquals(Object.class, result.getClass().getComponentType());
	}

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	@Test
	public void constructor_withMockGodot_doesNotThrow() {
		PluginTemplatePlugin newPlugin =
				assertDoesNotThrow(() -> new PluginTemplatePlugin(mockGodot));
		assertNotNull(newPlugin);
	}

	@Test
	public void constructor_producesIndependentInstances() {
		PluginTemplatePlugin a = new PluginTemplatePlugin(mockGodot);
		PluginTemplatePlugin b = new PluginTemplatePlugin(mockGodot);
		assertNotSame(a, b);
	}

	// -------------------------------------------------------------------------
	// Full lifecycle sequence
	// -------------------------------------------------------------------------

	@Test
	public void fullLifecycleSequence_doesNotThrow() {
		assertDoesNotThrow(() -> {
			plugin.onMainCreate(mockActivity);
			plugin.onGodotSetupCompleted();
			plugin.get_plugin_template();
			plugin.onMainDestroy();
		});
	}
}
