package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * A member mapping to a set of bit-flags
 * @param name the name of the member
 * @param mapping the mapping for the member type
 */
record BitFlagConversionMember(CharSequence name, BitFlagGenerationRequest mapping) implements StructureMember {
	@Override
	public CharSequence simpleTypeName() {
		return STR."Collection<\{mapping.name()}>";
	}

	@Override
	public Collection<? extends CharSequence> imports(StructureGenerationRequest request) {
		var result = new ArrayList<CharSequence>(StructureMember.super.imports(request));
		result.add(mapping.qualifiedName());
		result.add(StructureGenerator.getContext().lookup().bitFlag().orElseThrow().getQualifiedName());
		result.add(request.target().getQualifiedName());
		return result;
	}

	@Override
	public Optional<CharSequence> importTypeName() {
		return Optional.of(Collection.class.getCanonicalName());
	}

	@Override
	public CharSequence of(StructureGenerationRequest request, CharSequence argument) {
		var bitFlag = StructureGenerator.getContext().lookup().bitFlag().orElseThrow();
		return STR."\{bitFlag.getSimpleName()}.flags(\{request.target().getSimpleName()}.\{name}$get(\{argument}), \{mapping.name()}.class)";
	}

	@Override
	public CharSequence asNative(StructureGenerationRequest request, CharSequence argument) {
		var bitFlag = StructureGenerator.getContext().lookup().bitFlag().orElseThrow();
		return STR."\{request.target().getSimpleName()}.\{name}$set(\{argument}, \{bitFlag.getSimpleName()}.getFlagBits(\{name}));";
	}
}
