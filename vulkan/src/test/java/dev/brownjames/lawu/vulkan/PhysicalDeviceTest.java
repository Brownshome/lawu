package dev.brownjames.lawu.vulkan;

import java.lang.foreign.MemorySegment;
import java.util.List;

import dev.brownjames.lawu.vulkan.getphysicaldeviceproperties2.GetPhysicalDeviceProperties2Extension;
import org.junit.jupiter.api.*;

import dev.brownjames.lawu.vulkan.bindings.*;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

final class PhysicalDeviceTest {
	VulkanInstance instance;
	PhysicalDevice device;

	@BeforeEach
	void createPhysicalDeviceAndInstance() throws VulkanValidationException {
		instance = TestVulkanInstanceHelper.builder()
				.withExtension(GetPhysicalDeviceProperties2Extension.extensionName())
				.validate();

		device = instance.allPhysicalDevices().getFirst();
	}

	@AfterEach
	void closeInstance() {
		instance.close();
	}

	@Test
	void getProperties() {
		var properties = device.getProperties();
		assertEquals(VulkanVersionNumber.headerVersion().major(), properties.apiVersion().major());
	}

	@Test
	void getProperties2() {
		PhysicalDevice.PropertiesNext vulkanVersionOneProperties = (chainArena, next) -> {
			var properties = VkPhysicalDeviceVulkan11Properties.allocate(chainArena);
			VkPhysicalDeviceVulkan11Properties.sType$set(properties, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_PROPERTIES());
			VkPhysicalDeviceVulkan11Properties.pNext$set(properties, MemorySegment.NULL);
			return properties;
		};

		var properties = device.getProperties(List.of(vulkanVersionOneProperties));

		assertEquals(VulkanVersionNumber.headerVersion().major(), properties.properties().apiVersion().major());

		var chain = properties.nexts();
		assertEquals(1, chain.size());

		var vulkanVersionOnePropertiesResult = chain.getFirst();
		assertEquals(VkPhysicalDeviceVulkan11Properties.$LAYOUT().byteSize(), vulkanVersionOnePropertiesResult.byteSize());
		assertEquals(vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_PROPERTIES(), VkPhysicalDeviceVulkan11Properties.sType$get(vulkanVersionOnePropertiesResult));
	}

	@Test
	void getPropertiesExtension() {
		PhysicalDevice.PropertiesNext vulkanVersionOneProperties = (chainArena, next) -> {
			var properties = VkPhysicalDeviceVulkan11Properties.allocate(chainArena);
			VkPhysicalDeviceVulkan11Properties.sType$set(properties, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_PROPERTIES());
			VkPhysicalDeviceVulkan11Properties.pNext$set(properties, next);
			return properties;
		};

		var extension = GetPhysicalDeviceProperties2Extension.extend(instance);
		var properties = device.getProperties(List.of(vulkanVersionOneProperties), extension);

		assertEquals(VulkanVersionNumber.headerVersion().major(), properties.properties().apiVersion().major());

		var chain = properties.nexts();
		assertEquals(1, chain.size());

		var vulkanVersionOnePropertiesResult = chain.getFirst();
		assertEquals(VkPhysicalDeviceVulkan11Properties.$LAYOUT().byteSize(), vulkanVersionOnePropertiesResult.byteSize());
		assertEquals(vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_PROPERTIES(), VkPhysicalDeviceVulkan11Properties.sType$get(vulkanVersionOnePropertiesResult));
	}

	@Test
	void getFeatures() {
		var features = device.getFeatures();
		assertEquals(VkPhysicalDeviceFeatures.$LAYOUT().byteSize(), features.byteSize());
	}

	@Test
	void getFeatures2() {
		PhysicalDevice.FeaturesNext vulkanVersionOneFeatures = (chainArena, next) -> {
			var features = VkPhysicalDeviceVulkan11Features.allocate(chainArena);
			VkPhysicalDeviceVulkan11Features.sType$set(features, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES());
			VkPhysicalDeviceVulkan11Features.pNext$set(features, MemorySegment.NULL);
			return features;
		};

		var features = device.getFeatures(List.of(vulkanVersionOneFeatures));

		assertEquals(VkPhysicalDeviceFeatures2.$LAYOUT().byteSize(), features.features().byteSize());

		var chain = features.nexts();
		assertEquals(1, chain.size());

		var vulkanVersionOneFeaturesResult = chain.getFirst();
		assertEquals(VkPhysicalDeviceVulkan11Features.$LAYOUT().byteSize(), vulkanVersionOneFeaturesResult.byteSize());
		assertEquals(vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES(), VkPhysicalDeviceVulkan11Features.sType$get(vulkanVersionOneFeaturesResult));
	}

