package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * A mapping representing a request for the generation of a structure
 * @param target the existing type to be mapped
 * @param destination the package destination of the generated type
 * @param name the name of the generated type
 */
record GenerationRequest(TypeElement target, PackageElement destination, CharSequence name) implements TargetedMapping {
	@Override
	public CharSequence qualifiedName() {
		return STR."\{destination}.\{name}";
	}
}
