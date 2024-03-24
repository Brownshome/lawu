package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.VkExtensionProperties;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public record VulkanExtensionProperties(String name, int version) {
	public static List<VulkanExtensionProperties> all() {
		try (var arena = Arena.ofConfined()) {
			var propertyCount = arena.allocate(vulkan_h.uint32_t);

			var result = vulkan_h.vkEnumerateInstanceExtensionProperties(MemorySegment.NULL, propertyCount, MemorySegment.NULL);
			Vulkan.checkResult(result);

			var properties = VkExtensionProperties.allocateArray(propertyCount.get(vulkan_h.uint32_t, 0), arena);
			result = vulkan_h.vkEnumerateInstanceExtensionProperties(MemorySegment.NULL, propertyCount, properties);
			Vulkan.checkResult(result);

			return properties.elements(VkExtensionProperties.$LAYOUT())
					.map(VulkanExtensionProperties::from)
					.toList();
		}
	}

	public static VulkanExtensionProperties from(MemorySegment segment) {
		return new VulkanExtensionProperties(
				VkExtensionProperties.extensionName$slice(segment).getUtf8String(0),
				VkExtensionProperties.specVersion$get(segment));
	}
}
