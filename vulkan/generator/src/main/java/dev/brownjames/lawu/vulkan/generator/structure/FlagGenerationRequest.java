package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;

record FlagGenerationRequest(CharSequence target, List<ExecutableElement> flags,
                             PackageElement destination, CharSequence name) implements NameMapping, GenerationRequest {
	@Override
	public CharSequence qualifiedName() {
		return STR."\{destination}.\{name}";
	}
}
