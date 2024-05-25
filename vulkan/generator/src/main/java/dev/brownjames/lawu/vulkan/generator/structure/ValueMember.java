package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.type.TypeMirror;

import static java.lang.StringTemplate.RAW;

/**
 * Members like {@code uint32_t aNumber}
 * @param name the name of the member
 * @param type the type of the member
 */
record ValueMember(CharSequence name, TypeMirror type) implements StructureMember {
	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{request.target()}.\{name}$get(\{argument})";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."\{request.target()}.\{name}$set(\{argument}, \{name});";
	}
}
