package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.Optional;

/**
 * Members whose type is described by a class
 */
interface ClassMember extends StructureMember {
	/**
	 * The type of this member
	 * @return a class
	 */
	Class<?> type();

	@Override
	default CharSequence simpleTypeName() {
		return type().getSimpleName();
	}

	@Override
	default Optional<CharSequence> importTypeName() {
		var type = type();

		if (type().isArray()) {
			type = type.componentType();
		}

		return Optional.ofNullable(type.getCanonicalName());
	}
}
