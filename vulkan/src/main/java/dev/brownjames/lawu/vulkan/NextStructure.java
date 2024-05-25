/*
 * Copyright James Brown 2023
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

import dev.brownjames.lawu.vulkan.annotation.MapStructure;
import dev.brownjames.lawu.vulkan.bindings.VkBaseInStructure;

/**
 * A structure in a pNext chain
 */
@MapStructure("VkBaseInStructure")
@MapStructure("VkBaseOutStructure")
public interface NextStructure<T extends NextStructure<T>> extends Structure.WithNext<T> {
	/**
	 * A helper method to create a memory segment for a structure chain from a given collection of nexts
	 * @param allocator the allocator to use
	 * @param nexts the nexts to chain together
	 * @return a memory segment containing the head of the created chain, or a null segment
	 * @param <T> the type of item in this chain
	 */
	static <T extends NextStructure<T>> MemorySegment asRaw(SegmentAllocator allocator, Iterable<? extends T> nexts) {
		for (var head : nexts) {
			var nativeHead = head.asNative(allocator);
			nativeHead.addNext(allocator, nexts);
			return nativeHead.asRaw();
		}

		return MemorySegment.NULL;
	}

	interface Native<T extends NextStructure<T>> extends NextStructure<T>, WithNext.Native<T> {
		@Override
		default StructureType sType() {
			return StructureType.of(VkBaseInStructure.sType$get(raw()));
		}

		@Override
		default NextStructure.Native<T> asNative() {
			return this;
		}
	}

	/**
	 * Gets the structure-type of this next instance
	 * @return the structure type
	 */
	StructureType sType();

	@Override
	default NextStructure.Native<T> asNative() {
		return (NextStructure.Native<T>) WithNext.super.asNative();
	}

	@Override
	default NextStructure.Native<T> asNative(SegmentAllocator allocator) {
		return (NextStructure.Native<T>) WithNext.super.asNative(allocator);
	}
}
