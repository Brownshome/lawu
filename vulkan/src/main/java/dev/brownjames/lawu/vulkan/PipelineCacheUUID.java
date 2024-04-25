package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.annotation.MapStructure;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.MemorySegment;

@MapStructure("uint32_t pipelineCacheUUID")
public record PipelineCacheUUID(byte[] value) {
	public PipelineCacheUUID {
		assert value.length == vulkan_h.VK_UUID_SIZE();
	}

	public static PipelineCacheUUID of(MemorySegment raw) {
		return new PipelineCacheUUID(raw.toArray(vulkan_h.uint8_t));
	}

	public void asNative(MemorySegment destination) {
		assert destination.byteSize() == value.length;
		destination.copyFrom(MemorySegment.ofArray(value));
	}

	public MemorySegment asNative() {
		return MemorySegment.ofArray(value);
	}
}
