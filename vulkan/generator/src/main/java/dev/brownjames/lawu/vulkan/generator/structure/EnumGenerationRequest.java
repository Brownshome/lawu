package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;

/**
 * A generation request for a non-bit-field enum
 *
 * @param target      the name of the native enum
 * @param values      the values of the enum
 * @param destination the package to generate the enum in
 * @param name        the name of the generated class
 */
record EnumGenerationRequest(CharSequence target, List<EnumValue> values, PackageElement destination,
                             CharSequence name) implements EnumMapping, GenerationRequest {
	record EnumValue(ExecutableElement valueGetter, CharSequence name, int value) { }

	@Override
	public CharSequence qualifiedName() {
		return STR."\{destination}.\{name}";
	}
}
