package dev.brownjames.lawu.vulkan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VulkanTest {
	@Test
	void getInstanceVersion() {
		var version = Vulkan.getInstanceVersion();
		assertEquals(0, version.variant());
		assertEquals(1, version.major());
		assertEquals(3, version.minor());
	}
}
