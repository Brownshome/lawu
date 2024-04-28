package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

sealed interface GenerationRequest extends NameMapping
		permits FlagGenerationRequest, EnumGenerationRequest, StructureGenerationRequest {
	PackageElement destination();

	default Element owner() {
		return target() instanceof Element e ? e : destination();
	}
}
