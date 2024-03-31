package dev.brownjames.lawu.vulkan;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;

@FunctionalInterface
public interface FunctionLookup {
	Optional<MemorySegment> lookup(String name);

	@FunctionalInterface
	interface FromMemorySegment extends FunctionLookup {
		Optional<MemorySegment> lookup(MemorySegment name);

		@Override
		default Optional<MemorySegment> lookup(String name) {
			try (var arena = Arena.ofConfined()) {
				return lookup(arena.allocateUtf8String(name));
			}
		}
	}
}
