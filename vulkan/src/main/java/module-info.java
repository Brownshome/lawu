module dev.brownjames.lawu.vulkan {
	requires transitive de.skuzzle.semantic;

	requires static dev.brownjames.lawu.vulkan.annotation;
	requires java.compiler;

	exports dev.brownjames.lawu.vulkan;
	exports dev.brownjames.lawu.vulkan.bindings;
	exports dev.brownjames.lawu.vulkan.debugutils;
	exports dev.brownjames.lawu.vulkan.directdriverloading;
	exports dev.brownjames.lawu.vulkan.getphysicaldeviceproperties2;
}
