package dev.brownjames.lawu.vulkan;

/**
 * An exception thrown when validating Vulkan usage
 */
public class VulkanValidationException extends Exception {
	public VulkanValidationException(String message) {
		super(message);
	}
}
