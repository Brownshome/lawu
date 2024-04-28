package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import dev.brownjames.lawu.vulkan.generator.ElementLookup;

/**
 * A class holding information about the requested generation environment
 */
final class GenerationContext {
	private final ProcessingEnvironment processingEnvironment;
	private final ElementLookup lookup;

	private TypeElement vulkanHeader;
	private Map<String, VariableElement> vulkanHeaderFields;
	private Map<String, ExecutableElement> vulkanHeaderMethods;

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
		vulkanHeaderFields = new HashMap<>();
		vulkanHeaderMethods = new HashMap<>();

		for (var e : processingEnvironment.getElementUtils().getAllMembers(this.vulkanHeader)) {
			if (!e.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC))) {
				continue;
			}

			switch (e) {
				case VariableElement variable
						when e.getKind() == ElementKind.FIELD -> {
					if (vulkanHeaderFields.put(e.getSimpleName().toString(), variable) != null) {
						throw new IllegalStateException(STR."Duplicate header fields found for \{e.getSimpleName()}");
					}
				}
				case ExecutableElement executable
						when e.getKind() == ElementKind.METHOD && executable.getParameters().isEmpty() -> {
					if (vulkanHeaderMethods.put(e.getSimpleName().toString(), executable) != null) {
						throw new IllegalStateException(STR."Duplicate header methods found for \{e.getSimpleName()}");
					}
				}
				default -> { }
			}
		}
	}

	void addMapping(CharSequence target, TypeElement mapping) throws IllegalStateException {
		var targetString = target.toString();

		var oldMapping = mappings.remove(targetString);
		switch (oldMapping) {
			case StructureGenerationRequest request -> {
				mappings.put(targetString, new ReplacementMapping(request.target(), mapping));
				processingEnvironment.getMessager().printNote(STR."Skipping generating \{request.qualifiedName()} as \{target} has been mapped to \{mapping}", mapping);
			}
			case FunctionPointerMapping functionPointerMapping -> mappings.put(targetString, new ReplacementMapping(functionPointerMapping.target(), mapping));
			case FlagGenerationRequest flagGenerationRequest -> {
				mappings.put(targetString, new NewMapping(flagGenerationRequest.target(), mapping));
				processingEnvironment.getMessager().printNote(STR."Skipping generating \{flagGenerationRequest.qualifiedName()} as \{target} has been mapped to \{mapping}", mapping);
			}
			case null -> mappings.put(targetString, new NewMapping(target, mapping));
			default -> throw new IllegalStateException(STR."Duplicate mappings defined for \{targetString}, \{mapping}, and \{oldMapping.qualifiedName()}");
		}
	}

	void addFunctionPointer(TypeElement target) {
		var targetString = target.getSimpleName().toString();

		var oldMapping = mappings.remove(targetString);
		switch (oldMapping) {
			case NewMapping newMapping -> mappings.put(targetString, new ReplacementMapping(target, newMapping.mapping()));
			case null -> mappings.put(targetString, new FunctionPointerMapping(target));
			default -> throw new IllegalStateException(STR."Both \{oldMapping.target()} and \{target} have the same name, \{target.getSimpleName()}");
		}
	}

	private void addGenerationRequest(GenerationRequest request) {
		var targetString = switch (request.target()) {
			case TypeElement element -> element.getSimpleName().toString();
			case null, default -> request.target().toString();
		};

		checkForAndAddExistingMapping(targetString, request.destination(), request.name());

		var oldMapping = mappings.remove(targetString);
		switch (oldMapping) {
			case NewMapping newMapping -> {
				mappings.put(targetString, switch (request.target()) {
					case TypeElement element -> new ReplacementMapping(element, newMapping.mapping());
					case null, default -> newMapping;
				});
				processingEnvironment.getMessager().printNote(STR."Skipping generating \{request.qualifiedName()} as \{targetString} has been mapped to \{newMapping.mapping()}", newMapping.mapping());
			}
			case null -> mappings.put(targetString, request);
			default -> throw new IllegalStateException(STR."Both \{oldMapping.target()} and \{request.target()} have the same name, \{targetString}");
		}
	}

	void addStructureGenerationRequest(TypeElement target, PackageElement destination, CharSequence newName) throws IllegalStateException {
		addGenerationRequest(new StructureGenerationRequest(target, destination, newName));
	}

	void addBitFlagGenerationRequest(CharSequence target, List<ExecutableElement> flags, PackageElement destination, CharSequence newName) {
		var request = new FlagGenerationRequest(target, flags, destination, newName);
		addGenerationRequest(request);

		var bitFieldMemberTarget = new StringBuilder(target);
		bitFieldMemberTarget.replace(bitFieldMemberTarget.length() - "Flags".length(), bitFieldMemberTarget.length(), "FlagBits");
		mappings.putIfAbsent(bitFieldMemberTarget.toString(), new FlagBitConversionMapping(bitFieldMemberTarget, mappings.get(target.toString())));
	}

	void addEnumGenerationRequest(CharSequence target, List<ExecutableElement> values, PackageElement destination, CharSequence newName) {
		addGenerationRequest(new EnumGenerationRequest(target, values, destination, newName));
	}

	private void checkForAndAddExistingMapping(CharSequence target, PackageElement destination, CharSequence newName) {
		var qualifiedName = STR."\{destination}.\{newName}";
		var module = processingEnvironment.getElementUtils().getModuleOf(destination);
		TypeElement existingElement = module == null
				? processingEnvironment.getElementUtils().getTypeElement(qualifiedName)
				: processingEnvironment.getElementUtils().getTypeElement(module, qualifiedName);

		if (existingElement != null) {
			addMapping(target, existingElement);
		}
	}

	public Stream<StructureGenerationRequest> structureGenerationRequests() {
		return mappings.values().stream().mapMulti((nameMapping, consumer) -> {
			if (nameMapping instanceof StructureGenerationRequest request) {
				consumer.accept(request);
			}
		});
	}

	public Stream<FlagGenerationRequest> bitFlagGenerationRequests() {
		return mappings.values().stream().mapMulti((nameMapping, consumer) -> {
			if (nameMapping instanceof FlagGenerationRequest request) {
				consumer.accept(request);
			}
		});
	}

	public Stream<EnumGenerationRequest> enumGenerationRequests() {
		return mappings.values().stream().mapMulti((nameMapping, consumer) -> {
			if (nameMapping instanceof EnumGenerationRequest request) {
				consumer.accept(request);
			}
		});
	}

	public Stream<ExecutableElement> coreBitFlags() {
		if (vulkanHeader == null) {
			return Stream.empty();
		}

		return vulkanHeaderMethods.values().stream()
				.filter(m -> {
					var name = m.getSimpleName().toString();
					return name.startsWith("VK_") && name.endsWith("_BIT");
				});
	}

	public Stream<ExecutableElement> coreEnums() {
		if (vulkanHeader == null) {
			return Stream.empty();
		}

		return vulkanHeaderMethods.values().stream()
				.filter(m -> {
					var name = m.getSimpleName().toString();
					return name.startsWith("VK_") && !name.endsWith("_BIT");
				});
	}

	public Optional<NameMapping> mapping(CharSequence name) {
		return Optional.ofNullable(mappings.get(name.toString()));
	}

	public Optional<VariableElement> getLayout(CharSequence type) {
		if (vulkanHeader == null) {
			throw new IllegalStateException("No vulkan header specified");
		}

		return Optional.ofNullable(vulkanHeaderFields.get(type.toString()));
	}
}
