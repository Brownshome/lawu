package dev.brownjames.lawu.bindings;

import dev.brownjames.lawu.bindings.VkExtensionProperties;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import static dev.brownjames.lawu.bindings.vulkan_h.*;

public record VulkanExtensionProperties(String name, int version) {
	public static List<VulkanExtensionProperties> all() {
		try (var arena = Arena.openConfined()) {
			var propertyCount = arena.allocate(uint32_t);

			var result = vkEnumerateInstanceExtensionProperties(MemorySegment.NULL, propertyCount, MemorySegment.NULL);
			Vulkan.checkResult(result);

			var properties = VkExtensionProperties.allocateArray(propertyCount.get(uint32_t, 0), arena);
			result = vkEnumerateInstanceExtensionProperties(MemorySegment.NULL, propertyCount, properties);
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
