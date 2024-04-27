package dev.brownjames.lawu.vulkan.generator.structure;

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
	public CharSequence of(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{slice(request, argument)}.getUtf8String(0L)";
	}

	@Override
	public CharSequence asNative(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{slice(request, argument)}.setUtf8String(0L, \{name});";
	}
}
