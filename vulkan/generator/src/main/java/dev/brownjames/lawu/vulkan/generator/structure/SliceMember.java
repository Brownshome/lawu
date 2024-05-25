package dev.brownjames.lawu.vulkan.generator.structure;

import static java.lang.StringTemplate.RAW;

/**
 * A member that's getters and setters involving reading a slice of the structure memory
 */
interface SliceMember extends StructureMember {
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
	default StringTemplate slice(StructureGenerationRequest request, CharSequence argument) {
		return RAW."\{request.target()}.\{name()}$slice(\{argument})";
	}
}
