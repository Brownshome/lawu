package dev.brownjames.lawu.vulkan;

import java.lang.foreign.*;
import java.util.*;

import dev.brownjames.lawu.vulkan.bindings.*;

public final class VulkanInstance implements AutoCloseable, VulkanHandle {
	private static final System.Logger LOGGER = System.getLogger(String.valueOf(VulkanInstance.class.getModule().getName()));

	private final MemorySegment handle;
	private final MemorySegment allocator;
	private final Arena arena;

	private final InstanceFunctionLookup instanceFunctionLookup;

	private final PFN_vkDestroyInstance destroyInstance;
	private final PFN_vkEnumeratePhysicalDevices enumeratePhysicalDevices;
	private final PFN_vkGetPhysicalDeviceProperties getPhysicalDeviceProperties;
	private final PFN_vkGetPhysicalDeviceFeatures getPhysicalDeviceFeatures;
	private final PFN_vkGetPhysicalDeviceImageFormatProperties getPhysicalDeviceImageFormatProperties;
	private final PFN_vkGetPhysicalDeviceQueueFamilyProperties getPhysicalDeviceQueueFamilyProperties;

	private interface VersionedFunctionality {
		default void getPhysicalDeviceProperties2(MemorySegment device, MemorySegment properties) {
			throw new UnsupportedOperationException();
		}

		default void getPhysicalDeviceFeatures2(MemorySegment device, MemorySegment features) {
			throw new UnsupportedOperationException();
		}

		default int getPhysicalDeviceImageFormatProperties2(MemorySegment device, MemorySegment info, MemorySegment properties) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * An implementation of functionality specific to version zero
	 */
	private sealed class VersionZero implements VersionedFunctionality permits VersionOne { }

	/**
	 * An implementation of functionality specific to version one
	 */
	private sealed class VersionOne extends VersionZero permits VersionTwo {
		private final PFN_vkGetPhysicalDeviceProperties2 getPhysicalDeviceProperties2 = instanceFunctionLookup.lookup("vkGetPhysicalDeviceProperties2")
				.map(address -> PFN_vkGetPhysicalDeviceProperties2.ofAddress(address, arena))
				.orElseThrow();

		private final PFN_vkGetPhysicalDeviceFeatures2 getPhysicalDeviceFeatures2 = instanceFunctionLookup.lookup("vkGetPhysicalDeviceFeatures2")
				.map(address -> PFN_vkGetPhysicalDeviceFeatures2.ofAddress(address, arena))
				.orElseThrow();

		private final PFN_vkGetPhysicalDeviceImageFormatProperties2 getPhysicalDeviceImageFormatProperties2 = instanceFunctionLookup.lookup("vkGetPhysicalDeviceImageFormatProperties2")
				.map(address -> PFN_vkGetPhysicalDeviceImageFormatProperties2.ofAddress(address, arena))
				.orElseThrow();

		@Override
		public void getPhysicalDeviceProperties2(MemorySegment device, MemorySegment properties) {
			getPhysicalDeviceProperties2.apply(device, properties);
		}

		@Override
		public void getPhysicalDeviceFeatures2(MemorySegment device, MemorySegment features) {
			getPhysicalDeviceFeatures2.apply(device, features);
		}

		@Override
		public int getPhysicalDeviceImageFormatProperties2(MemorySegment device, MemorySegment info, MemorySegment properties) {
			return getPhysicalDeviceImageFormatProperties2.apply(device, info, properties);
		}
	}

	/**
	 * An implementation of functionality specific to version two
	 */
	private sealed class VersionTwo extends VersionOne permits VersionThree { }

	/**
	 * An implementation of functionality specific to version three
	 */
	private final class VersionThree extends VersionTwo { }

	private final VersionedFunctionality versionedFunctionality;

	public static InstanceCreateInfo builder() {
		return new InstanceCreateInfo();
	}

	public static VulkanInstance create() {
		return builder().build();
	}

	public static VulkanInstance create(MemorySegment instanceCreateInfo, VulkanVersionNumber version) {
		return new VulkanInstance(Vulkan.createInstance(instanceCreateInfo), version);
	}

