package dev.brownjames.lawu.vulkan.generator.structure;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import dev.brownjames.lawu.vulkan.generator.GenerationFailedException;

import static java.lang.StringTemplate.RAW;

/**
 * A member of a generated structure
 */
interface StructureMember {
	/**
	 * Creates information regarding the Java method linking the native memory and a raw Java-type
	 * @param bindingMethod the method
	 * @param getterStyle the style of the method
	 * @return a binding information object if this method is a valid binding method
	 */
	static Optional<BindingInformation> createBindingInformation(ExecutableElement bindingMethod, String getterStyle) {
		Optional<BindingInformation.Type> getterStyleEnum = switch (getterStyle) {
			case "get" -> Optional.of(BindingInformation.Type.VALUE);
			case "slice" -> Optional.of(BindingInformation.Type.SLICE);
			default -> Optional.empty();
		};

		return getterStyleEnum.map(o -> new BindingInformation(bindingMethod, o, bindingMethod.getReturnType()));
	}

	/**
	 * A list of extra declarations
	 * @return a list of extra declarations
	 */
	default List<? extends StringTemplate> extraDeclarations(StructureGenerationRequest request) {
		return Collections.emptyList();
	}

	/**
	 * Information about a Java method linking native memory and a raw Java-type
	 * @param bindingMethod the method
	 * @param getterStyle the style of the method
	 * @param type the Java type
	 */
	record BindingInformation(ExecutableElement bindingMethod, Type getterStyle, TypeMirror type) {
		/**
		 * The type of the binding method
		 */
		enum Type {
			/**
			 * A {@code name$slice} style accessor
			 */
			SLICE,

			/**
			 * A {@code name$get} style accessor
			 */
			VALUE
		}
	}

