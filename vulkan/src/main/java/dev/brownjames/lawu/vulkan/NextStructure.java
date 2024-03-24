package dev.brownjames.lawu.vulkan;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collection;

/**
 * A structure in a pNext chain
 */
public interface NextStructure {
	static MemorySegment buildNativeStructureChain(Arena arena, Collection<? extends NextStructure> nexts) {
		var head = MemorySegment.NULL;
		for (var next : nexts) {
			head = next.createNativeStructure(arena, head);
		}

		return head;
	}

	/**
	 * Creates a native structure
	 *
	 * @param arena the arena to allocate it from
	 * @param next  the next item in the chain
	 * @return an allocated native structure
	 */
	MemorySegment createNativeStructure(Arena arena, MemorySegment next);
}
