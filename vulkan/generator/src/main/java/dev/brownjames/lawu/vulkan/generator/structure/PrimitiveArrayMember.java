package dev.brownjames.lawu.vulkan.generator.structure;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

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
	public CharSequence of(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{slice(request, argument)}.toArray(\{typeContainingValueLayout().getSimpleName()}.\{valueLayout.getSimpleName()})";
	}

	@Override
	public CharSequence asNative(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{slice(request, argument)}.copyFrom(\{MemorySegment.class.getSimpleName()}.ofArray(\{name}));";
	}

	@Override
	public CharSequence simpleTypeName() {
		return type.toString();
	}

	@Override
	public Optional<CharSequence> importTypeName() {
		return Optional.empty();
	}

	@Override
	public Collection<? extends CharSequence> imports(StructureGenerationRequest request) {
		var result = new ArrayList<CharSequence>(SliceMember.super.imports(request));
		result.add(MemorySegment.class.getCanonicalName());
		result.add(typeContainingValueLayout().getQualifiedName());
		return result;
	}

	private TypeElement typeContainingValueLayout() {
		return (TypeElement) valueLayout.getEnclosingElement();
	}
}
