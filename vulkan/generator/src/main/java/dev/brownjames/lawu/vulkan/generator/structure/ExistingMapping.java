package dev.brownjames.lawu.vulkan.generator.structure;

import javax.lang.model.element.TypeElement;

/**
 * A mapping from a name to an existing type
 */
sealed interface ExistingMapping extends NameMapping permits ReplacementMapping, NewMapping {
	/**
	 * The existing type to map to
	 * @return the type element for that type
	 */
	TypeElement mapping();

	@Override
	default CharSequence name() {
		return mapping().getSimpleName();
	}

	@Override
	default CharSequence qualifiedName() {
		return mapping().getQualifiedName();
	}
}
