package dev.brownjames.lawu.vulkan.getphysicaldeviceproperties2;

import dev.brownjames.lawu.vulkan.*;
import dev.brownjames.lawu.vulkan.bindings.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

public final class GetPhysicalDeviceProperties2Extension implements VulkanHandle {
	private final MemorySegment handle;

	private final PFN_vkGetPhysicalDeviceProperties2KHR getPhysicalDeviceProperties2KHR;
	private final PFN_vkGetPhysicalDeviceFeatures2KHR getPhysicalDeviceFeatures2KHR;
	private final PFN_vkGetPhysicalDeviceImageFormatProperties2KHR getPhysicalDeviceImageFormatProperties2KHR;

	public static GetPhysicalDeviceProperties2Extension extend(VulkanInstance instance) {
		return new GetPhysicalDeviceProperties2Extension(instance.handle(), instance.arena(), instance.instanceFunctionLookup());
	}

	public GetPhysicalDeviceProperties2Extension(MemorySegment handle, Arena arena, InstanceFunctionLookup lookup) {
		this.handle = handle;

		getPhysicalDeviceProperties2KHR = lookup.lookup("vkGetPhysicalDeviceProperties2KHR")
				.map(address -> PFN_vkGetPhysicalDeviceProperties2KHR.ofAddress(address, arena))
				.orElseThrow();

		getPhysicalDeviceFeatures2KHR = lookup.lookup("vkGetPhysicalDeviceFeatures2KHR")
				.map(address -> PFN_vkGetPhysicalDeviceFeatures2KHR.ofAddress(address, arena))
				.orElseThrow();

		getPhysicalDeviceImageFormatProperties2KHR = lookup.lookup("vkGetPhysicalDeviceImageFormatProperties2KHR")
				.map(address -> PFN_vkGetPhysicalDeviceImageFormatProperties2KHR.ofAddress(address, arena))
				.orElseThrow();
	}

	public static String extensionName() {
		return "VK_KHR_get_physical_device_properties2";
	}

	@Override
	public MemorySegment handle() {
		return handle;
	}

	public void getPhysicalDeviceProperties2(MemorySegment device, MemorySegment properties) {
		getPhysicalDeviceProperties2KHR.apply(device, properties);
	}

	public void getPhysicalDeviceFeatures2(MemorySegment device, MemorySegment features) {
		getPhysicalDeviceFeatures2KHR.apply(device, features);
	}

	public void getPhysicalDeviceImageFormatProperties2(MemorySegment device, MemorySegment imageFormatInfo, MemorySegment imageFormatProperties) {
		Vulkan.checkResult(getPhysicalDeviceImageFormatProperties2KHR.apply(device, imageFormatInfo, imageFormatProperties));
	}
}
