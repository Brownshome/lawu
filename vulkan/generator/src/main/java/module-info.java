/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

import dev.brownjames.lawu.vulkan.generator.structure.StructureGenerator;

module dev.brownjames.lawu.vulkan.generator {
	requires static java.compiler;

	requires dev.brownjames.lawu.vulkan.annotation;

	requires org.antlr.antlr4.runtime;
	requires antlr4;

	provides javax.annotation.processing.Processor
			with StructureGenerator;
}
