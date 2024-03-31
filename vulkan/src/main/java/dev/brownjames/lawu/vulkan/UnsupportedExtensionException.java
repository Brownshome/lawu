package dev.brownjames.lawu.vulkan;

import java.util.Collection;
import java.util.stream.Collectors;

public class UnsupportedExtensionException extends VulkanValidationException {
	private final Collection<String> unsupportedExtensions;
	private final Collection<ExtensionProperties> availableExtensions;

	public UnsupportedExtensionException(Collection<String> unsupportedExtensions, Collection<ExtensionProperties> availableExtensions) {
		super(getMessage(unsupportedExtensions, availableExtensions));

		this.unsupportedExtensions = unsupportedExtensions;
		this.availableExtensions = availableExtensions;
	}

	private static String getMessage(Collection<String> unsupportedLayers, Collection<ExtensionProperties> availableLayers) {
		var unmatchedLayersString = unsupportedLayers.stream().collect(Collectors.joining(", ", "[", "]"));
		var availableLayersString = availableLayers.stream().map(ExtensionProperties::name).collect(Collectors.joining(", ", "[", "]"));
		return "Unsupported extensions %s requested %s available".formatted(unmatchedLayersString, availableLayersString);
	}

	public Collection<String> unsupportedExtensions() {
		return unsupportedExtensions;
	}

	public Collection<ExtensionProperties> availableExtensions() {
		return availableExtensions;
	}
}