	/**
	 * Creates a suitable structure member for the given binding and C++ information
	 *
	 * @param bindingInformation the Java-binding information
	 * @param cppMember          the C++ information
	 * @return a suitable structure member
	 */
	static StructureMember createStructureMember(BindingInformation bindingInformation, CommentParser.StructMember cppMember) throws GenerationFailedException {
		var context = StructureGenerator.getContext();

		return switch (cppMember.declarator()) {
			case CommentParser.Value _ when context.mapping(cppMember.type().toString()).orElse(null) instanceof FunctionPointerMapping mapping ->
				new FunctionPointerMember(cppMember.name(), mapping.target());

			// VkSomeMappedStruct someMappedStruct;
			case CommentParser.Value _ when context.mapping(cppMember.type().toString()).orElse(null) instanceof TargetedMapping mapping ->
				new MappedConversionMember(cppMember.name(), mapping);

			// VkSomeFlags flags;
			case CommentParser.Value _ when context.mapping(cppMember.type().toString()).orElse(null) instanceof FlagGenerationRequest mapping ->
				new FlagConversionMember(cppMember.name(), mapping);

			// VkSomeFlagBits flagBit;
			case CommentParser.Value _ when context.mapping(cppMember.type().toString()).orElse(null) instanceof EnumMapping mapping ->
				new EnumConversionMember(cppMember.name(), mapping);

			// uint32_t aType;
			case CommentParser.Value _ ->
				new ValueMember(cppMember.name(),
						bindingInformation.type);

			case CommentParser.Pointer(CommentParser.Declarator to) when to instanceof CommentParser.Value &&
					"void".contentEquals(cppMember.type()) && "pNext".contentEquals(cppMember.name()) ->
				new NextMember();

			// Type *aPointer, (*aFunction)();
			case CommentParser.Pointer _, CommentParser.Function _ -> {
				var memorySegment = context.lookup().memorySegment()
						.orElseThrow(() -> new GenerationFailedException(STR."Unable to find \{MemorySegment.class.getCanonicalName()}", bindingInformation.bindingMethod));

				yield new ValueMember(cppMember.name(),
						memorySegment.asType());
			}

			// char string[12];
			case CommentParser.Array(CommentParser.Declarator of) when of instanceof CommentParser.Value &&
					"char".contentEquals(cppMember.type()) ->
				new TextMember(cppMember.name());

			// float f[12];
			case CommentParser.Array(CommentParser.Declarator of) when of instanceof CommentParser.Value &&
					"float".contentEquals(cppMember.type()) -> {
				var valueLayoutElement = context.lookup().valueLayout()
						.orElseThrow(() -> new GenerationFailedException(STR."No \{ValueLayout.class.getCanonicalName()} type found", bindingInformation.bindingMethod));

				var layoutList = ElementFilter.fieldsIn(valueLayoutElement.getEnclosedElements()).stream()
						.filter(f -> f.getSimpleName().contentEquals("JAVA_FLOAT"))
						.toList();

				if (layoutList.size() != 1) {
					throw new GenerationFailedException(STR."Unable to find \{ValueLayout.class.getSimpleName()}.JAVA_FLOAT", bindingInformation.bindingMethod);
				}

				yield new PrimitiveArrayMember(cppMember.name(),
						context.processingEnvironment().getTypeUtils().getArrayType(context.processingEnvironment().getTypeUtils().getPrimitiveType(TypeKind.FLOAT)),
						layoutList.getFirst());
			}

			// someType knownVariableName
			case CommentParser.Array(CommentParser.Declarator of) when of instanceof CommentParser.Value &&
					context.mapping(STR."\{cppMember.type()} \{cppMember.name()}").orElse(null) instanceof NewMapping mapping ->
				new MappedConversionMember(cppMember.name(), mapping);

			// VkSomeMappedStruct array[21];
			case CommentParser.Array(CommentParser.Declarator of) when of instanceof CommentParser.Value &&
					context.mapping(cppMember.type().toString()).orElse(null) instanceof TargetedMapping mapping -> {
				// Find the layout object
				var layoutMethods = ElementFilter.methodsIn(mapping.target().getEnclosedElements()).stream()
						.filter(m -> m.getParameters().isEmpty() && m.getSimpleName().contentEquals("$LAYOUT"))
						.toList();

				if (layoutMethods.size() != 1) {
					throw new GenerationFailedException(STR."Failed to find a single $LAYOUT method for \{mapping.target()}", mapping.target());
				}

				yield new MappedListMember(cppMember.name(), mapping, layoutMethods.getFirst());
			}

			// VkPhysicalDevice device[2];
			case CommentParser.Array(CommentParser.Declarator of) when of instanceof CommentParser.Value
					&& context.getLayout(cppMember.type()).orElse(null) instanceof VariableElement layout
					&& context.processingEnvironment().getTypeUtils().isSameType(
							layout.asType(),
							context.lookup().addressLayout()
									.orElseThrow(() -> new GenerationFailedException(STR."Unable to find \{AddressLayout.class.getCanonicalName()}", bindingInformation.bindingMethod))
									.asType()) ->
				new MemorySegmentListMember(cppMember.name(), layout);

			// uint32_t array[12];
			case CommentParser.Array(CommentParser.Declarator of) when of instanceof CommentParser.Value
					&& context.getLayout(cppMember.type()).orElse(null) instanceof VariableElement layout -> {

				// Get the array-type
				var memorySegment = context.lookup().memorySegment()
						.orElseThrow(() -> new GenerationFailedException(STR."Unable to find \{MemorySegment.class.getCanonicalName()}", bindingInformation.bindingMethod));

				var layoutTypes = ElementFilter.methodsIn(memorySegment.getEnclosedElements()).stream()
						.filter(method -> "toArray".contentEquals(method.getSimpleName())
								&& method.getParameters().size() == 1
								&& context.processingEnvironment().getTypeUtils().isSameType(method.getParameters().getFirst().asType(), layout.asType()))
						.map(ExecutableElement::getReturnType)
						.toList();

				if (layoutTypes.size() != 1) {
					throw new GenerationFailedException(STR."Unable to find the single \{MemorySegment.class.getSimpleName()}.toArray(\{layout.asType()}) method", bindingInformation.bindingMethod);
				}

				yield new PrimitiveArrayMember(cppMember.name(), layoutTypes.getFirst(), layout);
			}

			default -> throw new GenerationFailedException(STR."Unhandled member type '\{cppMember}'", bindingInformation.bindingMethod);
		};
	}

	/**
	 * The type of this member
	 * @return the name
	 */
	Object type();

	/**
	 * The name of this member
	 * @return the name
	 */
	CharSequence name();

	/**
	 * Generates a snippet of code that, using the given argument, constructs the member's Java type
	 * @param request the request object for this structure
	 * @param argument the name of the argument containing native memory
	 * @return a code snippet
	 */
	StringTemplate of(StructureGenerationRequest request, CharSequence argument);

	/**
	 * Generates a snippet of code that writes this member's information into the given argument
	 * @param request the request object for this structure
	 * @param destination the name of the argument containing native memory
	 * @param allocator the name of the argument containing the allocator
	 * @return a code snippet
	 */
	StringTemplate asRaw(StructureGenerationRequest request, CharSequence destination, CharSequence allocator);

	/**
	 * A code snippet for this member's declaration
	 * @return a code snippet
	 */
	default StringTemplate declaration() {
		return RAW."\{type()} \{name()}";
	}
}
