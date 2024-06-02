package dev.brownjames.lawu.vulkan.generator.structure;

import static java.lang.StringTemplate.RAW;

/**
 * A member mapping a value to another type
 * @param name the name of the value
 * @param mapping the mapping for that value
 */
record MappedValueMember(CharSequence name, NameMapping mapping) implements StructureMember {
	@Override
	public NameMapping type() {
		return mapping;
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{mapping}.of(\{request.target()}.\{name}$get(\{argument}))";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence destination, CharSequence allocator) {
		return RAW."\{request.target()}.\{name}$set(\{destination}, \{name}.asRaw());";
	}
}
