package dev.brownjames.lawu.vulkan;

import de.skuzzle.semantic.Version;
import dev.brownjames.lawu.vulkan.bindings.VkDebugUtilsMessengerCallbackDataEXT;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.util.EnumSet;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class VulkanInstanceTest implements DebugUtilsMessengerCallback {
	private static final System.Logger LOGGER = System.getLogger(String.valueOf(VulkanInstanceTest.class.getModule().getName()));

	private VulkanInstance.Builder testInstanceBuilder() {
		return VulkanInstance.builder()
			.withDebugCallback(BitFlag.allFlags(DebugUtilsMessageSeverity.class),
						BitFlag.allFlags(DebugUtilsMessageType.class),
						this);
	}

	@Test
	void createInstance() {
		var v = testInstanceBuilder()
				.withApplicationName("create")
				.withApplicationVersion(Version.create(0, 1))
				.withEngineName("in-house")
				.withEngineVersion(Version.create(0, 1))
				.build();

		v.close();
	}

	@Test
	void allPhysicalDevices() {
		try (var vulkan = testInstanceBuilder().build()) {
			var devices = vulkan.allPhysicalDevices();

			assertFalse(devices.isEmpty());
		}
	}

	@Test
	void directDriverLoading() {
		var vulkan = testInstanceBuilder().withDrivers(DirectDriverLoadingMode.EXCLUSIVE, new TestVulkanDriver()).build();
		vulkan.close();
	}

	@Override
	public void callback(DebugUtilsMessageSeverity severity, int messageTypes, MemorySegment callbackData) {
		var message = VkDebugUtilsMessengerCallbackDataEXT.pMessage$get(callbackData).getUtf8String(0);

		LOGGER.log(switch (severity) {
			case VERBOSE -> System.Logger.Level.DEBUG;
			case INFO -> System.Logger.Level.INFO;
			case WARNING -> System.Logger.Level.WARNING;
			case ERROR -> System.Logger.Level.ERROR;
		}, "Vulkan Debug Message: {0} (type: {1})", message, messageTypes);
	}
}
