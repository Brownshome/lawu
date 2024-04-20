package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public enum SampleCountFlag implements BitFlag {
	ONE(vulkan_h.VK_SAMPLE_COUNT_1_BIT()),
	TWO(vulkan_h.VK_SAMPLE_COUNT_2_BIT()),
	FOUR(vulkan_h.VK_SAMPLE_COUNT_4_BIT()),
	EIGHT(vulkan_h.VK_SAMPLE_COUNT_8_BIT()),
	SIXTEEN(vulkan_h.VK_SAMPLE_COUNT_16_BIT()),
	THIRTY_TWO(vulkan_h.VK_SAMPLE_COUNT_32_BIT()),
	SIXTY_FOUR(vulkan_h.VK_SAMPLE_COUNT_64_BIT());

	private final int bit;

	SampleCountFlag(int bit) {
		this.bit = bit;
	}

	@Override
	public int bit() {
		return bit;
	}
}
