package dev.brownjames.lawu.vulkan.debugutils;

import dev.brownjames.lawu.vulkan.InstanceFunctionLookup;
import dev.brownjames.lawu.vulkan.Vulkan;
import dev.brownjames.lawu.vulkan.VulkanHandle;
import dev.brownjames.lawu.vulkan.VulkanInstance;
import dev.brownjames.lawu.vulkan.bindings.PFN_vkCreateDebugUtilsMessengerEXT;
import dev.brownjames.lawu.vulkan.bindings.PFN_vkDestroyDebugUtilsMessengerEXT;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collection;

public final class DebugUtilsExtension implements VulkanHandle {
	private final MemorySegment handle, allocator;

	private final PFN_vkCreateDebugUtilsMessengerEXT createDebugUtilsMessenger;
	private final PFN_vkDestroyDebugUtilsMessengerEXT destroyDebugUtilsMessenger;

	public static DebugUtilsExtension extend(VulkanInstance instance) {
		return new DebugUtilsExtension(instance.handle(), instance.allocator(), instance.arena(), instance.instanceFunctionLookup());
	}

	public static String extensionName() {
		return "VK_EXT_debug_utils";
	}

	public DebugUtilsExtension(MemorySegment handle, MemorySegment allocator, Arena arena, InstanceFunctionLookup lookup) {
		this.handle = handle;
		this.allocator = allocator;

		createDebugUtilsMessenger = lookup.lookup("vkCreateDebugUtilsMessengerEXT")
				.map(address -> PFN_vkCreateDebugUtilsMessengerEXT.ofAddress(address, arena))
				.orElseThrow();

		destroyDebugUtilsMessenger = lookup.lookup("vkDestroyDebugUtilsMessengerEXT")
				.map(address -> PFN_vkDestroyDebugUtilsMessengerEXT.ofAddress(address, arena))
				.orElseThrow();
	}

	@Override
	public MemorySegment handle() {
		return handle;
	}

	public DebugUtilsMessenger createDebugUtilsMessenger(Collection<DebugUtilsMessageSeverity> severities, Collection<DebugUtilsMessageType> types, DebugUtilsMessengerCallback callback) {
		return createDebugUtilsMessenger(DebugUtilsMessengerCreateInfo.of(severities, types, callback));
	}

	public DebugUtilsMessenger createDebugUtilsMessenger(DebugUtilsMessengerCreateInfo createInfo) {
		try (var arena = Arena.ofConfined()) {
			return createDebugUtilsMessenger(createInfo.asRaw(arena));
		}
	}

	public DebugUtilsMessenger createDebugUtilsMessenger(MemorySegment debugUtilsMessengerCreateInfo) {
		try (var arena = Arena.ofConfined()) {
			var result = arena.allocate(vulkan_h.VkDebugUtilsMessengerEXT);
			Vulkan.checkResult(createDebugUtilsMessenger.apply(handle, debugUtilsMessengerCreateInfo, allocator, result));
			return new DebugUtilsMessenger(this, result.get(vulkan_h.VkDebugUtilsMessengerEXT, 0));
		}
	}

	public void destroyDebugUtilsMessenger(MemorySegment messenger) {
		destroyDebugUtilsMessenger.apply(handle, messenger, allocator);
	}
}
