package dev.brownjames.lawu.bindings.glfw;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.SymbolLookup;
import java.util.List;

import static dev.brownjames.lawu.bindings.glfw.glfw3_h.*;

public record Glfw() implements AutoCloseable {
	private static final MemorySegment ERROR_CALLBACK = GLFWerrorfun.allocate((errorCode, description) -> {
		throw new GlfwException(errorCode, description.getUtf8String(0));
	}, SegmentScope.global());

	public Glfw {
		glfwInitVulkanLoader(SymbolLookup.loaderLookup().find("vkGetInstanceProcAddr")
				.orElseThrow(() -> new IllegalStateException("Unable to load Vulkan")));
		glfwSetErrorCallback(ERROR_CALLBACK);
		if (glfwInit() != GLFW_TRUE()) {
			throw new GlfwException(GLFW_NOT_INITIALIZED(), "Failed to initialise GLFW");
		}
	}

	public List<String> getRequiredInstanceExtensions() {
		try (var arena = Arena.openConfined()) {
			var count = arena.allocate(uint32_t);
			var rawExtensions = glfw3_h.glfwGetRequiredInstanceExtensions(count);
			rawExtensions = MemorySegment.ofAddress(rawExtensions.address(), C_POINTER.byteSize() * count.get(uint32_t, 0));

			return rawExtensions.elements(C_POINTER)
					.map(e -> e.get(C_POINTER, 0).getUtf8String(0))
					.toList();
		}
	}

	public boolean vulkanSupported() {
		return glfwVulkanSupported() == GLFW_TRUE();
	}

	@Override
	public void close() {
		glfwTerminate();
	}
}
