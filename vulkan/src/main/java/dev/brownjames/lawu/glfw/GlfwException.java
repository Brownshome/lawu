package dev.brownjames.lawu.glfw;

public class GlfwException extends RuntimeException {
	private final int errorCode;
	private final String description;

	public GlfwException(int errorCode, String description) {
		super("GLFW Error (%d): %s".formatted(errorCode, description));

		this.errorCode = errorCode;
		this.description = description;
	}

	public int errorCode() {
		return errorCode;
	}

	public String description() {
		return description;
	}
}
