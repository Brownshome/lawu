package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.PFN_vkEnumerateInstanceExtensionProperties;
import dev.brownjames.lawu.vulkan.bindings.VkExtensionProperties;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public record ExtensionProperties(String name, int version) {
	private static final PFN_vkEnumerateInstanceExtensionProperties enumerateInstanceExtensionProperties = Vulkan.globalFunctionLookup()
			.lookup("vkEnumerateInstanceExtensionProperties")
			.map(address -> PFN_vkEnumerateInstanceExtensionProperties.ofAddress(address, Arena.global()))
			.orElseThrow();

	public static List<ExtensionProperties> vulkanOrImplicit() {
		return forLayer(MemorySegment.NULL);
	}

	public static List<ExtensionProperties> forLayer(String layerName) {
		try (var arena = Arena.ofConfined()) {
			return forLayer(arena.allocateUtf8String(layerName));
		}
	}

	public static List<ExtensionProperties> forLayer(MemorySegment layerName) {
		try (var arena = Arena.ofConfined()) {
			var propertyCount = arena.allocate(vulkan_h.uint32_t);

			/*
			 * @note james.brown 31 March 2024
			 * The layers available and the versions of those layers may change; confirm that we have
			 * all the extensions before moving on.
			 */
			MemorySegment properties;
			do {
				Vulkan.checkResult(enumerateInstanceExtensionProperties.apply(layerName, propertyCount, MemorySegment.NULL));
				properties = VkExtensionProperties.allocateArray(propertyCount.get(vulkan_h.uint32_t, 0), arena);
			} while (Vulkan.checkResult(enumerateInstanceExtensionProperties.apply(layerName, propertyCount, properties)) == vulkan_h.VK_INCOMPLETE());

			return properties.elements(VkExtensionProperties.$LAYOUT())
					.map(ExtensionProperties::from)
					.toList();
		}
	}

	public static ExtensionProperties from(MemorySegment segment) {
		return new ExtensionProperties(
				VkExtensionProperties.extensionName$slice(segment).getUtf8String(0),
				VkExtensionProperties.specVersion$get(segment));
	}
}
