package dev.brownjames.lawu.vulkan.annotation;

import java.lang.annotation.*;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface VulkanHeader {
	Class<?> value();
}
