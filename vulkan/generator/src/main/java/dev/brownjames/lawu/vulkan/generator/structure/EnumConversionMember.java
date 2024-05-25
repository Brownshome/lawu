package dev.brownjames.lawu.vulkan.generator.structure;

import static java.lang.StringTemplate.RAW;

record EnumConversionMember(CharSequence name, EnumMapping mapping) implements StructureMember {
	@Override
	public EnumMapping type() {
		return mapping();
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{mapping}.of(\{request.target()}.\{name}$get(\{argument}))";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."\{request.target()}.\{name}$set(\{argument}, \{name}.\{switch (mapping) {
			case EnumGenerationRequest _ -> "value";
			case FlagBitConversionMapping _ -> "bit";
		}}());";
	}
}
