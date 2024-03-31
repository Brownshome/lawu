package dev.brownjames.lawu.vulkan.debugutils;

import dev.brownjames.lawu.vulkan.VulkanHandle;

import java.lang.foreign.MemorySegment;

public record DebugUtilsMessenger(DebugUtilsExtension instance, MemorySegment handle) implements VulkanHandle, AutoCloseable {
	@Override
	public void close() {
		instance.destroyDebugUtilsMessenger(handle);
	}
}
