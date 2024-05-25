package dev.brownjames.lawu.vulkan.generator.structure;

/**
 * Members whose type is described by a class
 */
interface ClassMember extends StructureMember {
	@Override
	Class<?> type();
}
