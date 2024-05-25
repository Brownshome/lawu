package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.List;

import javax.lang.model.element.*;

import static java.lang.StringTemplate.RAW;

record MappedListMember(CharSequence name, TargetedMapping mapping, Element layout) implements ListMember {
	MappedListMember {
		if (!layout.getEnclosingElement().getKind().isDeclaredType()) {
			throw new IllegalArgumentException(STR."\{layout} is not the member of a declared type");
		}

		if (layout.getKind().isField() && layout instanceof VariableElement field) {
			if (!field.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC))) {
				throw new IllegalArgumentException(STR."\{layout} is a field, but it not public and static");
			}
		} else if (layout.getKind() == ElementKind.METHOD && layout instanceof ExecutableElement method) {
			if (!method.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC))) {
				throw new IllegalArgumentException(STR."\{layout} is a method, but it not public and static");
			}

			if (!method.getParameters().isEmpty()) {
				throw new IllegalArgumentException(STR."\{layout} must not have parameters");
			}
		}
	}

	@Override
	public TargetedMapping memberType() {
		return mapping;
	}

	@Override
	public StringTemplate asRaw(CharSequence itemArgument, CharSequence sliceArgument, CharSequence allocatorArgument) {
		return RAW."\{itemArgument}.asRaw(\{sliceArgument}, \{allocatorArgument});";
	}

	@Override
	public StringTemplate of(String sliceArgument) {
		return RAW."(\{mapping}) \{mapping}.of(\{sliceArgument})";
	}
}
