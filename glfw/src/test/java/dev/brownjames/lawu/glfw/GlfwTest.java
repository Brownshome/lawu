package dev.brownjames.lawu.glfw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GlfwTest {
	@Test
	void init() {
		try (var _ = new Glfw()) { }
	}

	@Test
	void getRequiredInstanceExtensions() {
		try (var glfw = new Glfw()) {
			glfw.getRequiredInstanceExtensions();
		}
	}

	@Test
	void vulkanSupported() {
		try (var glfw = new Glfw()){
			assertTrue(glfw.vulkanSupported());
		}
	}
}
