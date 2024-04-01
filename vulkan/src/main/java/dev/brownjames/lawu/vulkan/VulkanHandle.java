package dev.brownjames.lawu.vulkan;

import java.lang.foreign.MemorySegment;

public interface VulkanHandle {

	MemorySegment handle();
}
