/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan.generator.structure;

import java.io.IOException;
import java.lang.foreign.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import dev.brownjames.lawu.vulkan.annotation.GenerateCoreStructuresFrom;
import dev.brownjames.lawu.vulkan.annotation.MapStructure;

import dev.brownjames.lawu.vulkan.generator.ElementLookup;
import dev.brownjames.lawu.vulkan.generator.GenerationFailedException;

/**
 * Processes vulkan sources to generate structure wrappers
 */
@SupportedAnnotationTypes("dev.brownjames.lawu.vulkan.annotation/dev.brownjames.lawu.vulkan.annotation.*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class StructureGenerator extends AbstractProcessor {
	private static final String VULKAN_PREFIX = "Vk";

	private ElementLookup elements;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		processingEnv.getMessager().printNote("Generating Vulkan structures");

		elements = ElementLookup.lookupElements(processingEnv);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		generateCoreStructuresFromPackages(annotations, roundEnv);

		return true;
	}

	private void generateCoreStructuresFromPackages(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		var context = getGenerationContext(annotations, roundEnv);
		for (var generationRequest : context.generationRequests()) {
			try {
				generateStructure(generationRequest, context);
			} catch (GenerationFailedException e) {
				e.raise(processingEnv);
			}
		}
	}

	private GenerationContext getGenerationContext(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		var result = new GenerationContext(processingEnv, elements);

		elements.mapStructureAnnotation().filter(annotations::contains).ifPresent(mapAnnotation -> {
			for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(mapAnnotation)) {
				if (!(annotatedElement instanceof TypeElement annotatedType) || !annotatedElement.getKind().isDeclaredType()) {
					processingEnv.getMessager().printError("Map annotations must only be applied to declared types", annotatedElement);
					continue;
				}

				for (MapStructure annotation : annotatedType.getAnnotationsByType(MapStructure.class)) {
					try {
						var name = annotation.value();
						result.addMapping(name, annotatedType);
					} catch (IllegalStateException | IllegalArgumentException e) {
						processingEnv.getMessager().printError(e.getMessage(), annotatedElement);
					}
				}
			}
		});

		elements.vulkanHeaderAnnotation().filter(annotations::contains).ifPresent(vulkanHeaderAnnotation -> {
			for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(vulkanHeaderAnnotation)) {
				if (!(annotatedElement instanceof PackageElement annotatedPackage)) {
					processingEnv.getMessager().printError("VulkanHeader annotations must only be applied to packages", annotatedElement);
					continue;
				}

				for (var annotation : annotatedPackage.getAnnotationMirrors()) {
					if (!processingEnv.getTypeUtils().isSameType(annotation.getAnnotationType(), vulkanHeaderAnnotation.asType())) {
						continue;
					}

					for (var value : processingEnv.getElementUtils().getElementValuesWithDefaults(annotation).entrySet()) {
						if (!"value".contentEquals(value.getKey().getSimpleName())) {
							continue;
						}

						try {
							if (!(value.getValue().getValue() instanceof TypeMirror type)) {
								throw new IllegalStateException("VulkanHeader annotations must contain classes");
							}

							result.addHeader(type);
						} catch (IllegalStateException | IllegalArgumentException e) {
							processingEnv.getMessager().printError(e.getMessage(), annotatedElement);
						}
					}
				}
			}
		});

		elements.generateCoreStructuresFromAnnotation().filter(annotations::contains).ifPresent(generateCoreStructuresFromAnnotation -> {
			for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(generateCoreStructuresFromAnnotation)) {
				if (!(annotatedElement instanceof PackageElement annotatedPackage)) {
					processingEnv.getMessager().printError("Structure annotations must only be applied to packages", annotatedElement);
					continue;
				}

				for (GenerateCoreStructuresFrom annotation : annotatedPackage.getAnnotationsByType(GenerateCoreStructuresFrom.class)) {
					var targetPackage = processingEnv.getElementUtils().getPackageElement(annotation.value());

					for (var type : ElementFilter.typesIn(targetPackage.getEnclosedElements())) {
						if (isVulkanCoreName(type.getSimpleName())) {
							try {
								result.addGenerationRequest(type, annotatedPackage, convertName(type.getSimpleName()));
							} catch (IllegalStateException ise) {
								processingEnv.getMessager().printError(ise.getMessage(), annotatedElement);
							}
						}
					}
				}
			}
		});

		return result;
	}

	private boolean isVulkanCoreName(CharSequence name) {
		return isVulkanName(name) && !isExtensionName(name);
	}

	private boolean isExtensionName(CharSequence name) {
		return Character.isUpperCase(name.charAt(name.length() - 1)) && Character.isUpperCase(name.charAt(name.length() - 2));
	}

	private boolean isVulkanName(CharSequence name) {
		return name.toString().startsWith(VULKAN_PREFIX);
	}

	private CharSequence convertName(CharSequence nativeName) {
		var result = new StringBuilder(nativeName);

		// Remove the prefix
		if (result.subSequence(0, VULKAN_PREFIX.length()).equals(VULKAN_PREFIX)) {
			result.delete(0, VULKAN_PREFIX.length());
		}

		// Replace 3D and 2D
		record Replacement(String value, String replacement) {
			void execute(StringBuilder string) {
				for (int index = 0; (index = string.indexOf(value, index)) != -1;) {
					string.replace(index, index + value.length(), Replacement.this.replacement);
				}
			}
		}

		List.of(
				new Replacement("3D", "3d"),
				new Replacement("2D", "2d"),
				new Replacement("RGBA10X6", "Rgba10x6")
		).forEach(r -> r.execute(result));

		// Remove extension suffixes
		while (Character.isUpperCase(result.charAt(result.length() - 1))) {
			result.deleteCharAt(result.length() - 1);
		}

		return result;
	}

	private void generateStructure(GenerationRequest request, GenerationContext context) throws GenerationFailedException {
		var comment = processingEnv.getElementUtils().getDocComment(request.target());

		var parsedDeclaration = parseComment(request, comment);

		// Exit early if this is a type-definition
		if (!(parsedDeclaration instanceof CommentParser.ParsedStructOrUnion structDeclaration)) {
			assert parsedDeclaration instanceof CommentParser.ParsedTypeDefinition;
			return;
		}

		// Find all getters and slice-access methods of the target
		var bindingInformationMap = getBindingInformationMap(request);

		// Combine these with the native information
		var members = getStructureMembers(context, request, structDeclaration, bindingInformationMap);

		var importGroups = List.of(
				Pattern.compile("java\\..*"),
				Pattern.compile("javax\\..*"));

		var memberImports = members.stream()
				.flatMap(structureMember -> structureMember.imports(request).stream());

		var requiredImports = Stream.of(
				Arena.class.getCanonicalName(),
				MemorySegment.class.getCanonicalName(),
				SegmentAllocator.class.getCanonicalName(),
				Generated.class.getCanonicalName()
		);

		Map<Integer, List<CharSequence>> imports = Stream.concat(memberImports, requiredImports)
				.map(CharSequence::toString)
				.sorted()
				.distinct()
				.collect(Collectors.groupingBy(charSequence -> {
					for (int i = 0; i < importGroups.size(); i++) {
						if (importGroups.get(i).matcher(charSequence).matches()) {
							return i;
						}
					}

					return importGroups.size();
				}));

		StringBuilder importStatements = new StringBuilder();
		for (int i = 0; i <= importGroups.size(); i++) {
			var importGroup = imports.getOrDefault(i, Collections.emptyList());
			if (importGroup.isEmpty()) {
				continue;
			}

			importStatements.append('\n');
			for (var importStatement : importGroup) {
				importStatements.append("import ");
				importStatements.append(importStatement);
				importStatements.append(";\n");
			}
		}

		var result =
				STR."""
				package \{request.destination()};
				\{importStatements}

				/**
				 * \{comment.trim().replace("\n", "\n * ")}
				 */
				@Generated(
						value = "\{StructureGenerator.class.getName()}",
						date = "\{DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())}",
						comments = "Generated from \{request.target()}")
				public record \{request.name()}(\{members.stream().map(StructureMember::declaration).collect(Collectors.joining(", "))}) {
					public static \{request.name()} of(MemorySegment raw) {
						return new \{request.name()}(\{members.stream().map(m -> m.of(request, "raw")).collect(Collectors.joining(",\n\t\t\t\t"))});
					}

					public void asNative(MemorySegment destination) {
						\{members.stream().map(m -> m.asNative(request, "destination")).collect(Collectors.joining("\n\t\t"))}
					}

					public MemorySegment asNative(SegmentAllocator arena) {
						var raw = \{request.target().getSimpleName()}.allocate(arena);
						asNative(raw);
						return raw;
					}

					public MemorySegment asNative() {
						return asNative(Arena.ofAuto());
					}
				}
				""";

		try (var destination = processingEnv.getFiler().createSourceFile(request.qualifiedName(), request.target()).openWriter()) {
			destination.write(result);
		} catch (IOException e) {
			throw new GenerationFailedException(STR."Unable to write out file: \{e}", request.target(), e);
		}
	}

	private static ArrayList<StructureMember> getStructureMembers(GenerationContext context, GenerationRequest request, CommentParser.ParsedStructOrUnion structDeclaration, Map<String, StructureMember.BindingInformation> bindingInformationMap) throws GenerationFailedException {
		var members = new ArrayList<StructureMember>();
		for (var member : structDeclaration.members()) {
			var bindingInformation = bindingInformationMap.remove(member.name().toString());
			if (bindingInformation == null) {
				throw new GenerationFailedException(STR."No getter or slice methods found for \{member.name()}", request.target());
			}

			members.add(StructureMember.createStructureMember(context, bindingInformation, member));
		}

		for (var entry : bindingInformationMap.entrySet()) {
			throw new GenerationFailedException(STR."\{entry.getKey()} does not have a C++ equivalent", entry.getValue().bindingMethod());
		}
		return members;
	}

	private Map<String, StructureMember.BindingInformation> getBindingInformationMap(GenerationRequest request) throws GenerationFailedException {
		var memorySegmentType = elements.memorySegment()
				.map(TypeElement::asType)
				.orElseThrow(() -> new GenerationFailedException(STR."Unable to find \{MemorySegment.class.getName()}", request.target()));

		var result = new HashMap<String, StructureMember.BindingInformation>();
		for (var method : ElementFilter.methodsIn(request.target().getEnclosedElements())) {
			// Check the parameters match a getter or a slice method
			var parameters = method.getParameters();
			if (parameters.size() != 1 || !processingEnv.getTypeUtils().isSameType(parameters.getFirst().asType(), memorySegmentType)) {
				continue;
			}

			// Parse the name
			var name = method.getSimpleName().toString();
			var separatorLocation = name.lastIndexOf('$');
			if (separatorLocation == -1) {
				continue;
			}

			var memberName = name.substring(0, separatorLocation);
			var getterType = name.substring(separatorLocation + 1);

			// Create the binding
			var bindingInformation = StructureMember.createBindingInformation(method, getterType);
			if (bindingInformation.isEmpty()) {
				continue;
			}

			// If it is unique, add it to the result map
			var existingBinding = result.put(memberName, bindingInformation.get());
			if (existingBinding != null) {
				throw new GenerationFailedException(STR."Duplicate methods for \{memberName} found", method);
			}
		}

		return result;
	}

	private CommentParser.ParsedDeclaration parseComment(GenerationRequest request, CharSequence comment) throws GenerationFailedException {
		// Parse the snippet
		Stream<CommentParser.ParsedDeclaration> commentDeclarations;
		try {
			commentDeclarations = comment == null ? Stream.empty() : CommentParser.parse(comment);
		} catch (CommentParser.ParseException e) {
			throw new GenerationFailedException("Failed to parse structure snippet", request.target(), e);
		}

		// Find the structure
		List<CommentParser.ParsedDeclaration> declarationList = commentDeclarations
				.filter(parsedDeclaration -> parsedDeclaration.name().toString().contentEquals(request.target().getSimpleName()))
				.toList();

		if (declarationList.size() != 1) {
			throw new GenerationFailedException("No matching structure found", request.target());
		}

		// Generate struct if it is not a type-definition
		return declarationList.getFirst();
	}
}
