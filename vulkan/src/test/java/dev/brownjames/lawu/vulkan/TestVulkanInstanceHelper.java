package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.VkDebugUtilsMessengerCallbackDataEXT;
import dev.brownjames.lawu.vulkan.debugutils.*;

public final class TestVulkanInstanceHelper {
	private static final System.Logger LOGGER = System.getLogger(String.valueOf(TestVulkanInstanceHelper.class.getModule().getName()));

	private static final DebugUtilsMessengerCreateInfo createInfo = DebugUtilsMessengerCreateInfo.of(
			BitFlag.allFlags(DebugUtilsMessageSeverity.class),
			BitFlag.allFlags(DebugUtilsMessageType.class),
			(severity, messageTypes, callbackData) -> {
				var message = VkDebugUtilsMessengerCallbackDataEXT.pMessage$get(callbackData).getUtf8String(0);

				LOGGER.log(switch (severity) {
					case VERBOSE -> System.Logger.Level.DEBUG;
					case INFO -> System.Logger.Level.INFO;
					case WARNING -> System.Logger.Level.WARNING;
					case ERROR -> System.Logger.Level.ERROR;
				}, "Vulkan Debug Message: {0} (type: {1})", message, messageTypes);
			});

	public static InstanceCreateInfo builder() {
		return VulkanInstance.builder()
				.withApplicationInfo(new ApplicationInfo().withApiVersion(VulkanVersionNumber.headerVersion()))
				.withDebugCallback(createInfo)
				.withValidationLayers();
	}

	public static DebugUtilsMessenger createMessenger(DebugUtilsExtension instance) {
		return instance.createDebugUtilsMessenger(createInfo);
	}
}
