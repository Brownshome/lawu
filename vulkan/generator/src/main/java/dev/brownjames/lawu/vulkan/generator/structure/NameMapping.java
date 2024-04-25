package dev.brownjames.lawu.vulkan.generator.structure;

/**
 * A mapping between a target and a name
 */
sealed interface NameMapping permits ExistingMapping, TargetedMapping {
	/**
	 * The target of the mapping. This may be a {@link javax.lang.model.element.TypeElement} or a {@link CharSequence} if
	 * the type has not been generated yet.
	 *
	 * @return the target
	 */
	Object target();

	/**
	 * The name that the target is mapped to
	 * @return a name
	 */
	CharSequence name();

	/**
	 * The qualified name that the target is mapped to
	 * @return the qualified name
	 */
	CharSequence qualifiedName();
}
