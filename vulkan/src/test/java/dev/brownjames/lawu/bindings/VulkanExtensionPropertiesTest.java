package dev.brownjames.lawu.bindings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VulkanExtensionPropertiesTest {
	@Test
	void queryExtensions() {
		var extensions = VulkanExtensionProperties.all();

		assertTrue(extensions.stream().anyMatch(extension -> extension.name().equals("VK_KHR_surface")));
	}
}
