package dev.brownjames.lawu.vulkan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

record VulkanVersionNumberTest() {
	@Test
	void instanceVersion() {
		var version = VulkanVersionNumber.instanceVersion();
		assertEquals(0, version.variant());
		assertEquals(1, version.major());
		assertEquals(3, version.minor());
	}
}
