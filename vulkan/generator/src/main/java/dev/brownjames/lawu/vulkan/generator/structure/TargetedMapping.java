package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.TypeElement;

/**
 * A mapping that has a target that exists
 */
public sealed interface TargetedMapping extends NameMapping
		permits FunctionPointerMapping, ReplacementMapping, StructureGenerationRequest {
	@Override
	TypeElement target();
}
