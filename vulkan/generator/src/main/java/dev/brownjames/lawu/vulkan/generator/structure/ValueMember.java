package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Members like {@code uint32_t aNumber}
 * @param name the name of the member
 * @param type the type of the member
 * @param importTarget the type element of the member, if it exists, for importing
 */
record ValueMember(CharSequence name, TypeMirror type, Optional<TypeElement> importTarget) implements StructureMember {
	@Override
	public CharSequence simpleTypeName() {
		return importTarget.<CharSequence>map(TypeElement::getSimpleName).orElse(type.toString());
	}

	@Override
	public Optional<CharSequence> importTypeName() {
		return importTarget.map(TypeElement::getQualifiedName);
	}

	@Override
	public Collection<? extends CharSequence> imports(GenerationRequest request) {
		var result = new ArrayList<CharSequence>(StructureMember.super.imports(request));
		result.add(request.target().getQualifiedName());
		return result;
	}

	@Override
	public CharSequence of(GenerationRequest request, CharSequence argument) {
		return STR."\{request.target().getSimpleName()}.\{name}$get(\{argument})";
	}

	@Override
	public CharSequence asNative(GenerationRequest request, CharSequence argument) {
		return STR."\{request.target().getSimpleName()}.\{name}$set(\{argument}, \{name});";
	}
}
