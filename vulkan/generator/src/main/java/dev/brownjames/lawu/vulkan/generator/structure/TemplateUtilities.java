package dev.brownjames.lawu.vulkan.generator.structure;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.lang.model.element.TypeElement;

import dev.brownjames.lawu.vulkan.generator.GenerationFailedException;

import static java.lang.StringTemplate.RAW;

final class TemplateUtilities {
	@FunctionalInterface
	interface Generator {
		void generate(GenerationRequest request) throws GenerationFailedException;
	}

	record Generated() { }

	static final StringTemplate.Processor<Generator, RuntimeException> GENERATOR = stringTemplate -> request -> {
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
						case Generated _ -> iterator.set(
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

		try (var destination = StructureGenerator.getContext().processingEnvironment().getFiler().createSourceFile(request.qualifiedName(), request.owner()).openWriter()) {
			destination.write(result);
		} catch (IOException e) {
			throw new GenerationFailedException(STR."Unable to write out file: \{e}", request.owner(), e);
		}
	};

	private TemplateUtilities() { }

	static Collector<StringTemplate, ?, StringTemplate> stringTemplateJoiner(StringTemplate separator) {
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
}
