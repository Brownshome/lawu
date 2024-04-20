package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public record PipelineCacheUUID(byte[] value) {
	public PipelineCacheUUID {
		assert value.length == vulkan_h.VK_UUID_SIZE();
	}

	public static PipelineCacheUUID of(MemorySegment raw) {
		return new PipelineCacheUUID(raw.toArray(ValueLayout.JAVA_BYTE));
	}

	public void asNative(MemorySegment destination) {
		assert destination.byteSize() == value.length;
		destination.copyFrom(MemorySegment.ofArray(value));
	}

	public MemorySegment asNative() {
		return MemorySegment.ofArray(value);
	}
}
