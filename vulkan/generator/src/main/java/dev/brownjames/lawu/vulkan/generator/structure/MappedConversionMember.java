package dev.brownjames.lawu.vulkan.generator.structure;

import static java.lang.StringTemplate.RAW;

/**
 * A member with a mapping. For example, {@code VkPhysicalDeviceFeatures features}
 * @param name the name of the member
 * @param mapping the mapping for the member type
 */
record MappedConversionMember(CharSequence name, NameMapping mapping) implements SliceMember {
	@Override
	public NameMapping type() {
		return mapping;
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{mapping}.of(\{slice(request, argument)})";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."\{name}.asRaw(\{slice(request, argument)}, \{allocator});";
	}
}
