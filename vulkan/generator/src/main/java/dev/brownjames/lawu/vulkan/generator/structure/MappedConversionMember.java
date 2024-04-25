package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.Optional;

/**
 * A member with a mapping. For example, {@code VkPhysicalDeviceFeatures features}
 * @param name the name of the member
 * @param mapping the mapping for the member type
 */
record MappedConversionMember(CharSequence name, NameMapping mapping) implements SliceMember {
	@Override
	public CharSequence simpleTypeName() {
		return mapping.name();
	}

	@Override
	public Optional<CharSequence> importTypeName() {
		return Optional.of(mapping.qualifiedName());
	}

	@Override
	public CharSequence of(GenerationRequest request, CharSequence argument) {
		return STR."\{mapping.name()}.of(\{slice(request, argument)})";
	}

	@Override
	public CharSequence asNative(GenerationRequest request, CharSequence argument) {
		return STR."\{name}.asNative(\{slice(request, argument)});";
	}
}
