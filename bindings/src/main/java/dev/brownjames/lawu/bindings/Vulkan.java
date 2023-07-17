package dev.brownjames.lawu.bindings;

import de.skuzzle.semantic.Version;

import java.lang.foreign.Arena;

import static dev.brownjames.lawu.bindings.vulkan_h.*;

public final class Vulkan {
	public static void checkResult(int result) {
		if (result != VK_SUCCESS()) {
			throw new VulkanException(result);
		}
	}

	public static VulkanVersionNumber getInstanceVersion() {
		try (var arena = Arena.openConfined()) {
			var version = arena.allocate(uint32_t);
			var result = vkEnumerateInstanceVersion(version);
			checkResult(result);

			return VulkanVersionNumber.of(version.get(uint32_t, 0L));
		}
	}
}
