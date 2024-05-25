package dev.brownjames.lawu.vulkan;

import de.skuzzle.semantic.Version;
import dev.brownjames.lawu.vulkan.debugutils.DebugUtilsExtension;
import dev.brownjames.lawu.vulkan.directdriverloading.DirectDriverLoadingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class VulkanInstanceTest {
	@Test
	void createDefaultInstance() throws VulkanValidationException {
		var v = VulkanInstance.create();

		v.close();
	}

	@Test
	void createInstance() throws VulkanValidationException {
		var v = TestVulkanInstanceHelper.builder()
				.withApplicationInfo(new ApplicationInfo()
						.withApplicationName("create")
						.withApplicationVersion(Version.create(0, 1))
						.withEngineName("in-house")
						.withEngineVersion(Version.create(0, 1)))
				.validate();

		v.close();
	}

	@Test
	void enumeratePhysicalDevices() throws VulkanValidationException {
		try (var vulkan = TestVulkanInstanceHelper.builder().validate();
		     var _ = TestVulkanInstanceHelper.createMessenger(DebugUtilsExtension.extend(vulkan))) {
			var devices = vulkan.enumeratePhysicalDevices();

			assertFalse(devices.isEmpty());
		}
	}

	@Test
	void directDriverLoading() throws VulkanValidationException {
		var vulkan = TestVulkanInstanceHelper.builder().withDrivers(DirectDriverLoadingMode.EXCLUSIVE, new TestVulkanDriver()).validate();
		vulkan.close();
	}

	@Test
	void portabilityEnumeration() throws VulkanValidationException {
		var vulkan = TestVulkanInstanceHelper.builder().withPortabilityEnumeration().validate();
		vulkan.close();
	}
}
