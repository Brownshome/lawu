package dev.brownjames.lawu.bindings;

import de.skuzzle.semantic.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VulkanInstanceTest {
	@Test
	void createInstance() {
		var v = VulkanInstance.builder()
				.withApplicationName("create")
				.withApplicationVersion(Version.create(0, 1))
				.withEngineName("in-house")
				.withEngineVersion(Version.create(0, 1))
				.build();

		v.close();
	}

	@Test
	void allPhysicalDevices() {
		try (var vulkan = VulkanInstance.create()) {
			var devices = vulkan.allPhysicalDevices();

			assertTrue(devices.size() >= 1);
		}
	}
}
