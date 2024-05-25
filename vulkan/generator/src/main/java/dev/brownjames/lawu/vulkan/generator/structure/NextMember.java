package dev.brownjames.lawu.vulkan.generator.structure;

import java.lang.foreign.MemorySegment;
import java.util.List;

import dev.brownjames.lawu.vulkan.generator.GenerationFailedException;

import static java.lang.StringTemplate.RAW;

record NextMember() implements StructureMember {
	@Override
	public List<? extends StringTemplate> extraDeclarations(StructureGenerationRequest request) {
		try {
			return NextGenerator.generateNextStructure(request);
		} catch (GenerationFailedException e) {
			throw e.unchecked();
		}
	}

	@Override
	public StringTemplate type() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CharSequence name() {
		return "pNext";
	}

	@Override
	public StringTemplate of(StructureGenerationRequest request, CharSequence argument) {
		return RAW."Next.ofChain(\{request.target()}.\{name()}$get(\{argument}))";
	}

	@Override
	public StringTemplate asRaw(StructureGenerationRequest request, CharSequence argument, CharSequence allocator) {
		return RAW."\{request.target()}.\{name()}$set(\{argument}, \{MemorySegment.class}.NULL);";
	}
}
