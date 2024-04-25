package dev.brownjames.lawu.vulkan.generator.structure;

import java.lang.foreign.MemorySegment;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.*;

record MemorySegmentListMember(CharSequence name, Element layout) implements ListMember {
	MemorySegmentListMember {
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
	public CharSequence listMemberSimpleTypeName() {
		return MemorySegment.class.getSimpleName();
	}

	@Override
	public Optional<CharSequence> listMemberImportTypeName() {
		return Optional.of(MemorySegment.class.getCanonicalName());
	}

	@Override
	public CharSequence asNative(CharSequence itemArgument, CharSequence sliceArgument) {
		return STR."\{sliceArgument}.set(\{layoutExpression()}, 0L, \{itemArgument});";
	}

	@Override
	public CharSequence of(String sliceArgument) {
		return STR."\{sliceArgument}.get(\{layoutExpression()}, 0L)";
	}
}
