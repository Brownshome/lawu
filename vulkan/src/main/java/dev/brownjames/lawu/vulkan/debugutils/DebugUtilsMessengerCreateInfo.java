package dev.brownjames.lawu.vulkan.debugutils;

import dev.brownjames.lawu.vulkan.BitFlag;
import dev.brownjames.lawu.vulkan.InstanceCreateInfo;
import dev.brownjames.lawu.vulkan.bindings.VkDebugUtilsMessengerCallbackDataEXT;
import dev.brownjames.lawu.vulkan.bindings.VkDebugUtilsMessengerCreateInfoEXT;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public record DebugUtilsMessengerCreateInfo(
		Collection<DebugUtilsMessageSeverity> severities,
		Collection<DebugUtilsMessageType> types,
		DebugUtilsMessengerCallback callback
) implements InstanceCreateInfo.Next {
	@Override
	public MemorySegment createNativeStructure(Arena arena, MemorySegment next) {
		var structure = VkDebugUtilsMessengerCreateInfoEXT.allocate(arena);
		VkDebugUtilsMessengerCreateInfoEXT.sType$set(structure, vulkan_h.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT());
		VkDebugUtilsMessengerCreateInfoEXT.pNext$set(structure, next);
		VkDebugUtilsMessengerCreateInfoEXT.flags$set(structure, 0);

		VkDebugUtilsMessengerCreateInfoEXT.messageSeverity$set(structure, BitFlag.getFlagBits(severities));
		VkDebugUtilsMessengerCreateInfoEXT.messageType$set(structure, BitFlag.getFlagBits(types));

		var adaptor = new Object() {
			int callback(int severity, int flags, MemorySegment callbackData, MemorySegment userData) {
				var severitySet = BitFlag.flags(severity, DebugUtilsMessageSeverity.class);
				assert severitySet.size() == 1;
				var severityEnum = severitySet.iterator().next();

				callback.callback(severityEnum, flags, callbackData);

				return vulkan_h.VK_FALSE();
			}
		};

		try {
			var upCallMethod = Linker.nativeLinker().upcallStub(
					MethodHandles.lookup().bind(adaptor, "callback", MethodType.methodType(Integer.TYPE, Integer.TYPE, Integer.TYPE, MemorySegment.class, MemorySegment.class)),
					FunctionDescriptor.of(vulkan_h.VkBool32,
							JAVA_INT,
							JAVA_INT,
							AddressLayout.ADDRESS.withTargetLayout(VkDebugUtilsMessengerCallbackDataEXT.$LAYOUT()),
							AddressLayout.ADDRESS),
					Arena.global());

			VkDebugUtilsMessengerCreateInfoEXT.pfnUserCallback$set(structure, upCallMethod);
		} catch (NoSuchMethodException | IllegalAccessException e) {
			throw new AssertionError(e);
		}

		VkDebugUtilsMessengerCreateInfoEXT.pUserData$set(structure, MemorySegment.NULL);

		return structure;
	}

	public MemorySegment createNativeStructure(Arena arena) {
		return createNativeStructure(arena, MemorySegment.NULL);
	}
}
