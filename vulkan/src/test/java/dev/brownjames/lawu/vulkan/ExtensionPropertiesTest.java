package dev.brownjames.lawu.vulkan;

import org.junit.jupiter.api.Test;

import java.lang.System.Logger.Level;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

record ExtensionPropertiesTest() {
	private static final System.Logger LOGGER = System.getLogger(String.valueOf(ExtensionPropertiesTest.class.getModule().getName()));

	@Test
	void queryExtensions() {
		var extensions = ExtensionProperties.vulkanOrImplicit();

		LOGGER.log(Level.INFO, extensions.stream().map(ExtensionProperties::name).collect(Collectors.joining(", ", "Extensions [", "]")));

		assertTrue(extensions.stream().allMatch(extension -> extension.name().startsWith("VK_")));
	}
}
