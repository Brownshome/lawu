package dev.brownjames.lawu.vulkan;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public record BindingHelper() {
	public static final AddressLayout CHAR_POINTER = ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(JAVA_BYTE));
}
