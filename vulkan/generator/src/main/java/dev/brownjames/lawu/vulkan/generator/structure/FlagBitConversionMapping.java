package dev.brownjames.lawu.vulkan.generator.structure;

/**
 * A mapping for a bit-flag used as a single flag
 * @param target the target name to map
 * @param mapping the bit-flag request to map to. This may be an existing mapping if the bit-flag is already mapped
 */
record FlagBitConversionMapping(CharSequence target, NameMapping mapping) implements EnumMapping {
	@Override
	public CharSequence name() {
		return mapping.name();
	}

	@Override
	public CharSequence qualifiedName() {
		return mapping.qualifiedName();
	}
}
