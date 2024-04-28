package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

record EnumConversionMember(CharSequence name, EnumMapping mapping) implements StructureMember {
	@Override
	public CharSequence simpleTypeName() {
		return mapping().name();
	}

	@Override
	public Optional<CharSequence> importTypeName() {
		return Optional.of(mapping().qualifiedName());
	}

	@Override
	public Collection<? extends CharSequence> imports(StructureGenerationRequest request) {
		var result = new ArrayList<CharSequence>(StructureMember.super.imports(request));
		result.add(request.target().getQualifiedName());
		return result;
	}

	@Override
	public CharSequence of(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{mapping.name()}.of(\{request.target().getSimpleName()}.\{name}$get(\{argument}))";
	}

	@Override
	public CharSequence asNative(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{request.target().getSimpleName()}.\{name}$set(\{argument}, \{name}.\{switch (mapping) {
			case EnumGenerationRequest _ -> "value";
			case FlagBitConversionMapping _ -> "bit";
		}}());";
	}
}
