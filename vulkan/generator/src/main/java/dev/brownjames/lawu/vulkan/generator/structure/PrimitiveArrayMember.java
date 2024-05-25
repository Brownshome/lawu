package dev.brownjames.lawu.vulkan.generator.structure;

import java.lang.foreign.MemorySegment;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static java.lang.StringTemplate.RAW;

/**
 * A member of the form {@code uint32_t values[32]} mapping to an array of primitives
 * @param name the name of the member
 * @param type the array-type of the member
 * @param valueLayout the value-layout variable that describes the array
 */
record PrimitiveArrayMember(CharSequence name, TypeMirror type, VariableElement valueLayout) implements SliceMember {
	PrimitiveArrayMember {
		if (!valueLayout.getKind().isField() || !valueLayout.getEnclosingElement().getKind().isDeclaredType()
				|| !valueLayout.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC))) {
			throw new IllegalArgumentException(STR."\{valueLayout.getSimpleName()} is not a public, static, field of a declared type");
		}

		if (type.getKind() != TypeKind.ARRAY) {
			throw new IllegalArgumentException(STR."\{type} is not an array");
		}
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{slice(request, argument)}.toArray(\{typeContainingValueLayout()}.\{valueLayout.getSimpleName()})";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."\{slice(request, argument)}.copyFrom(\{MemorySegment.class}.ofArray(\{name}));";
	}

	private TypeElement typeContainingValueLayout() {
		return (TypeElement) valueLayout.getEnclosingElement();
	}
}
