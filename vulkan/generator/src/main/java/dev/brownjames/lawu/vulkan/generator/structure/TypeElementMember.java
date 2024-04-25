package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.Optional;

import javax.lang.model.element.TypeElement;

/**
 * A member whose type is represented by a {@link TypeElement}
 */
interface TypeElementMember extends StructureMember {
	/**
	 * The type element representing the type of this member
	 * @return a type element
	 */
	TypeElement type();

	@Override
	default CharSequence simpleTypeName() {
		return type().getSimpleName();
	}

	@Override
	default Optional<CharSequence> importTypeName() {
		return Optional.of(type().getQualifiedName());
	}
}
