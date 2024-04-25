package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * A member that maps to a list
 */
interface ListMember extends SliceMember {
	@Override
	default CharSequence simpleTypeName() {
		return STR."\{List.class.getSimpleName()}<\{listMemberSimpleTypeName()}>";
	}

	/**
	 * The simple name of the type contained in this list
	 * @return the name
	 */
	CharSequence listMemberSimpleTypeName();

	/**
	 * The import name of the type contained in this list, if it needs importing
	 * @return the name
	 */
	Optional<CharSequence> listMemberImportTypeName();

	/**
	 * The element describing the native layout of list elements
	 * @return a variable or method element
	 */
	Element layout();

	/**
	 * The type containing the layout
	 * @return a type element
	 */
	default TypeElement typeContainingLayout() {
		return (TypeElement) layout().getEnclosingElement();
	}

	default CharSequence layoutExpression() {
		return switch (layout().getKind()) {
			case METHOD -> STR."\{typeContainingLayout().getSimpleName()}.\{layout().getSimpleName()}()";
			case FIELD -> STR."\{typeContainingLayout().getSimpleName()}.\{layout().getSimpleName()}";
			default -> throw new AssertionError(STR."Unexpected layout element type \{layout()}");
		};
	}

	/**
	 * A code-snippet than can write the given item argument into the given slice
	 * @param itemArgument the item to write
	 * @param sliceArgument the slice to write it into
	 * @return a code statement
	 */
	CharSequence asNative(CharSequence itemArgument, CharSequence sliceArgument);

	/**
	 * An expression that converts the given slice to the member item
	 * @param sliceArgument the argument containing the slice
	 * @return an expression
	 */
	CharSequence of(String sliceArgument);

	@Override
	default CharSequence of(GenerationRequest request, CharSequence argument) {
		return STR."\{slice(request, argument)}.elements(\{layoutExpression()}).map(\{name()}$slice -> \{of(STR."\{name()}$slice")}).toList()";
	}

	@Override
	default CharSequence asNative(GenerationRequest request, CharSequence argument) {
		return STR."""
				var \{name()}$slices = \{Spliterators.class.getSimpleName()}.iterator(\{slice(request, argument)}.spliterator(\{layoutExpression()}));
				var \{name()}$items = \{name()}.iterator();
				while (\{name()}$slices.hasNext() && \{name()}$items.hasNext()) {
					var \{name()}$item = \{name()}$items.next();
					var \{name()}$slice = \{name()}$slices.next();
					\{asNative(STR."\{name()}$item", STR."\{name()}$slice")}
				}
		""";
	}

	@Override
	default Optional<CharSequence> importTypeName() {
		return Optional.of(List.class.getCanonicalName());
	}

	@Override
	default Collection<? extends CharSequence> imports(GenerationRequest request) {
		var result = new ArrayList<CharSequence>(SliceMember.super.imports(request));

		result.add(Spliterators.class.getCanonicalName());
		result.add(typeContainingLayout().getQualifiedName());

		listMemberImportTypeName().ifPresent(result::add);

		return result;
	}
}
