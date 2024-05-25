package dev.brownjames.lawu.vulkan.generator.structure;

import java.lang.foreign.Arena;
import java.util.List;

import javax.lang.model.element.TypeElement;

import static java.lang.StringTemplate.RAW;

/**
 * Members like {@code PFN_vkSomeFunction member}
 * @param name the name of the member
 * @param functionPointer the function pointer element of the member
 */
record FunctionPointerMember(CharSequence name, TypeElement functionPointer) implements TypeElementMember {
	private static final CharSequence FUNCTION_POINTER_PREFIX = "pfn";

	@Override
	public TypeElement type() {
		return StructureGenerator.getContext().lookup().memorySegment()
				.orElseThrow();
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{request.target()}.\{name}$get(\{argument})";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."\{request.target()}.\{name}$set(\{argument}, \{name});";
	}

	private CharSequence convertedFunctionPointerName() {
		var builder = new StringBuilder(name.subSequence(FUNCTION_POINTER_PREFIX.length(), name.length()));
		builder.setCharAt(0, Character.toLowerCase(builder.charAt(0)));
		return builder;
	}

	@Override
	public List<? extends StringTemplate> extraDeclarations(StructureGenerationRequest request) {
		return List.of(
				RAW."""
					/**
					 * Creates a {@link \{functionPointer}} object from the memory in this structure
					 * @param arena the scope of validity for the used memory
					 * @return a function pointer structure
					 */
					default \{functionPointer} \{convertedFunctionPointerName()}(\{Arena.class} arena) {
						return \{functionPointer}.ofAddress(\{name}(), arena);
					}
				""",
				RAW."""
					/**
					 * Creates a {@link \{functionPointer}} object from the memory in this structure
					 *
					 * @return a function pointer structure
					 */
					default \{functionPointer} \{convertedFunctionPointerName()}() {
						return \{convertedFunctionPointerName()}(\{Arena.class}.global());
					}
				"""
		);
	}
}
