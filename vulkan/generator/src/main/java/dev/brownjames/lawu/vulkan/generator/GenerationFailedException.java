package dev.brownjames.lawu.vulkan.generator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public final class GenerationFailedException extends Exception {
	private final Element element;

	public GenerationFailedException(String message, Element element, Throwable e) {
		super(message, e);
		this.element = element;
	}

	public GenerationFailedException(String message, Element element) {
		super(message);
		this.element = element;
	}

	public void raise(ProcessingEnvironment processingEnv) {
		if (element != null) {
			processingEnv.getMessager().printError(getMessage(), element);
		} else {
			processingEnv.getMessager().printError(getMessage());
		}
	}

	public final class Unchecked extends RuntimeException {
		public Unchecked() {
			super(GenerationFailedException.this);
		}

		public GenerationFailedException getCause() {
			return (GenerationFailedException) super.getCause();
		}
	}

	public Unchecked unchecked() {
		return new Unchecked();
	}
}
