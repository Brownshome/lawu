package dev.brownjames.lawu.vulkan.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(MapStructure.Container.class)
public @interface MapStructure {
	String value();

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.SOURCE)
	@Documented
	@interface Container {
		MapStructure[] value();
	}
}
