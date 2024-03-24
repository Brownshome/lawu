package dev.brownjames.lawu.vulkan;

import java.lang.foreign.Arena;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public final class Vulkan {
	public static void checkResult(int result) {
		if (result != vulkan_h.VK_SUCCESS()) {
			throw new VulkanException(result);
		}
	}

	public static VulkanVersionNumber getInstanceVersion() {
		try (var arena = Arena.ofConfined()) {
			var version = arena.allocate(vulkan_h.uint32_t);
			var result = vulkan_h.vkEnumerateInstanceVersion(version);
			checkResult(result);

			return VulkanVersionNumber.of(version.get(vulkan_h.uint32_t, 0L));
		}
	}
}
