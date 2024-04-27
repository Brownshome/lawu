package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.TypeElement;

/**
 * A mapping linking the name of a function pointer to its structure
 * @param target the structure element
 */
record FunctionPointerMapping(TypeElement target) implements ExistingMapping, TargetedMapping {
	@Override
	public TypeElement mapping() {
		return target;
	}
}
