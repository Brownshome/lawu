package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.lang.model.element.TypeElement;

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
	public Collection<? extends CharSequence> imports(StructureGenerationRequest request) {
		var result = new ArrayList<CharSequence>(TypeElementMember.super.imports(request));
		result.add(request.target().getQualifiedName());
		result.add(functionPointer.getQualifiedName());
		return result;
	}

	@Override
	public CharSequence of(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{request.target().getSimpleName()}.\{name}$get(\{argument})";
	}

	@Override
	public CharSequence asNative(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{request.target().getSimpleName()}.\{name}$set(\{argument}, \{name});";
	}

	private CharSequence convertedFunctionPointerName() {
		var builder = new StringBuilder(name.subSequence(FUNCTION_POINTER_PREFIX.length(), name.length()));
		builder.setCharAt(0, Character.toLowerCase(builder.charAt(0)));
		return builder;
	}

	@Override
	public List<? extends CharSequence> extraDeclarations() {
		return List.of(
				STR."""
					/**
					 * Creates a {@link \{functionPointer.getSimpleName()}} object from the memory in this structure
					 * @param arena the scope of validity for the used memory
					 * @return a function pointer structure
					 */
					public \{functionPointer.getSimpleName()} \{convertedFunctionPointerName()}(Arena arena) {
						return \{functionPointer.getSimpleName()}.ofAddress(\{name}, arena);
					}
				""",
				STR."""
					/**
					 * Creates a {@link \{functionPointer.getSimpleName()}} object from the memory in this structure
					 *
					 * @return a function pointer structure
					 */
					public \{functionPointer.getSimpleName()} \{convertedFunctionPointerName()}() {
						return \{convertedFunctionPointerName()}(Arena.global());
					}
				"""
		);
	}
}
