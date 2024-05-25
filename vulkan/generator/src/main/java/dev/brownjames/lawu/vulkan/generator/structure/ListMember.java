package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import static java.lang.StringTemplate.RAW;

/**
 * A member that maps to a list
 */
interface ListMember extends SliceMember {
	@Override
	default StringTemplate type() {
		return RAW."\{List.class}<\{memberType()}>";
	}

	/**
	 * The simple name of the type contained in this list
	 * @return the name
	 */
	Object memberType();

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

	default StringTemplate layoutExpression() {
		return switch (layout().getKind()) {
			case METHOD -> RAW."\{typeContainingLayout()}.\{layout().getSimpleName()}()";
			case FIELD -> RAW."\{typeContainingLayout()}.\{layout().getSimpleName()}";
			default -> throw new AssertionError(STR."Unexpected layout element type \{layout()}");
		};
	}

	/**
	 * A code-snippet than can write the given item argument into the given slice
	 * @param itemArgument the item to write
	 * @param sliceArgument the slice to write it into
	 * @param allocatorArgument the allocator to use
	 * @return a code statement
	 */
	StringTemplate asRaw(CharSequence itemArgument, CharSequence sliceArgument, CharSequence allocatorArgument);

	/**
	 * An expression that converts the given slice to the member item
	 * @param sliceArgument the argument containing the slice
	 * @return an expression
	 */
	StringTemplate of(String sliceArgument);

	@Override
	default StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{slice(request, argument)}.elements(\{layoutExpression()}).map(\{name()}$slice -> \{of(STR."\{name()}$slice")}).toList()";
	}

	@Override
	default StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."""
				var \{name()}$slices = \{Spliterators.class}.iterator(\{slice(request, argument)}.spliterator(\{layoutExpression()}));
				var \{name()}$items = \{name()}.iterator();
				while (\{name()}$slices.hasNext() && \{name()}$items.hasNext()) {
					var \{name()}$item = \{name()}$items.next();
					var \{name()}$slice = \{name()}$slices.next();
					\{asRaw(STR."\{name()}$item", STR."\{name()}$slice", allocator)}
				}
		""";
	}
}
