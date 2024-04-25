package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.TypeElement;

/**
 * A mapping from a type that does not exist
 * @param target the name of the mapped-from item
 * @param mapping the mapped-to type
 */
record NewMapping(CharSequence target, TypeElement mapping) implements ExistingMapping { }
