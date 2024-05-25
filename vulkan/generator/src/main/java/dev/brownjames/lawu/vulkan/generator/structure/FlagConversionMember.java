package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.Collection;

import static java.lang.StringTemplate.RAW;

/**
 * A member mapping to a set of bit-flags
 * @param name the name of the member
 * @param mapping the mapping for the member type
 */
record FlagConversionMember(CharSequence name, FlagGenerationRequest mapping) implements StructureMember {
	@Override
	public StringTemplate type() {
		return RAW."\{Collection.class}<\{mapping}>";
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		var bitFlag = StructureGenerator.getContext().lookup().bitFlag().orElseThrow();
		return RAW."\{bitFlag}.flags(\{request.target()}.\{name}$get(\{argument}), \{mapping}.class)";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		var bitFlag = StructureGenerator.getContext().lookup().bitFlag().orElseThrow();
		return RAW."\{request.target()}.\{name}$set(\{argument}, \{bitFlag}.getFlagBits(\{name}));";
	}
}
