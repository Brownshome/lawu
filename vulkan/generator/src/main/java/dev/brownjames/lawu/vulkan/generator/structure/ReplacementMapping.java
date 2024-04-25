package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.TypeElement;

/**
 * A mapping replacing the mapped-from type with a mapped-to type
 * @param target the mapped-from type
 * @param mapping the mapped-to type
 */
record ReplacementMapping(TypeElement target, TypeElement mapping) implements ExistingMapping, TargetedMapping { }
