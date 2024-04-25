package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import dev.brownjames.lawu.vulkan.generator.ElementLookup;

/**
 * A class holding information about the requested generation environment
 */
final class GenerationContext {
	private final ProcessingEnvironment processingEnvironment;
	private final ElementLookup lookup;

	private TypeElement vulkanHeader;
	private final Map<String, NameMapping> mappings;

	GenerationContext(ProcessingEnvironment processingEnvironment, ElementLookup lookup) {
		assert processingEnvironment != null;

		this.processingEnvironment = processingEnvironment;
		this.lookup = lookup;
		this.vulkanHeader = null;
		this.mappings = new HashMap<>();
	}

	ElementLookup lookup() {
		return lookup;
	}

	ProcessingEnvironment processingEnvironment() {
		return processingEnvironment;
	}

	void addHeader(TypeMirror vulkanHeader) throws IllegalStateException, IllegalArgumentException {
		assert vulkanHeader != null;

		if (vulkanHeader.getKind() != TypeKind.DECLARED) {
			throw new IllegalArgumentException(STR."Vulkan header \{vulkanHeader} is not a declared type");
		}

		if (this.vulkanHeader != null) {
			throw new IllegalStateException(STR."Duplicate Vulkan headers defined, \{vulkanHeader}, and \{this.vulkanHeader}");
		}

		var element = processingEnvironment.getTypeUtils().asElement(vulkanHeader);
		if (!(element instanceof TypeElement vulkanHeaderElement) || !element.getKind().isDeclaredType()) {
			throw new IllegalArgumentException(STR."No declaration found for \{vulkanHeader}");
		}

		this.vulkanHeader = vulkanHeaderElement;
	}

	void addMapping(CharSequence target, TypeElement mapping) throws IllegalStateException {
		var targetString = target.toString();

		var oldMapping = mappings.remove(targetString);
		switch (oldMapping) {
			case GenerationRequest request -> mappings.put(targetString, new ReplacementMapping(request.target(), mapping));
			case ExistingMapping existingMapping -> throw new IllegalStateException(STR."Duplicate mappings defined for \{targetString}, \{mapping}, and \{existingMapping.mapping()}");
			case null -> mappings.put(targetString, new NewMapping(target, mapping));
		}
	}

	void addGenerationRequest(TypeElement target, PackageElement destination, CharSequence newName) throws IllegalStateException {
		var targetString = target.getSimpleName().toString();

		checkForAndAddExistingMapping(target, destination, newName);

		var oldMapping = mappings.remove(targetString);
		switch (oldMapping) {
			case TargetedMapping targetedMapping -> throw new IllegalStateException(STR."Both \{targetedMapping.target()} and \{target} have the same name, \{target.getSimpleName()}");
			case NewMapping newMapping -> {
				mappings.put(targetString, new ReplacementMapping(target, newMapping.mapping()));
				processingEnvironment.getMessager().printNote(STR."Skipping generating \{destination}.\{newName} as \{target} has been mapped to \{newMapping.mapping()}", newMapping.mapping());
			}
			case null -> mappings.put(targetString, new GenerationRequest(target, destination, newName));
		}
	}

	private void checkForAndAddExistingMapping(TypeElement target, PackageElement destination, CharSequence newName) {
		var qualifiedName = STR."\{destination}.\{newName}";
		var module = processingEnvironment.getElementUtils().getModuleOf(destination);
		TypeElement existingElement = module == null
				? processingEnvironment.getElementUtils().getTypeElement(qualifiedName)
				: processingEnvironment.getElementUtils().getTypeElement(module, qualifiedName);

		if (existingElement != null) {
			addMapping(target.getSimpleName(), existingElement);
		}
	}

	public Iterable<GenerationRequest> generationRequests() {
		return () -> mappings.values().stream().<GenerationRequest>mapMulti((nameMapping, objectConsumer) -> {
			if (nameMapping instanceof GenerationRequest request) {
				objectConsumer.accept(request);
			}
		}).iterator();
	}

	public Optional<NameMapping> mapping(CharSequence name) {
		return Optional.ofNullable(mappings.get(name.toString()));
	}

	public Optional<VariableElement> getLayout(CharSequence type) {
		if (vulkanHeader == null) {
			throw new IllegalStateException("No vulkan header specified");
		}

		var fields = ElementFilter.fieldsIn(processingEnvironment.getElementUtils().getAllMembers(vulkanHeader)).stream()
				.filter(field -> field.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC))
						&& field.getSimpleName().contentEquals(type))
				.toList();

		if (fields.isEmpty()) {
			return Optional.empty();
		}

		if (fields.size() > 1) {
			throw new RuntimeException(STR."Multiple layout fields for \{type}");
		}

		return Optional.of(fields.getFirst());
	}
}
