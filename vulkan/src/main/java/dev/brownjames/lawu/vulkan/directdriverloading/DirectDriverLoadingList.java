package dev.brownjames.lawu.vulkan.directdriverloading;

import dev.brownjames.lawu.vulkan.InstanceCreateInfo;
import dev.brownjames.lawu.vulkan.NextStructure;
import dev.brownjames.lawu.vulkan.Structure;
import dev.brownjames.lawu.vulkan.StructureType;
import dev.brownjames.lawu.vulkan.bindings.VkDirectDriverLoadingInfoLUNARG;
import dev.brownjames.lawu.vulkan.bindings.VkDirectDriverLoadingListLUNARG;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.List;

public interface DirectDriverLoadingList extends InstanceCreateInfo.Next {
	record Value(DirectDriverLoadingMode mode, List<? extends VulkanDriver> drivers) implements DirectDriverLoadingList, Structure.Value {
		@Override
		public StructureType sType() {
			return StructureType.DIRECT_DRIVER_LOADING_LIST_LUNARG;
		}

		@Override
		public void asRaw(MemorySegment destination, SegmentAllocator allocator) {
			VkDirectDriverLoadingListLUNARG.sType$set(destination, sType().value());
			VkDirectDriverLoadingListLUNARG.pNext$set(destination, MemorySegment.NULL);
			VkDirectDriverLoadingListLUNARG.mode$set(destination, mode.value());
			VkDirectDriverLoadingListLUNARG.driverCount$set(destination, drivers.size());

			var driverStructures = VkDirectDriverLoadingInfoLUNARG.allocateArray(drivers.size(), allocator);
			for (int i = 0; i < drivers.size(); i++) {
				VkDirectDriverLoadingInfoLUNARG.sType$set(driverStructures, i, vulkan_h.VK_STRUCTURE_TYPE_DIRECT_DRIVER_LOADING_INFO_LUNARG());
				VkDirectDriverLoadingInfoLUNARG.pNext$set(driverStructures, i, MemorySegment.NULL);
				VkDirectDriverLoadingInfoLUNARG.flags$set(driverStructures, i, 0);
				VkDirectDriverLoadingInfoLUNARG.pfnGetInstanceProcAddr$set(driverStructures, i, drivers.get(i).getInstanceProcAddressPointer());
			}
			VkDirectDriverLoadingListLUNARG.pDrivers$set(destination, driverStructures);
		}

		@Override
		public MemorySegment asRaw(SegmentAllocator allocator) {
			var raw = VkDirectDriverLoadingInfoLUNARG.allocate(allocator);
			asRaw(raw, allocator);
			return raw;
		}

		@Override
		public DirectDriverLoadingList.Value asValue() {
			return this;
		}

		@Override
		public DirectDriverLoadingList.Native asNative() {
			return of(asRaw());
		}

		@Override
		public DirectDriverLoadingList.Native asNative(SegmentAllocator allocator) {
			return of(asRaw(allocator));
		}
	}

	record Native(MemorySegment raw) implements DirectDriverLoadingList, NextStructure.Native<InstanceCreateInfo.Next> {
		@Override
		public StructureType sType() {
			return StructureType.of(VkDirectDriverLoadingListLUNARG.sType$get(raw));
		}

		@Override
		public DirectDriverLoadingMode mode() {
			return DirectDriverLoadingMode.of(VkDirectDriverLoadingListLUNARG.mode$get(raw));
		}

		@Override
		public List<? extends VulkanDriver> drivers() {
			var rawArray = VkDirectDriverLoadingListLUNARG.pDrivers$get(raw);
			var count = VkDirectDriverLoadingListLUNARG.driverCount$get(raw);
			return rawArray.reinterpret(count * VkDirectDriverLoadingInfoLUNARG.sizeof())
					.elements(VkDirectDriverLoadingInfoLUNARG.$LAYOUT())
					.<VulkanDriver>map(m -> {
						assert VkDirectDriverLoadingInfoLUNARG.sType$get(m) == vulkan_h.VK_STRUCTURE_TYPE_DIRECT_DRIVER_LOADING_INFO_LUNARG();
						return () -> VkDirectDriverLoadingInfoLUNARG.pfnGetInstanceProcAddr$get(raw);
					})
					.toList();
		}

		@Override
		public DirectDriverLoadingList.Value asValue() {
			return of(mode(), drivers());
		}

		@Override
		public DirectDriverLoadingList.Native asNative() {
			return this;
		}

		@Override
		public DirectDriverLoadingList.Native asNative(SegmentAllocator allocator) {
			return this;
		}
	}

	StructureType sType();
	DirectDriverLoadingMode mode();
	List<? extends VulkanDriver> drivers();

	static Value of(DirectDriverLoadingMode mode, List<? extends VulkanDriver> drivers) {
		return new Value(mode, drivers);
	}

	static Native of(MemorySegment raw) {
		return new Native(raw);
	}

	static Native allocate(SegmentAllocator allocator) {
		return of(VkDirectDriverLoadingListLUNARG.allocate(allocator));
	}

	static Native allocate() {
		return allocate(Arena.ofAuto());
	}
}

