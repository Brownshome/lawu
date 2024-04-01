package dev.brownjames.lawu.vulkan;

import java.util.Collection;
import java.util.stream.Collectors;

public class UnsupportedLayerException extends VulkanValidationException {
	private final Collection<String> unsupportedLayers;
	private final Collection<LayerProperties> availableLayers;

	public UnsupportedLayerException(Collection<String> unsupportedLayers, Collection<LayerProperties> availableLayers) {
		super(getMessage(unsupportedLayers, availableLayers));

		this.unsupportedLayers = unsupportedLayers;
		this.availableLayers = availableLayers;
	}

	private static String getMessage(Collection<String> unsupportedLayers, Collection<LayerProperties> availableLayers) {
		var unmatchedLayersString = unsupportedLayers.stream().collect(Collectors.joining(", ", "[", "]"));
		var availableLayersString = availableLayers.stream().map(LayerProperties::name).collect(Collectors.joining(", ", "[", "]"));
		return "Unsupported layers %s requested %s available".formatted(unmatchedLayersString, availableLayersString);
	}

	public Collection<String> unsupportedLayers() {
		return unsupportedLayers;
	}

	public Collection<LayerProperties> availableLayers() {
		return availableLayers;
	}
}
