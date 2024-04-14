/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public enum QueueFlag implements BitFlag {
	GRAPHICS(vulkan_h.VK_QUEUE_GRAPHICS_BIT()),
	COMPUTE(vulkan_h.VK_QUEUE_COMPUTE_BIT()),
	TRANSFER(vulkan_h.VK_QUEUE_TRANSFER_BIT()),
	SPARSE_BINDING(vulkan_h.VK_QUEUE_SPARSE_BINDING_BIT()),
	PROTECTED(vulkan_h.VK_QUEUE_PROTECTED_BIT()),
	VIDEO_DECODE(vulkan_h.VK_QUEUE_VIDEO_DECODE_BIT_KHR()),
	VIDEO_ENCODE(vulkan_h.VK_QUEUE_VIDEO_ENCODE_BIT_KHR()),
	OPTICAL_FLOW(vulkan_h.VK_QUEUE_OPTICAL_FLOW_BIT_NV());

	private final int bit;

	QueueFlag(int bit) {
		this.bit = bit;
	}

	@Override
	public int bit() {
		return bit;
	}
}
