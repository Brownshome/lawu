package dev.brownjames.lawu.bindings;

import java.lang.foreign.Arena;

public final class Vulkan {
	public VulkanInstance createInstance() {
		try (var arena = Arena.ofConfined()) {
			var applicationInformation = VkApplicationInfo.allocate(arena);
		}
	}
}
