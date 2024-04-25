/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan.annotation;

import java.lang.annotation.*;

/**
 * Generates structures from a package. This does not include any extension classes
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(GenerateCoreStructuresFrom.Container.class)
public @interface GenerateCoreStructuresFrom {
	/**
	 * The package to search for classes
	 * @return the name of a package
	 */
	String value();

	@Target(ElementType.PACKAGE)
	@Retention(RetentionPolicy.SOURCE)
	@Documented
	@interface Container {
		GenerateCoreStructuresFrom[] value();
	}
}
