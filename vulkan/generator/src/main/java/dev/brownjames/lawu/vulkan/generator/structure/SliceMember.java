package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A member that's getters and setters involving reading a slice of the structure memory
 */
interface SliceMember extends StructureMember {
	@Override
	default Collection<? extends CharSequence> imports(StructureGenerationRequest request) {
		var result = new ArrayList<CharSequence>(StructureMember.super.imports(request));
		result.add(request.target().getQualifiedName());
		return result;
	}

	/**
	 * The name of the member
	 * @return a name
	 */
	CharSequence name();

	/**
	 * Gets the slice related to this member
	 * @param request the generation request object for this structure
	 * @param argument the name of the argument representing the memory of the structure
	 * @return code retrieving the slice
	 */
	default CharSequence slice(StructureGenerationRequest request, CharSequence argument) {
		return STR."\{request.target().getSimpleName()}.\{name()}$slice(\{argument})";
	}
}
