package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

public record LayerProperties(
		String name,
		VulkanVersionNumber specificationVersion,
		int implementationVersion,
		String description
) {
	private static final PFN_vkEnumerateInstanceLayerProperties enumerateInstanceLayerProperties = Vulkan.globalFunctionLookup()
			.lookup("vkEnumerateInstanceLayerProperties")
			.map(address -> PFN_vkEnumerateInstanceLayerProperties.ofAddress(address, Arena.global()))
			.orElseThrow();

	public static List<LayerProperties> all() {
		try (var arena = Arena.ofConfined()) {
			var propertyCount = arena.allocate(vulkan_h.uint32_t);
			Vulkan.checkResult(enumerateInstanceLayerProperties.apply(propertyCount, MemorySegment.NULL));

			var properties = VkLayerProperties.allocateArray(propertyCount.get(vulkan_h.uint32_t, 0), arena);
			int result = Vulkan.checkResult(enumerateInstanceLayerProperties.apply(propertyCount, properties));
			assert result == vulkan_h.VK_SUCCESS();

			return properties.elements(VkLayerProperties.$LAYOUT())
					.map(LayerProperties::from)
					.toList();
		}
	}

	public static LayerProperties from(MemorySegment segment) {
		return new LayerProperties(
				VkLayerProperties.layerName$slice(segment).getUtf8String(0),
				VulkanVersionNumber.of(VkLayerProperties.specVersion$get(segment)),
				VkLayerProperties.implementationVersion$get(segment),
				VkLayerProperties.description$slice(segment).getUtf8String(0));
	}
}
