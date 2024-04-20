package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.VkExtent3D;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

public record Extent3d(int width, int height, int depth) {
	public static Extent3d of(MemorySegment raw) {
		return new Extent3d(VkExtent3D.width$get(raw), VkExtent3D.height$get(raw), VkExtent3D.depth$get(raw));
	}

	public void asNative(MemorySegment destination) {
		VkExtent3D.width$set(destination, width);
		VkExtent3D.height$set(destination, height);
		VkExtent3D.depth$set(destination, depth);
	}

	public MemorySegment asNative(SegmentAllocator arena) {
		var raw = VkExtent3D.allocate(arena);
		asNative(raw);
		return raw;
	}

	public MemorySegment asNative() {
		return asNative(Arena.ofAuto());
	}
}
