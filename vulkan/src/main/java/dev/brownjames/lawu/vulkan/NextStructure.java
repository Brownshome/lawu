package dev.brownjames.lawu.vulkan;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A structure in a pNext chain
 */
public interface NextStructure {
	record NativeStructureChain(List<MemorySegment> items, MemorySegment head) { }

	static NativeStructureChain buildNativeStructureChain(Arena arena, Collection<? extends NextStructure> nexts) {
		var items = new ArrayList<MemorySegment>(nexts.size());
		var head = MemorySegment.NULL;

		for (var next : nexts) {
			head = next.createNativeStructure(arena, head);
			items.add(head);
		}

		return new NativeStructureChain(items, head);
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
