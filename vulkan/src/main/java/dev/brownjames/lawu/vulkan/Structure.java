/*
 * Copyright James Brown 2023
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.function.Function;

import dev.brownjames.lawu.vulkan.bindings.VkBaseInStructure;

/**
 * A structure representing a native type
 */
public interface Structure {
	interface WithNext<T extends NextStructure<T>> extends Structure {
		interface Native<T extends NextStructure<T>> extends WithNext<T>, Structure.Native {
			private void addNext(Iterable<? extends T> chain, Function<? super T, ? extends NextStructure.Native<T>> asNative) {
				MemorySegment oldNext = VkBaseInStructure.pNext$get(raw());
				WithNext.Native<T> head = this;

				for (var item : chain) {
					NextStructure.Native<T> next = asNative.apply(item);
					VkBaseInStructure.pNext$set(head.raw(), next.raw());
					head = next;
				}

				VkBaseInStructure.pNext$set(head.raw(), oldNext);
			}

			default void addNext(Iterable<? extends T> chain) {
				addNext(chain, NextStructure::asNative);
			}

			/**
			 * Links a series of supplied next items into a valid pNext chain and sets them on this object. Note, if the
			 * supplied objects are {@link Structure.Native} then their memory will be written to, updating the pNext field.
			 * @param chain the items to link into a chain
			 */
			default void addNext(SegmentAllocator allocator, Iterable<? extends T> chain) {
				addNext(chain, item -> item.asNative(allocator));
			}

			@Override
			default WithNext.Native<T> asNative() {
				return this;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		default Native<T> asNative() {
			return (Native<T>) Structure.super.asNative();
		}

		@Override
		@SuppressWarnings("unchecked")
		default Native<T> asNative(SegmentAllocator allocator) {
			return (Native<T>) Structure.super.asNative(allocator);
		}
	}

	/**
	 * A view into native memory
	 */
	interface Native extends Structure {
		MemorySegment raw();

		@Override
		default void asRaw(MemorySegment destination, SegmentAllocator allocator) {
			assert destination != null;
			assert allocator != null;
			destination.copyFrom(raw());
		}

		@Override
		default MemorySegment asRaw() {
			return raw();
		}

		@Override
		default Native asNative() {
			return this;
		}
	}

	/**
	 * The structure represented as a Java object
	 */
	interface Value extends Structure {
		@Override
		default Value asValue() {
			return this;
		}
	}

	/**
	 * If not already, converts and returns this structure as a view into native memory
	 * @return a view of this object into native memory
	 */
	default Native asNative() {
		return asNative(Arena.ofAuto());
	}

	/**
	 * If not already, converts and returns this structure as a view into native memory
	 *
	 * @param allocator thd allocator to use if required
	 *
	 * @return a view of this object into native memory
	 */
	default Native asNative(SegmentAllocator allocator) {
		assert allocator != null;
		return asNative();
	}

	/**
	 * If not already, converts and returns this structure as a Java object
	 * @return a Java object
	 */
	Value asValue();

	/**
	 * Copies data from this structure into the given destination.
	 *
	 * @param destination the memory segment to copy data into
	 * @param allocator the allocator to use for any allocations needed
	 */
	void asRaw(MemorySegment destination, SegmentAllocator allocator);

	/**
	 * Creates a memory segment from this structure
	 *
	 * @param allocator the segment allocator to allocate from
	 *
	 * @return an allocated memory-segment
	 */
	default MemorySegment asRaw(SegmentAllocator allocator) {
		assert allocator != null;
		return asRaw();
	}

	/**
	 * Creates a memory segment from this structure allocated from an auto arena
	 *
	 * @return an allocated memory segment
	 */
	default MemorySegment asRaw() {
		return asRaw(Arena.ofAuto());
	}
}
