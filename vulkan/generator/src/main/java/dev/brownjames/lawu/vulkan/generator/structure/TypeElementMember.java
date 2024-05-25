package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.TypeElement;

/**
 * A member whose type is represented by a {@link TypeElement}
 */
interface TypeElementMember extends StructureMember {
	/**
	 * The type element representing the type of this member
	 * @return a type element
	 */
	@Override
	TypeElement type();
}
