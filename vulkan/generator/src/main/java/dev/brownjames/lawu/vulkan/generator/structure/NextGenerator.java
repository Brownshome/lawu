/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan.generator.structure;

import java.lang.foreign.SegmentAllocator;
import java.util.List;

import dev.brownjames.lawu.vulkan.generator.GenerationFailedException;

import static java.lang.StringTemplate.RAW;

/**
 * A class for generating pNext structure classes and helpers
 */
final class NextGenerator {
	static List<StringTemplate> generateNextStructure(StructureGenerationRequest request) throws GenerationFailedException {
		var context = StructureGenerator.getContext();

		var nextStructure = context.lookup().nextStructure()
				.orElseThrow(() -> new GenerationFailedException("NextStructure type not found", request.target()));
		var nextStructureNative = context.lookup().nextStructureNative()
				.orElseThrow(() -> new GenerationFailedException("NextStructure.Native type not found", request.target()));
		var structureTypeMapping = context.mapping("VkStructureType")
				.orElseThrow(() -> new GenerationFailedException("No suitable type for VkStructureType found", request.target()));

		var nextInterface =
				RAW."""
					public interface Next extends \{nextStructure}<Next> {
						interface Native extends \{nextStructure}.Native<Next>, Next {
							@Override
							default Next.Native asNative() {
								return this;
							}

							@Override
							default Next.Native asNative(\{SegmentAllocator.class} allocator) {
								return this;
							}
						}

						@Override
						default Next.Native asNative() {
							return (Native) \{nextStructure}.super.asNative();
						}

						@Override
						default Next.Native asNative(\{SegmentAllocator.class} allocator) {
							return (Native) \{nextStructure}.super.asNative(allocator);
						}
					}
				""";

		var ofWithChain =
				RAW."""
					
				""";

		return List.of(nextInterface, ofWithChain);
	}
}
