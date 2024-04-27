/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan.generator.structure;

import java.io.IOException;
import java.lang.foreign.*;
import java.math.BigDecimal;
import java.math.BigInteger;
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

import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import dev.brownjames.lawu.vulkan.annotation.GenerateCoreStructuresFrom;
import dev.brownjames.lawu.vulkan.annotation.MapStructure;

import dev.brownjames.lawu.vulkan.generator.ElementLookup;
import dev.brownjames.lawu.vulkan.generator.GenerationFailedException;

import static java.lang.StringTemplate.RAW;

/**
 * Processes vulkan sources to generate structure wrappers
 */
@SupportedAnnotationTypes("dev.brownjames.lawu.vulkan.annotation/dev.brownjames.lawu.vulkan.annotation.*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class StructureGenerator extends AbstractProcessor {
	private static final String VULKAN_FUNCTION_POINTER_PREFIX = "PFN_vk";
	private static final String VULKAN_PREFIX = "Vk";
	private static final ScopedValue<GenerationContext> CONTEXT = ScopedValue.newInstance();
	private static final String BIT_FLAG_SUFFIX = "Flags";
	private static final NumberFormat SPELL_OUT_FORMATTER = new RuleBasedNumberFormat(RuleBasedNumberFormat.SPELLOUT);

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

	/**
	 * Gets the current context object for generation
	 * @return the context object
	 */
	static GenerationContext getContext() {
		return CONTEXT.get();
	}

	private void generateCoreStructuresFromPackages(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		ScopedValue.where(CONTEXT, makeGenerationContext(annotations, roundEnv))
				.run(() -> {
					for (var generationRequest : getContext().structureGenerationRequests()) {
						try {
							generateStructure(generationRequest);
						} catch (GenerationFailedException e) {
							e.raise(processingEnv);
						}
					}

					for (var generationRequest : getContext().bitFlagGenerationRequests()) {
						try {
							generateBitFlags(generationRequest);
						} catch (GenerationFailedException e) {
							e.raise(processingEnv);
						}
					}
				});
	}

	private void addBitFlagGenerationRequests(PackageElement destination) {
		var context = getContext();

		var bitFlagMaps = context.coreBitFlags().collect(Collectors.groupingBy(executableElement -> {
			// Get snippets
			var comment = processingEnv.getElementUtils().getDocComment(executableElement);
			var names = CommentParser.extractSnippets(comment)
					.map(CharSequence::toString)
					.map(snippet -> {
						var end = snippet.indexOf(STR."FlagBits.\{executableElement.getSimpleName()}");
						if (end == -1) {
							throw new GenerationFailedException("Unable to find enum type", executableElement).unchecked();
						}

						var start = snippet.lastIndexOf(' ', end) + 1;
						return STR."\{snippet.substring(start, end)}Flags";
					}).toList();

			if (names.size() != 1) {
				throw new GenerationFailedException("Unable to find enum type", executableElement).unchecked();
			}

			return names.getFirst();
		}));

		for (var entry : bitFlagMaps.entrySet()) {
			context.addBitFlagGenerationRequest(entry.getKey(), entry.getValue(), destination, convertBitFlagsName(entry.getKey()));
		}
	}

	private GenerationContext makeGenerationContext(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
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
							ScopedValue.runWhere(CONTEXT, result, () -> addBitFlagGenerationRequests(annotatedPackage));
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
						try {
							if (isVulkanCoreName(type.getSimpleName())) {
								result.addGenerationRequest(type, annotatedPackage, convertStructureName(type.getSimpleName()));
							} else if (isCoreVulkanFunctionPointerName(type.getSimpleName())) {
								result.addFunctionPointer(type);
							}
						} catch (IllegalStateException ise) {
							processingEnv.getMessager().printError(ise.getMessage(), annotatedElement);
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

	private boolean isCoreVulkanFunctionPointerName(CharSequence name) {
		return isFunctionPointerName(name) && !isExtensionName(name);
	}

	private boolean isFunctionPointerName(CharSequence name) {
		return name.toString().startsWith(VULKAN_FUNCTION_POINTER_PREFIX);
	}

	private boolean isExtensionName(CharSequence name) {
		return Character.isUpperCase(name.charAt(name.length() - 1)) && Character.isUpperCase(name.charAt(name.length() - 2));
	}

	private boolean isVulkanName(CharSequence name) {
		return name.toString().startsWith(VULKAN_PREFIX);
	}

	private CharSequence convertStructureName(CharSequence nativeName) {
		var result = new StringBuilder(nativeName);

		// Remove the prefix
		removePrefix(result, VULKAN_PREFIX);

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
		removeExtensionSuffixes(result);

		return result;
	}

	private CharSequence convertBitFlagsName(CharSequence nativeName) {
		var result = new StringBuilder(nativeName);

		removeExtensionSuffixes(result);

		removePrefix(result, VULKAN_PREFIX);
		removeSuffix(result, BIT_FLAG_SUFFIX);
		result.append("Flag");

		return result;
	}

	private static void removeSuffix(StringBuilder builder, String suffix) {
		removeIfMatched(builder, suffix, builder.length() - suffix.length(), builder.length());
	}

	private static void removePrefix(StringBuilder builder, String prefix) {
		removeIfMatched(builder, prefix, 0, prefix.length());
	}

	private static void removeIfMatched(StringBuilder builder, String match, int start, int end) {
		if (match.contentEquals(builder.subSequence(start, end))) {
			builder.delete(start, end);
		}
	}

	private static void removeExtensionSuffixes(StringBuilder result) {
		// Remove extension suffixes
		while (Character.isUpperCase(result.charAt(result.length() - 1))) {
			result.deleteCharAt(result.length() - 1);
		}
	}

	@FunctionalInterface
	private interface Generator {
		record Generated() { }

		void generate(GenerationRequest request, Stream<? extends CharSequence> importStatements) throws GenerationFailedException;
	}

	private static final StringTemplate.Processor<Generator, RuntimeException> GENERATOR = stringTemplate -> (request, importStatements) -> {
		var importGroups = List.of(
				Pattern.compile("java\\..*"),
				Pattern.compile("javax\\..*"));

		// Add generator if required
		if (stringTemplate.values().contains(new Generator.Generated())) {
			importStatements = Stream.concat(importStatements, Stream.of(Generated.class.getCanonicalName()));
		}

		Map<Integer, List<CharSequence>> imports = importStatements
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

		StringBuilder combinedImports = new StringBuilder();
		for (int i = 0; i <= importGroups.size(); i++) {
			var importGroup = imports.getOrDefault(i, Collections.emptyList());
			if (importGroup.isEmpty()) {
				continue;
			}

			combinedImports.append('\n');
			for (var importStatement : importGroup) {
				combinedImports.append("import ");
				combinedImports.append(importStatement);
				combinedImports.append(";\n");
			}
		}

		var header =
				RAW."""
				package \{request.destination()};
				\{combinedImports}
				""";

		var values = new ArrayList<>(stringTemplate.values());
		for (var iterator = values.listIterator(); iterator.hasNext(); ) {
			switch (iterator.next()) {
				case Generator.Generated _ -> iterator.set(
						STR."""
						@Generated(
								value = "\{StructureGenerator.class.getName()}",
								date = "\{DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())}",
								comments = "Generated from \{request.target()}")"""
				);
				default -> { }
			}
		}

		var result = StringTemplate.combine(header, StringTemplate.of(stringTemplate.fragments(), values)).interpolate();

		try (var destination = CONTEXT.get().processingEnvironment().getFiler().createSourceFile(request.qualifiedName(), request.owner()).openWriter()) {
			destination.write(result);
		} catch (IOException e) {
			throw new GenerationFailedException(STR."Unable to write out file: \{e}", request.owner(), e);
		}
	};

	private void generateStructure(StructureGenerationRequest request) throws GenerationFailedException {
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
		var members = getStructureMembers(request, structDeclaration, bindingInformationMap);

		var memberImports = members.stream()
				.flatMap(structureMember -> structureMember.imports(request).stream());

		var requiredImports = Stream.of(
				Arena.class.getCanonicalName(),
				MemorySegment.class.getCanonicalName(),
				SegmentAllocator.class.getCanonicalName(),
				Generated.class.getCanonicalName()
		);

		Stream<CharSequence> imports = Stream.concat(memberImports, requiredImports);

		var ofDeclaration =
				STR."""
					/**
					 * Creates an instance of this structure from a given memory segment.
					 * All data is copied, and the memory segment does not need to be kept alive.
					 *
					 * @param raw the memory segment to copy from
					 *
					 * @return an instance of this structure
					 */
					public static \{request.name()} of(MemorySegment raw) {
						return new \{request.name()}(\{members.stream().map(m -> m.of(request, "raw")).collect(Collectors.joining(",\n\t\t\t\t"))});
					}
				""";

		var asNativeDeclarations = List.of(
				STR."""
					/**
					 * Copies data from this structure into the given destination.
					 *
					 * @param destination the memory segment to copy data into
					 */
					public void asNative(MemorySegment destination) {
						\{members.stream().map(m -> m.asNative(request, "destination")).collect(Collectors.joining("\n\t\t"))}
					}
				""",
				STR."""
					/**
					 * Creates a memory segment from this structure
					 *
					 * @param allocator the segment allocator to allocate from
					 *
					 * @return an allocated memory-segment
					 */
					public MemorySegment asNative(SegmentAllocator allocator) {
						var raw = \{request.target().getSimpleName()}.allocate(allocator);
						asNative(raw);
						return raw;
					}
				""",
				"""
					/**
					 * Creates a memory segment from this structure allocated from an auto arena
					 *
					 * @return an allocated memory segment
					 */
					public MemorySegment asNative() {
						return asNative(Arena.ofAuto());
					}
				""");

		var declarations = new ArrayList<CharSequence>();
		declarations.add(ofDeclaration);
		declarations.addAll(asNativeDeclarations);

		members.forEach(m -> declarations.addAll(m.extraDeclarations()));

		GENERATOR."""
		/**
		 * \{comment.trim().replace("\n", "\n * ")}
		 */
		\{new Generator.Generated()}
		public record \{request.name()}(\{members.stream().map(StructureMember::declaration).collect(Collectors.joining(", "))}) {\{declarations.stream().collect(Collectors.joining("\n", "\n", "\n"))}}
		""".generate(request, imports);
	}

	private void generateBitFlags(BitFlagGenerationRequest generationRequest) throws GenerationFailedException {
		var imports = new ArrayList<CharSequence>();

		imports.add(elements.bitFlag()
				.orElseThrow(() -> new GenerationFailedException("No BitFlag type found", generationRequest.destination()))
				.getQualifiedName());

		// Extract the flag prefix
		var flagPrefix = new StringBuilder(generationRequest.name());
		for (var i = 1; i < flagPrefix.length(); i++) {
			char c = flagPrefix.charAt(i);
			flagPrefix.setCharAt(i, Character.toUpperCase(c));

			if (Character.isUpperCase(c)) {
				flagPrefix.insert(i, '_');
				i++;
			}
		}
		removeSuffix(flagPrefix, "FLAG");
		flagPrefix.insert(0, "VK_");

		StringBuilder flags = new StringBuilder();
		for (var flag : generationRequest.flags()) {
			var nativeName = flag.getSimpleName();
			var enclosing = flag.getEnclosingElement();

			var name = new StringBuilder(nativeName);
			removeSuffix(name, "_BIT");
			removePrefix(name, flagPrefix.toString());
			if (Character.isDigit(name.charAt(0))) {
				var startingNumberLength = (int) name.chars()
						.takeWhile(Character::isDigit)
						.count();
				var startingNumber = name.substring(0, startingNumberLength);

				var replacement = SPELL_OUT_FORMATTER.format(new BigInteger(startingNumber)).replaceAll("[ -]", "_").toUpperCase();

				if (name.length() > startingNumberLength) {
					char firstCharAfterReplacement = name.charAt(startingNumberLength);
					if (firstCharAfterReplacement == 'D') {
						name.replace(startingNumberLength, startingNumberLength + 1, "_DIMENSIONAL");
					} else if (firstCharAfterReplacement != '_') {
						name.insert(startingNumberLength, '_');
					}
				}

				name.replace(0, startingNumberLength, replacement);
			}

			if (!enclosing.getKind().isDeclaredType() || !(enclosing instanceof TypeElement enclosingType)) {
				throw new GenerationFailedException(STR."\{flag.getSimpleName()} is not enclosed in a type", flag);
			}

			imports.add(enclosingType.getQualifiedName());
			flags.append(STR."\t\{name}(\{enclosing.getSimpleName()}.\{nativeName}()),\n");
		}

		flags.replace(flags.length() - 2, flags.length(), ";");

		GENERATOR."""
		\{new Generator.Generated()}
		public enum \{generationRequest.name()} implements BitFlag {
		\{flags}

			private final int bit;

			\{generationRequest.name()}(int bit) {
				this.bit = bit;
			}

			@Override
			public int bit() {
				return bit;
			}
		}
		""".generate(generationRequest, imports.stream());
	}

	private static ArrayList<StructureMember> getStructureMembers(StructureGenerationRequest request, CommentParser.ParsedStructOrUnion structDeclaration, Map<String, StructureMember.BindingInformation> bindingInformationMap) throws GenerationFailedException {
		var members = new ArrayList<StructureMember>();
		for (var member : structDeclaration.members()) {
			var bindingInformation = bindingInformationMap.remove(member.name().toString());
			if (bindingInformation == null) {
				throw new GenerationFailedException(STR."No getter or slice methods found for \{member.name()}", request.target());
			}

			members.add(StructureMember.createStructureMember(bindingInformation, member));
		}

		for (var entry : bindingInformationMap.entrySet()) {
			throw new GenerationFailedException(STR."\{entry.getKey()} does not have a C++ equivalent", entry.getValue().bindingMethod());
		}
		return members;
	}

	private Map<String, StructureMember.BindingInformation> getBindingInformationMap(StructureGenerationRequest request) throws GenerationFailedException {
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

	private CommentParser.ParsedDeclaration parseComment(StructureGenerationRequest request, CharSequence comment) throws GenerationFailedException {
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
