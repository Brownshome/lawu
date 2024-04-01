package dev.brownjames.lawu.vulkan.debugutils;

import dev.brownjames.lawu.vulkan.BitFlag;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public enum DebugUtilsMessageSeverity implements BitFlag {
	VERBOSE(vulkan_h.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT()),
	INFO(vulkan_h.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT()),
	WARNING(vulkan_h.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT()),
	ERROR(vulkan_h.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT());

	private final int bit;

	DebugUtilsMessageSeverity(int bit) {
		this.bit = bit;
	}

	@Override
	public int bit() {
		return bit;
	}
}
