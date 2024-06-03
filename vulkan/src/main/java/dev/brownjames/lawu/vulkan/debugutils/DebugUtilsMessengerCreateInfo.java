package dev.brownjames.lawu.vulkan.debugutils;

import dev.brownjames.lawu.vulkan.*;
import dev.brownjames.lawu.vulkan.bindings.PFN_vkDebugUtilsMessengerCallbackEXT;
import dev.brownjames.lawu.vulkan.bindings.VkDebugUtilsMessengerCreateInfoEXT;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.*;
import java.util.Collection;

public interface DebugUtilsMessengerCreateInfo extends InstanceCreateInfo.Next {
	Collection<DebugUtilsMessageSeverity> severities();
	Collection<DebugUtilsMessageType> types();
	DebugUtilsMessengerCallback callback();

	record Value(
		Collection<DebugUtilsMessageSeverity> severities,
		Collection<DebugUtilsMessageType> types,
		DebugUtilsMessengerCallback callback
	) implements DebugUtilsMessengerCreateInfo, Structure.Value {
		@Override
		public StructureType sType() {
			return StructureType.DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
		}

		@Override
		public void asRaw(MemorySegment destination, SegmentAllocator allocator) {
			if (!(allocator instanceof Arena arena)) {
				throw new IllegalArgumentException("The allocator must be an arena to allocate the up-call stub");
			}

			asRaw(destination, arena);
		}

		public void asRaw(MemorySegment destination, Arena arena) {
			VkDebugUtilsMessengerCreateInfoEXT.sType$set(destination, sType().value());
			VkDebugUtilsMessengerCreateInfoEXT.pNext$set(destination, MemorySegment.NULL);
			VkDebugUtilsMessengerCreateInfoEXT.flags$set(destination, 0);

			VkDebugUtilsMessengerCreateInfoEXT.messageSeverity$set(destination, BitFlag.getFlagBits(severities));
			VkDebugUtilsMessengerCreateInfoEXT.messageType$set(destination, BitFlag.getFlagBits(types));

			VkDebugUtilsMessengerCreateInfoEXT.pfnUserCallback$set(destination,
					PFN_vkDebugUtilsMessengerCallbackEXT.allocate((int severity, int flags, MemorySegment callbackData, MemorySegment _) -> {
						var severitySet = BitFlag.flags(severity, DebugUtilsMessageSeverity.class);
						assert severitySet.size() == 1;
						var severityEnum = severitySet.iterator().next();

						callback.callback(severityEnum, flags, callbackData);

						return vulkan_h.VK_FALSE();
					}, arena));

			VkDebugUtilsMessengerCreateInfoEXT.pUserData$set(destination, MemorySegment.NULL);
		}

		@Override
		public MemorySegment asRaw(SegmentAllocator allocator) {
			var raw = VkDebugUtilsMessengerCreateInfoEXT.allocate(allocator);
			asRaw(raw, allocator);
			return raw;
		}

		@Override
		public DebugUtilsMessengerCreateInfo.Native asNative(SegmentAllocator allocator) {
			return of(asRaw(allocator));
		}
	}

	record Native(MemorySegment raw) implements DebugUtilsMessengerCreateInfo, NextStructure.Native<InstanceCreateInfo.Next> {
		@Override
		public Structure.Value asValue() {
			return of(severities(), types(), callback());
		}

		@Override
		public Collection<DebugUtilsMessageSeverity> severities() {
			return BitFlag.flags(VkDebugUtilsMessengerCreateInfoEXT.messageSeverity$get(raw), DebugUtilsMessageSeverity.class);
		}

		@Override
		public Collection<DebugUtilsMessageType> types() {
			return BitFlag.flags(VkDebugUtilsMessengerCreateInfoEXT.messageType$get(raw), DebugUtilsMessageType.class);
		}

		public DebugUtilsMessengerCallback callback(Arena arena) {
			var callback = VkDebugUtilsMessengerCreateInfoEXT.pfnUserCallback(raw, arena);
			return (severity, messageTypes, callbackData) -> {
				callback.apply(severity.bit(), messageTypes, callbackData, MemorySegment.NULL);
			};
		}

		@Override
		public DebugUtilsMessengerCallback callback() {
			// We know nothing about the memory containing this callback, assume it is static
			return callback(Arena.global());
		}
	}

	static Native of(MemorySegment raw) {
		return new Native(raw);
	}

	static Value of(Collection<DebugUtilsMessageSeverity> severities,
	                Collection<DebugUtilsMessageType> types,
	                DebugUtilsMessengerCallback callback) {
		return new Value(severities, types, callback);
	}
}
