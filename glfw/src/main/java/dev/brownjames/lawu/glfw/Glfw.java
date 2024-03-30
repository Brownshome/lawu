package dev.brownjames.lawu.glfw;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.List;

import dev.brownjames.lawu.glfw.bindings.GLFWerrorfun;
import dev.brownjames.lawu.glfw.bindings.glfw3_h;

public record Glfw() implements AutoCloseable {
	private static final MemorySegment ERROR_CALLBACK = GLFWerrorfun.allocate((errorCode, description) -> {
		throw new GlfwException(errorCode, description.getUtf8String(0));
	}, Arena.global());

	public Glfw {
		glfw3_h.glfwInitVulkanLoader(SymbolLookup.loaderLookup().find("vkGetInstanceProcAddr")
				.orElseThrow(() -> new IllegalStateException("Unable to load Vulkan")));
		glfw3_h.glfwSetErrorCallback(ERROR_CALLBACK);
		if (glfw3_h.glfwInit() != glfw3_h.GLFW_TRUE()) {
			throw new GlfwException(glfw3_h.GLFW_NOT_INITIALIZED(), "Failed to initialise GLFW");
		}
	}

	public List<String> getRequiredInstanceExtensions() {
		try (var arena = Arena.ofConfined()) {
			var count = arena.allocate(glfw3_h.uint32_t);
			var rawExtensions = glfw3_h.glfwGetRequiredInstanceExtensions(count);
			rawExtensions = rawExtensions.reinterpret(glfw3_h.C_POINTER.byteSize() * count.get(glfw3_h.uint32_t, 0));

			return rawExtensions.elements(glfw3_h.C_POINTER)
					.map(e -> e.get(glfw3_h.C_POINTER, 0).getUtf8String(0))
					.toList();
		}
	}

	public boolean vulkanSupported() {
		return glfw3_h.glfwVulkanSupported() == glfw3_h.GLFW_TRUE();
	}

	@Override
	public void close() {
		glfw3_h.glfwTerminate();
	}
}
