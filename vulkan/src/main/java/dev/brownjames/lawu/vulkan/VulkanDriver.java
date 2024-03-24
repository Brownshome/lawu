package dev.brownjames.lawu.vulkan;

import java.lang.foreign.MemorySegment;

/**
 * A driver which can be loaded using VK_LUNARG_direct_driver_loading
 */
public interface VulkanDriver {
	/**
	 * Gets a function pointer into the driver
	 * @return the getInstanceProcAddress function for this driver
	 */
	MemorySegment getInstanceProcAddressPointer();
}