	public VulkanInstance(MemorySegment handle, VulkanVersionNumber version) {
		this.handle = handle;
		this.allocator = MemorySegment.NULL;
		this.arena = Arena.ofConfined();

		instanceFunctionLookup = Vulkan.globalFunctionLookup()
				.instanceFunctionLookup(this);

		destroyInstance = instanceFunctionLookup
				.lookup("vkDestroyInstance")
				.map(address -> PFN_vkDestroyInstance.ofAddress(address, arena))
				.orElseThrow();

		enumeratePhysicalDevices = instanceFunctionLookup
				.lookup("vkEnumeratePhysicalDevices")
				.map(address -> PFN_vkEnumeratePhysicalDevices.ofAddress(address, arena))
				.orElseThrow();

		getPhysicalDeviceProperties = instanceFunctionLookup
				.lookup("vkGetPhysicalDeviceProperties")
				.map(address -> PFN_vkGetPhysicalDeviceProperties.ofAddress(address, arena))
				.orElseThrow();

		getPhysicalDeviceFeatures = instanceFunctionLookup
				.lookup("vkGetPhysicalDeviceFeatures")
				.map(address -> PFN_vkGetPhysicalDeviceFeatures.ofAddress(address, arena))
				.orElseThrow();

		getPhysicalDeviceImageFormatProperties = instanceFunctionLookup
				.lookup("vkGetPhysicalDeviceImageFormatProperties")
				.map(address -> PFN_vkGetPhysicalDeviceImageFormatProperties.ofAddress(address, arena))
				.orElseThrow();

		getPhysicalDeviceQueueFamilyProperties = instanceFunctionLookup
				.lookup("vkGetPhysicalDeviceQueueFamilyProperties")
				.map(address -> PFN_vkGetPhysicalDeviceQueueFamilyProperties.ofAddress(address, arena))
				.orElseThrow();

		assert version.major() == VulkanVersionNumber.headerVersion().major() && version.isStandardVariant();

		versionedFunctionality = switch (version.minor()) {
			case 0 -> new VersionZero();
			case 1 -> new VersionOne();
			case 2 -> new VersionTwo();
			default -> new VersionThree();
		};
	}

	@Override
	public MemorySegment handle() {
		return handle;
	}

	public MemorySegment allocator() {
		return allocator;
	}

	public Arena arena() {
		return arena;
	}

	public InstanceFunctionLookup instanceFunctionLookup() {
		return instanceFunctionLookup;
	}

	public List<PhysicalDevice> enumeratePhysicalDevices() {
		try (var arena = Arena.ofConfined()) {
			var deviceCount = arena.allocate(vulkan_h.uint32_t);
			Vulkan.checkResult(enumeratePhysicalDevices.apply(handle, deviceCount, MemorySegment.NULL));

			var devices = arena.allocateArray(vulkan_h.VkPhysicalDevice, deviceCount.get(vulkan_h.uint32_t, 0L));
			int result = Vulkan.checkResult(enumeratePhysicalDevices.apply(handle, deviceCount, devices));
			assert result == vulkan_h.VK_SUCCESS();

			return devices.elements(vulkan_h.VkPhysicalDevice)
					.map(device -> new PhysicalDevice(device.get(vulkan_h.VkPhysicalDevice, 0), this))
					.toList();
		}
	}

	public void getPhysicalDeviceProperties(MemorySegment device, MemorySegment properties) {
		getPhysicalDeviceProperties.apply(device, properties);
	}

	public void getPhysicalDeviceProperties2(MemorySegment device, MemorySegment properties) {
		versionedFunctionality.getPhysicalDeviceProperties2(device, properties);
	}

	public void getPhysicalDeviceFeatures(MemorySegment device, MemorySegment features) {
		getPhysicalDeviceFeatures.apply(device, features);
	}

	public void getPhysicalDeviceFeatures2(MemorySegment device, MemorySegment features) {
		versionedFunctionality.getPhysicalDeviceFeatures2(device, features);
	}

	public void getPhysicalDeviceImageFormatProperties(MemorySegment device,
	                                                    int format,
	                                                    int imageType,
	                                                    int imageTiling,
	                                                    int imageUsageFlags,
	                                                    int imageCreateFlags,
	                                                    MemorySegment properties) {
		Vulkan.checkResult(getPhysicalDeviceImageFormatProperties.apply(device,
				format,
				imageType,
				imageTiling,
				imageUsageFlags,
				imageCreateFlags,
				properties));
	}

	public void getPhysicalDeviceImageFormatProperties2(MemorySegment device, MemorySegment info, MemorySegment properties) {
		Vulkan.checkResult(versionedFunctionality.getPhysicalDeviceImageFormatProperties2(device, info, properties));
	}

	public void getPhysicalDeviceQueueFamilyProperties(MemorySegment device, MemorySegment count, MemorySegment properties) {
		getPhysicalDeviceQueueFamilyProperties.apply(device, count, properties);
	}

	@Override
	public void close() {
		destroyInstance.apply(handle, allocator);
		arena.close();
	}
}
