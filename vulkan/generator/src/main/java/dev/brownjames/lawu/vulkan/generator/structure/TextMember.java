package dev.brownjames.lawu.vulkan.generator.structure;

import static java.lang.StringTemplate.RAW;

/**
 * A member that is represented by a {@link String}
 * @param name the name of the member
 */
record TextMember(CharSequence name) implements ClassMember, SliceMember {
	@Override
	public Class<?> type() {
		return String.class;
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{slice(request, argument)}.getUtf8String(0L)";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."\{slice(request, argument)}.setUtf8String(0L, \{name});";
	}
}
