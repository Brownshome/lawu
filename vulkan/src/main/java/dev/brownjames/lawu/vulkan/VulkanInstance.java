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

	public static InstanceCreateInfo builder() {
		return new InstanceCreateInfo();
	}

	public static VulkanInstance create() {
		return builder().build();
	}

	public static VulkanInstance create(MemorySegment instanceCreateInfo) {
		return new VulkanInstance(Vulkan.createInstance(instanceCreateInfo));
	}

	public VulkanInstance(MemorySegment handle) {
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

	public List<PhysicalDevice> allPhysicalDevices() {
		try (var arena = Arena.ofConfined()) {
			var deviceCount = arena.allocate(vulkan_h.uint32_t);
			Vulkan.checkResult(enumeratePhysicalDevices.apply(handle, deviceCount, MemorySegment.NULL));

			var devices = arena.allocateArray(vulkan_h.VkPhysicalDevice, deviceCount.get(vulkan_h.uint32_t, 0L));
			int result = Vulkan.checkResult(enumeratePhysicalDevices.apply(handle, deviceCount, devices));
			assert result == vulkan_h.VK_SUCCESS();

			return devices.elements(vulkan_h.VkPhysicalDevice)
					.map(PhysicalDevice::new)
					.toList();
		}
	}

	@Override
	public void close() {
		destroyInstance.apply(handle, allocator);
		arena.close();
	}
}
