/*
 * Copyright James Brown 2024
 * Author: James Brown
 */

package dev.brownjames.lawu.vulkan.generator.structure;

import java.io.IOException;
import java.lang.foreign.*;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

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
					getContext().structureGenerationRequests().forEach(generationRequest -> {
						try {
							generateStructure(generationRequest);
						} catch (GenerationFailedException e) {
							e.raise(processingEnv);
						}
					});

					getContext().bitFlagGenerationRequests().forEach(generationRequest -> {
						try {
							generateBitFlags(generationRequest);
						} catch (GenerationFailedException e) {
							e.raise(processingEnv);
						}
					});

					getContext().enumGenerationRequests().forEach(generationRequest -> {
						try {
							generateEnum(generationRequest);
						} catch (GenerationFailedException e) {
							e.raise(processingEnv);
						}
					});
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
							throw new GenerationFailedException("Unable to find bit-flag type", executableElement).unchecked();
						}

						var start = snippet.lastIndexOf(' ', end) + 1;
						return STR."\{snippet.substring(start, end)}Flags";
					}).toList();

			if (names.size() != 1) {
				throw new GenerationFailedException("Unable to find bit-flag type", executableElement).unchecked();
			}

			return names.getFirst();
		}));

		for (var entry : bitFlagMaps.entrySet()) {
			context.addBitFlagGenerationRequest(entry.getKey(), entry.getValue(), destination, convertBitFlagsName(entry.getKey()));
		}
	}

	private void addEnumGenerationRequests(PackageElement destination) {
		var context = getContext();

		record EnumValueInformation(String owner, ExecutableElement valueGetter, int value) {
			EnumGenerationRequest.EnumValue createEnumValue(CharSequence valuePrefix) {
				var nativeName = valueGetter.getSimpleName();
				var enclosing = valueGetter.getEnclosingElement();

				var name = new StringBuilder(nativeName);
				removePrefix(name, valuePrefix.toString());
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

				if (!enclosing.getKind().isDeclaredType() || !(enclosing instanceof TypeElement)) {
					throw new GenerationFailedException(STR."\{valueGetter().getSimpleName()} is not enclosed in a type", valueGetter()).unchecked();
				}

				return new EnumGenerationRequest.EnumValue(valueGetter, name, value);
			}
		}

		var enumMaps = context.coreEnums().<EnumValueInformation>mapMulti(((executableElement, enumValueConsumer) -> {
			// Get snippets
			var comment = processingEnv.getElementUtils().getDocComment(executableElement);

			var commentEnumValues = CommentParser.extractSnippets(comment)
					.map(CharSequence::toString)
					.<EnumValueInformation>mapMulti((snippet, processedCommentConsumer) -> {
						var end = snippet.indexOf(STR.".\{executableElement.getSimpleName()} = ");
						if (end == -1) {
							return;
						}

						var start = snippet.lastIndexOf(' ', end) + 1;
						var name = snippet.substring(start, end);

						if (name.endsWith("Bits") || isExtensionName(name)) {
							return;
						}

						var numberEnd = snippet.indexOf(';', end);
						if (numberEnd == -1) {
							return;
						}

						var numberStart = snippet.lastIndexOf(' ', numberEnd) + 1;

						try {
							var number = Integer.parseUnsignedInt(snippet, numberStart, numberEnd, 10);
							processedCommentConsumer.accept(new EnumValueInformation(name, executableElement, number));
						} catch (NumberFormatException _) { }
					})
					.toList();

			if (commentEnumValues.isEmpty()) {
				return;
			}

			if (commentEnumValues.size() > 1) {
				throw new GenerationFailedException("Unable to find enum type", executableElement).unchecked();
			}

			enumValueConsumer.accept(commentEnumValues.getFirst());
		})).collect(Collectors.groupingBy(EnumValueInformation::owner));

		for (var entry : enumMaps.entrySet()) {
			var newName = convertEnumName(entry.getKey());

			var valuePrefix = new StringBuilder(newName);
			for (var i = 1; i < valuePrefix.length(); i++) {
				char c = valuePrefix.charAt(i);
				valuePrefix.setCharAt(i, Character.toUpperCase(c));

				if (Character.isUpperCase(c)) {
					valuePrefix.insert(i, '_');
					i++;
				}
			}

			valuePrefix.insert(0, "VK_");
			valuePrefix.append('_');

			context.addEnumGenerationRequest(entry.getKey(), entry.getValue().stream().map(v -> v.createEnumValue(valuePrefix)).toList(), destination, newName);
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
							ScopedValue.runWhere(CONTEXT, result, () -> {
								addBitFlagGenerationRequests(annotatedPackage);
								addEnumGenerationRequests(annotatedPackage);
							});
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
								result.addStructureGenerationRequest(type, annotatedPackage, convertStructureName(type.getSimpleName()));
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
		var result = new StringBuilder(convertStructureName(nativeName));

		removeSuffix(result, BIT_FLAG_SUFFIX);
		result.append("Flag");

		return result;
	}

	private CharSequence convertEnumName(CharSequence nativeName) {
		return convertStructureName(nativeName);
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

		void generate(GenerationRequest request) throws GenerationFailedException;
	}

	private static final StringTemplate.Processor<Generator, RuntimeException> GENERATOR = stringTemplate -> request -> {
		var importGroups = List.of(
				Pattern.compile("java\\..*"),
				Pattern.compile("javax\\..*"));

		var localImportStatements = new ArrayList<CharSequence>();

		var convertTemplate = new UnaryOperator<StringTemplate>() {
			@Override
			public StringTemplate apply(StringTemplate st) {
				var values = new ArrayList<>(st.values());
				for (var iterator = values.listIterator(); iterator.hasNext(); ) {
					switch (iterator.next()) {
						case Generator.Generated _ -> iterator.set(
								apply(RAW."""
							@\{Generated.class}(
									value = "\{StructureGenerator.class.getName()}",
									date = "\{DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())}",
									comments = "Generated from \{request.target().toString()}")""").interpolate()
						);
						case Class<?> clazz when clazz.isMemberClass() ->
								iterator.set(RAW."\{clazz.getDeclaringClass()}.\{clazz.getSimpleName()}");
						case Class<?> clazz -> {
							iterator.set(clazz.getSimpleName());

							while (clazz.isArray()) {
								clazz = clazz.getComponentType();
							}

							localImportStatements.add(clazz.getCanonicalName());
						}
						case StringTemplate template -> iterator.set(apply(template).interpolate());
						case TypeElement type -> {
							iterator.set(type.getSimpleName());
							localImportStatements.add(type.getQualifiedName());
						}
						case NameMapping mapping -> {
							iterator.set(mapping.name());
							localImportStatements.add(mapping.qualifiedName());
						}
						default -> { }
					}
				}

				return StringTemplate.of(st.fragments(), values);
			}
		};

		var convertedTemplate = convertTemplate.apply(stringTemplate);

		Map<Integer, List<CharSequence>> imports = localImportStatements.stream()
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

		var result = StringTemplate.combine(header, convertedTemplate).interpolate();

		try (var destination = CONTEXT.get().processingEnvironment().getFiler().createSourceFile(request.qualifiedName(), request.owner()).openWriter()) {
			destination.write(result);
		} catch (IOException e) {
			throw new GenerationFailedException(STR."Unable to write out file: \{e}", request.owner(), e);
		}
	};

	private Collector<StringTemplate, ?, StringTemplate> stringTemplateJoiner(StringTemplate separator) {
		return Collectors.reducing(RAW."", (a, b) -> {
			if (a.fragments().size() == 1 && a.fragments().getFirst().isEmpty()) {
				return b;
			}

			if (b.fragments().size() == 1 && b.fragments().getFirst().isEmpty()) {
				return a;
			}

			return StringTemplate.combine(a, separator, b);
		});
	}

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
		boolean hasNextField = members.stream().anyMatch(m -> m instanceof NextMember);

		var structureWithNext = getContext().lookup().structureWithNext()
				.orElseThrow(() -> new GenerationFailedException("Unable to find 'Structure.WithNext'", request.target()));
		var structure = getContext().lookup().structure()
				.orElseThrow(() -> new GenerationFailedException("Unable to find 'Structure'", request.target()));
		var valueStructure = getContext().lookup().valueStructure()
				.orElseThrow(() -> new GenerationFailedException("Unable to find 'Structure'", request.target()));

		var superClass = hasNextField ? RAW."\{structureWithNext}<\{request}.Next>" : RAW."\{structure}";

		var valueRecord =
				RAW."""
					record Value(\{members.stream().filter(m -> !(m instanceof NextMember)).map(StructureMember::declaration).collect(stringTemplateJoiner(RAW.", "))}) implements \{request}, Structure.\{valueStructure} {
						@Override
						public void asRaw(\{MemorySegment.class} destination, \{SegmentAllocator.class} allocator) {
							\{members.stream().map(m -> m.asRaw(request, "destination", "allocator")).collect(stringTemplateJoiner(RAW."\n\t\t\t"))}
						}

						@Override
						public \{MemorySegment.class} asRaw(\{SegmentAllocator.class} allocator) {
							var raw = \{request.target()}.allocate(allocator);
							asRaw(raw, allocator);
							return raw;
						}

						@Override
						public \{request}.Value asValue() {
							return this;
						}

						@Override
						public \{request}.Native asNative(\{SegmentAllocator.class} allocator) {
							return of(asRaw(allocator));
						}

						@Override
						public \{request}.Native asNative() {
							return of(asRaw());
						}
					}
				""";

		var nativeSuperClass = hasNextField
				? RAW."WithNext.\{getContext().lookup().nativeWithNext()
				.orElseThrow(() -> new GenerationFailedException("Unable to find 'Structure.WithNext.Native'", request.target()))}<Next>"
				: RAW."Structure.\{getContext().lookup().nativeStructure()
				.orElseThrow(() -> new GenerationFailedException("Unable to find 'Structure.Native'", request.target()))}";
		var nativeRecord =
				RAW."""
					record Native(\{MemorySegment.class} raw) implements \{request}, \{nativeSuperClass} {
						@Override
						public \{request}.Value asValue() {
							return of(\{members.stream().filter(m -> !(m instanceof NextMember)).map(m -> RAW."\{m.name()}()").collect(stringTemplateJoiner(RAW.", "))});
						}

						@Override
						public \{request}.Native asNative() {
							return this;
						}

						@Override
						public \{request}.Native asNative(\{SegmentAllocator.class} allocator) {
							return this;
						}

				\{members.stream().filter(m -> !(m instanceof NextMember)).map(m ->
						RAW."""
								@Override
								public \{m.type()} \{m.name()}() {
									return \{m.of(request, "raw")};
								}
						""").collect(stringTemplateJoiner(RAW."\n"))}
					}
				""";

		var ofDeclarations = List.of(
				RAW."""
					/**
					 * Creates an instance of this structure from a given memory segment.
					 * All data is copied, and the memory segment does not need to be kept alive.
					 *
					 * @param raw the memory segment to copy from
					 *
					 * @return an instance of this structure
					 */
					static Native of(\{MemorySegment.class} raw) {
						return new Native(raw);
					}
				""",
				RAW."""
					static Value of(\{members.stream().filter(m -> !(m instanceof NextMember)).map(StructureMember::declaration).collect(stringTemplateJoiner(RAW.", "))}) {
						return new Value(\{members.stream().filter(m -> !(m instanceof NextMember)).map(StructureMember::name).collect(Collectors.joining(", "))});
					}
				""");

		var allocateDeclarations = List.of(
				RAW."""
					/**
					 * Allocates an instance of this structure
					 *
					 * @param allocator the allocator to use
					 */
					public static Native allocate(\{SegmentAllocator.class} allocator) {
						return of(\{request.target()}.allocate(allocator));
					}
				""",
				RAW."""
					/**
					 * Allocates an instance of this structure
					 */
					public static Native allocate() {
						return allocate(\{Arena.class}.ofAuto());
					}
				""");

		var declarations = new ArrayList<>();
		declarations.add(valueRecord);
		declarations.add(nativeRecord);
		declarations.addAll(ofDeclarations);
		declarations.addAll(allocateDeclarations);
		members.forEach(m -> {
			declarations.addAll(m.extraDeclarations(request));

			if (m instanceof NextMember) {
				return;
			}

			declarations.add(RAW."\t\{m.type()} \{m.name()}();");
		});

		var fragments = Collections.nCopies(declarations.size() + 1, "\n");
		var declarationsTemplate = StringTemplate.of(fragments, declarations);

		GENERATOR."""
		/**
		 * \{comment.trim().replace("\n", "\n * ")}
		 */
		\{new Generator.Generated()}
		public interface \{request.name()} extends \{superClass} {\{declarationsTemplate}}
		""".generate(request);
	}

	private void generateBitFlags(FlagGenerationRequest generationRequest) throws GenerationFailedException {
		var bitFlag = elements.bitFlag()
				.orElseThrow(() -> new GenerationFailedException("No BitFlag type found", generationRequest.destination()));

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

		var flags = generationRequest.flags().stream().map(flag -> {
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

			if (!enclosing.getKind().isDeclaredType() || !(enclosing instanceof TypeElement)) {
				throw new GenerationFailedException(STR."\{flag.getSimpleName()} is not enclosed in a type", flag).unchecked();
			}

			return RAW."\{name}(\{enclosing}.\{nativeName}())";
		}).collect(stringTemplateJoiner(RAW.",\n\t"));

		GENERATOR."""
		\{new Generator.Generated()}
		public enum \{generationRequest.name()} implements \{bitFlag} {
			\{flags};

			private static final \{Map.class}<\{Integer.class}, \{generationRequest.name()}> LOOKUP =
					\{Map.class}.ofEntries(\{Arrays.class}.stream(values()).map(v -> \{Map.class}.entry(v.bit, v)).toArray(\{Map.Entry[].class}::new));

			private final int bit;

			\{generationRequest.name()}(int bit) {
				this.bit = bit;
			}

			@Override
			public int bit() {
				return bit;
			}

			public static \{generationRequest.name()} of(int bit) {
				return \{Objects.class}.requireNonNull(LOOKUP.get(bit));
			}
		}
		""".generate(generationRequest);
	}

	private void generateEnum(EnumGenerationRequest generationRequest) throws GenerationFailedException {
		// Extract the flag prefix
		var values = generationRequest.values().stream()
				.map(v -> {
					var comment = processingEnv.getElementUtils().getDocComment(v.valueGetter());
					return STR."""
						/**
						 * \{comment.trim().replace("\n", "\n * ")}
						 */
						\{v.name()}(\{v.value()})
					""";
				})
				.collect(Collectors.joining(",\n"));

		var valueMapping = generationRequest.values().stream()
				.collect(Collectors.toMap(EnumGenerationRequest.EnumValue::value,
						Function.identity(),
						(a, b) -> a.name().length() <= b.name().length() ? a : b));

		GENERATOR."""
		\{new Generator.Generated()}
		public enum \{generationRequest.name()} {
		\{values};

			private final int value;

			\{generationRequest.name()}(int value) {
				this.value = value;
			}

			public int value() {
				return value;
			}

			public static \{generationRequest.name()} of(int value) {
				return switch (value) {\{valueMapping.values().stream().map(enumValue -> STR."\n\t\t\t\tcase \{enumValue.value()} -> \{enumValue.name()};").collect(Collectors.joining())}
					default -> throw new \{IllegalArgumentException.class}(\{StringTemplate.class}.STR."\\{value} is not a valid value for \{generationRequest.name()}");
				};
			}
		}
		""".generate(generationRequest);
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
