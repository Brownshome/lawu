package dev.brownjames.lawu.vulkan.directdriverloading;

import dev.brownjames.lawu.vulkan.InstanceCreateInfo;
import dev.brownjames.lawu.vulkan.bindings.VkDirectDriverLoadingInfoLUNARG;
import dev.brownjames.lawu.vulkan.bindings.VkDirectDriverLoadingListLUNARG;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.List;

public record DirectDriverLoadingList(DirectDriverLoadingMode mode, List<VulkanDriver> drivers) implements InstanceCreateInfo.Next {
	@Override
	public MemorySegment createNativeStructure(Arena arena, MemorySegment next) {
		var structure = VkDirectDriverLoadingListLUNARG.allocate(arena);
		VkDirectDriverLoadingListLUNARG.sType$set(structure, vulkan_h.VK_STRUCTURE_TYPE_DIRECT_DRIVER_LOADING_LIST_LUNARG());
		VkDirectDriverLoadingListLUNARG.pNext$set(structure, next);
		VkDirectDriverLoadingListLUNARG.mode$set(structure, mode.value());
		VkDirectDriverLoadingListLUNARG.driverCount$set(structure, drivers.size());

		var driverStructures = VkDirectDriverLoadingInfoLUNARG.allocateArray(drivers.size(), arena);
		for (int i = 0; i < drivers.size(); i++) {
			VkDirectDriverLoadingInfoLUNARG.sType$set(driverStructures, i, vulkan_h.VK_STRUCTURE_TYPE_DIRECT_DRIVER_LOADING_INFO_LUNARG());
			VkDirectDriverLoadingInfoLUNARG.pNext$set(driverStructures, i, MemorySegment.NULL);
			VkDirectDriverLoadingInfoLUNARG.flags$set(driverStructures, i, 0);
			VkDirectDriverLoadingInfoLUNARG.pfnGetInstanceProcAddr$set(driverStructures, i, drivers.get(i).getInstanceProcAddressPointer());
		}
		VkDirectDriverLoadingListLUNARG.pDrivers$set(structure, driverStructures);

		return structure;
	}
}
