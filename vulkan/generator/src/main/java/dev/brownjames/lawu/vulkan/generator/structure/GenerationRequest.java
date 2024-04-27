package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

sealed interface GenerationRequest
		permits BitFlagGenerationRequest, StructureGenerationRequest {
	CharSequence qualifiedName();
	PackageElement destination();
	CharSequence name();
	Object target();

	default Element owner() {
		return target() instanceof Element e ? e : destination();
	}
}