	@Test
	void getFeaturesExtension() {
		PhysicalDevice.FeaturesNext vulkanVersionOneFeatures = (chainArena, next) -> {
			var features = VkPhysicalDeviceVulkan11Features.allocate(chainArena);
			VkPhysicalDeviceVulkan11Features.sType$set(features, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES());
			VkPhysicalDeviceVulkan11Features.pNext$set(features, MemorySegment.NULL);
			return features;
		};

		var extension = GetPhysicalDeviceProperties2Extension.extend(instance);
		var features = device.getFeatures(List.of(vulkanVersionOneFeatures), extension);

		assertEquals(VkPhysicalDeviceFeatures2.$LAYOUT().byteSize(), features.features().byteSize());

		var chain = features.nexts();
		assertEquals(1, chain.size());

		var vulkanVersionOneFeaturesResult = chain.getFirst();
		assertEquals(VkPhysicalDeviceVulkan11Features.$LAYOUT().byteSize(), vulkanVersionOneFeaturesResult.byteSize());
		assertEquals(vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES(), VkPhysicalDeviceVulkan11Features.sType$get(vulkanVersionOneFeaturesResult));
	}

	@Test
	void getImageFormatProperties() {
		var deviceProperties = device.getProperties();
		var limits = deviceProperties.limits();

		var imageFormatProperties = device.getImageFormatProperties(vulkan_h.VK_FORMAT_R8G8B8A8_SRGB(),
				vulkan_h.VK_IMAGE_TYPE_2D(),
				vulkan_h.VK_IMAGE_TILING_OPTIMAL(),
				vulkan_h.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT(),
				0);

		assertTrue(imageFormatProperties.maxExtent().width() >= VkPhysicalDeviceLimits.maxImageDimension2D$get(limits));
		assertTrue(imageFormatProperties.maxExtent().height() >= VkPhysicalDeviceLimits.maxImageDimension2D$get(limits));
		assertEquals(1, imageFormatProperties.maxExtent().depth());

		int maximumDimension = Math.max(imageFormatProperties.maxExtent().width(), imageFormatProperties.maxExtent().height());
		int numberOfLayers = Integer.SIZE - Integer.numberOfLeadingZeros(maximumDimension);
		assertEquals(numberOfLayers, imageFormatProperties.maxMipLevels());

		assertTrue(imageFormatProperties.maxArrayLayers() >= VkPhysicalDeviceLimits.maxImageArrayLayers$get(limits));
		assertTrue(imageFormatProperties.maxResourceSize() >= (1 >> 31));
	}

	@Test
	void getImageFormatProperties2() {
		var deviceProperties = device.getProperties();
		var limits = deviceProperties.limits();

		PhysicalDevice.ImageFormatInfoNext externalImageFormatInfo = (arena, next) -> {
			var result = VkPhysicalDeviceExternalImageFormatInfo.allocate(arena);
			VkPhysicalDeviceExternalImageFormatInfo.sType$set(result, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_IMAGE_FORMAT_INFO());
			VkPhysicalDeviceExternalImageFormatInfo.pNext$set(result, next);
			VkPhysicalDeviceExternalImageFormatInfo.handleType$set(result, switch(OS.current()) {
				case WINDOWS -> vulkan_h.VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT();
				case LINUX -> vulkan_h.VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT();
				default -> 0;
			});
			return result;
		};

		PhysicalDevice.ImageFormatPropertiesNext externalImageFormatProperties = (arena, next) -> {
			var result = VkExternalImageFormatProperties.allocate(arena);
			VkExternalImageFormatProperties.sType$set(result, vulkan_h.VK_STRUCTURE_TYPE_EXTERNAL_IMAGE_FORMAT_PROPERTIES());
			VkExternalImageFormatProperties.pNext$set(result, next);
			return result;
		};

		var imageFormatProperties = device.getImageFormatProperties(vulkan_h.VK_FORMAT_R8G8B8A8_SRGB(),
				vulkan_h.VK_IMAGE_TYPE_2D(),
				vulkan_h.VK_IMAGE_TILING_OPTIMAL(),
				vulkan_h.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT(),
				0,
				List.of(externalImageFormatInfo),
				List.of(externalImageFormatProperties));

		assertTrue(imageFormatProperties.properties().maxExtent().width() >= VkPhysicalDeviceLimits.maxImageDimension2D$get(limits));
		assertTrue(imageFormatProperties.properties().maxExtent().height() >= VkPhysicalDeviceLimits.maxImageDimension2D$get(limits));
		assertEquals(1, imageFormatProperties.properties().maxExtent().depth());

		int maximumDimension = Math.max(imageFormatProperties.properties().maxExtent().width(), imageFormatProperties.properties().maxExtent().height());
		int numberOfLayers = Integer.SIZE - Integer.numberOfLeadingZeros(maximumDimension);
		assertEquals(numberOfLayers, imageFormatProperties.properties().maxMipLevels());

		assertTrue(imageFormatProperties.properties().maxArrayLayers() >= VkPhysicalDeviceLimits.maxImageArrayLayers$get(limits));
		assertTrue(imageFormatProperties.properties().maxResourceSize() >= (1 >> 31));

		var chain = imageFormatProperties.nexts();
		assertEquals(1, chain.size());

		var externalImageFormatPropertiesResult = chain.getFirst();
		assertEquals(VkExternalImageFormatProperties.$LAYOUT().byteSize(), externalImageFormatPropertiesResult.byteSize());
		assertEquals(vulkan_h.VK_STRUCTURE_TYPE_EXTERNAL_IMAGE_FORMAT_PROPERTIES(), VkExternalImageFormatProperties.sType$get(externalImageFormatPropertiesResult));
	}

