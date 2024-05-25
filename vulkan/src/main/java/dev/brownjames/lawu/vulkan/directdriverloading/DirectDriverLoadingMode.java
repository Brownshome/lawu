package dev.brownjames.lawu.vulkan.directdriverloading;

import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

public enum DirectDriverLoadingMode {
	EXCLUSIVE(vulkan_h.VK_DIRECT_DRIVER_LOADING_MODE_EXCLUSIVE_LUNARG()),
	INCLUSIVE(vulkan_h.VK_DIRECT_DRIVER_LOADING_MODE_EXCLUSIVE_LUNARG());

	private final int value;

	DirectDriverLoadingMode(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	public static DirectDriverLoadingMode of(int value) {
		if (value == EXCLUSIVE.value()) {
			return EXCLUSIVE;
		} else if (value == INCLUSIVE.value()) {
			return INCLUSIVE;
		}

		throw new IllegalArgumentException();
	}
}
