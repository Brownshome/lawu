package dev.brownjames.lawu.vulkan;

public class VulkanException extends RuntimeException {
	private final int result;
	public VulkanException(int result) {
		super("Vulkan exception: %d".formatted(result));
		this.result = result;
	}

	public int result() {
		return result;
	}
}
