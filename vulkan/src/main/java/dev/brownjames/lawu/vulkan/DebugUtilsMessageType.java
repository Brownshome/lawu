package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public enum DebugUtilsMessageType implements BitFlag {
	GENERAL(vulkan_h.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT()),
	VALIDATION(vulkan_h.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT()),
	PERFORMANCE(vulkan_h.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT()),
	DEVICE_ADDRESS_BINDING(vulkan_h.VK_DEBUG_UTILS_MESSAGE_TYPE_DEVICE_ADDRESS_BINDING_BIT_EXT());

	private final int bit;

	DebugUtilsMessageType(int bit) {
		this.bit = bit;
	}

	@Override
	public int bit() {
		return bit;
	}
}
