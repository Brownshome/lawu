package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.VkImageFormatProperties;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.Set;

public record ImageFormatProperties(
		Extent3d maxExtent,
		int maxMipLevels,
		int maxArrayLayers,
		Set<SampleCountFlag> sampleCounts,
		long maxResourceSize
) {
	public static ImageFormatProperties of(MemorySegment raw) {
		return new ImageFormatProperties(
				Extent3d.of(VkImageFormatProperties.maxExtent$slice(raw)),
				VkImageFormatProperties.maxMipLevels$get(raw),
				VkImageFormatProperties.maxArrayLayers$get(raw),
				BitFlag.flags(VkImageFormatProperties.sampleCounts$get(raw), SampleCountFlag.class),
				VkImageFormatProperties.maxResourceSize$get(raw)
		);
	}

	public void asNative(MemorySegment destination) {
		maxExtent.asNative(VkImageFormatProperties.maxExtent$slice(destination));
		VkImageFormatProperties.maxMipLevels$set(destination, maxMipLevels);
		VkImageFormatProperties.maxArrayLayers$set(destination, maxArrayLayers);
		VkImageFormatProperties.sampleCounts$set(destination, BitFlag.getFlagBits(sampleCounts));
		VkImageFormatProperties.maxResourceSize$set(destination, maxResourceSize);
	}

	public MemorySegment asNative(SegmentAllocator allocator) {
		var raw = VkImageFormatProperties.allocate(allocator);
		asNative(raw);
		return raw;
	}

	public MemorySegment asNative() {
		return asNative(Arena.ofAuto());
	}
}