	@Test
	void getImageFormatPropertiesExtension() {
		var deviceProperties = device.getProperties();
		var limits = deviceProperties.limits();

		PhysicalDevice.ImageFormatInfoNext externalImageFormatInfo = (arena, next) -> {
			var result = VkPhysicalDeviceExternalImageFormatInfo.allocate(arena);
			VkPhysicalDeviceExternalImageFormatInfo.sType$set(result, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_IMAGE_FORMAT_INFO());
			VkPhysicalDeviceExternalImageFormatInfo.pNext$set(result, next);
			VkPhysicalDeviceExternalImageFormatInfo.handleType$set(result, switch(OS.current()) {
				case WINDOWS -> vulkan_h.VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT();
				case LINUX -> vulkan_h.VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT();
				default -> 0;
			});
			return result;
		};

		PhysicalDevice.ImageFormatPropertiesNext externalImageFormatProperties = (arena, next) -> {
			var result = VkExternalImageFormatProperties.allocate(arena);
			VkExternalImageFormatProperties.sType$set(result, vulkan_h.VK_STRUCTURE_TYPE_EXTERNAL_IMAGE_FORMAT_PROPERTIES());
			VkExternalImageFormatProperties.pNext$set(result, next);
			return result;
		};

		var extension = GetPhysicalDeviceProperties2Extension.extend(instance);
		var imageFormatProperties = device.getImageFormatProperties(vulkan_h.VK_FORMAT_R8G8B8A8_SRGB(),
				vulkan_h.VK_IMAGE_TYPE_2D(),
				vulkan_h.VK_IMAGE_TILING_OPTIMAL(),
				vulkan_h.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT(),
				0,
				List.of(externalImageFormatInfo),
				List.of(externalImageFormatProperties),
				extension);

		assertTrue(imageFormatProperties.properties().maxExtent().width() >= VkPhysicalDeviceLimits.maxImageDimension2D$get(limits));
		assertTrue(imageFormatProperties.properties().maxExtent().height() >= VkPhysicalDeviceLimits.maxImageDimension2D$get(limits));
		assertEquals(1, imageFormatProperties.properties().maxExtent().depth());

		int maximumDimension = Math.max(imageFormatProperties.properties().maxExtent().width(), imageFormatProperties.properties().maxExtent().height());
		int numberOfLayers = Integer.SIZE - Integer.numberOfLeadingZeros(maximumDimension);
		assertEquals(numberOfLayers, imageFormatProperties.properties().maxMipLevels());

		assertTrue(imageFormatProperties.properties().maxArrayLayers() >= VkPhysicalDeviceLimits.maxImageArrayLayers$get(limits));
		assertTrue(imageFormatProperties.properties().maxResourceSize() >= (1 >> 31));

		var chain = imageFormatProperties.nexts();
		assertEquals(1, chain.size());

		var externalImageFormatPropertiesResult = chain.getFirst();
		assertEquals(VkExternalImageFormatProperties.$LAYOUT().byteSize(), externalImageFormatPropertiesResult.byteSize());
		assertEquals(vulkan_h.VK_STRUCTURE_TYPE_EXTERNAL_IMAGE_FORMAT_PROPERTIES(), VkExternalImageFormatProperties.sType$get(externalImageFormatPropertiesResult));
	}
}
