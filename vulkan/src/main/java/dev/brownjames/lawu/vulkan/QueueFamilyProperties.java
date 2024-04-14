/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.VkQueueFamilyProperties;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.Set;

public record QueueFamilyProperties(
		Set<QueueFlag> queueFlags,
		int queueCount,
		int timestampValidBits,
		Extent3d minImageTransferGranularity
) {
	public static QueueFamilyProperties of(MemorySegment raw) {
		return new QueueFamilyProperties(
				BitFlag.flags(VkQueueFamilyProperties.queueFlags$get(raw), QueueFlag.class),
				VkQueueFamilyProperties.queueCount$get(raw),
				VkQueueFamilyProperties.timestampValidBits$get(raw),
				Extent3d.of(VkQueueFamilyProperties.minImageTransferGranularity$slice(raw))
		);
	}

	public void asNative(MemorySegment destination) {
		VkQueueFamilyProperties.queueFlags$set(destination, BitFlag.getFlagBits(queueFlags));
		VkQueueFamilyProperties.queueCount$set(destination, queueCount);
		VkQueueFamilyProperties.timestampValidBits$set(destination, timestampValidBits);
		minImageTransferGranularity.asNative(VkQueueFamilyProperties.minImageTransferGranularity$slice(destination));
	}

	public MemorySegment asNative(SegmentAllocator allocator) {
		var result = VkQueueFamilyProperties.allocate(allocator);
		asNative(result);
		return result;
	}

	public MemorySegment asNative() {
		return asNative(Arena.ofAuto());
	}
}
