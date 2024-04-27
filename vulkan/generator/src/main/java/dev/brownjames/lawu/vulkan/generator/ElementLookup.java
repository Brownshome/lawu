package dev.brownjames.lawu.vulkan.generator;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import dev.brownjames.lawu.vulkan.annotation.GenerateCoreStructuresFrom;
import dev.brownjames.lawu.vulkan.annotation.MapStructure;
import dev.brownjames.lawu.vulkan.annotation.VulkanHeader;

public record ElementLookup(
		Optional<TypeElement> generateCoreStructuresFromAnnotation,
		Optional<TypeElement> mapStructureAnnotation,
		Optional<TypeElement> vulkanHeaderAnnotation,
		Optional<TypeElement> memorySegment,
		Optional<TypeElement> string,
		Optional<TypeElement> valueLayout,
		Optional<TypeElement> addressLayout,
		Optional<TypeElement> bitFlag
) {
	public static ElementLookup lookupElements(ProcessingEnvironment processingEnvironment) {
		return new ElementLookup(
				lookupElement(GenerateCoreStructuresFrom.class, processingEnvironment),
				lookupElement(MapStructure.class, processingEnvironment),
				lookupElement(VulkanHeader.class, processingEnvironment),
				lookupElement(MemorySegment.class, processingEnvironment),
				lookupElement(String.class, processingEnvironment),
				lookupElement(ValueLayout.class, processingEnvironment),
				lookupElement(AddressLayout.class, processingEnvironment),
				lookupElement("dev.brownjames.lawu.vulkan", "dev.brownjames.lawu.vulkan.BitFlag", processingEnvironment));
	}

	private static Optional<TypeElement> lookupElement(CharSequence moduleName, CharSequence typeName, ProcessingEnvironment processingEnvironment) {
		var module = processingEnvironment.getElementUtils().getModuleElement(moduleName);
		if (module != null) {
			return Optional.ofNullable(processingEnvironment.getElementUtils().getTypeElement(module, typeName));
		}

		return Optional.ofNullable(processingEnvironment.getElementUtils().getTypeElement(typeName));
	}

	private static Optional<TypeElement> lookupElement(Class<?> type, ProcessingEnvironment processingEnvironment) {
		var module = type.getModule();
		if (module.isNamed()) {
			var moduleElement = processingEnvironment.getElementUtils().getModuleElement(module.getName());
			if (moduleElement != null) {
				// Both the runtime and compilation environments have the requested module, only accept an exact match
				return Optional.ofNullable(processingEnvironment.getElementUtils().getTypeElement(moduleElement, type.getCanonicalName()));
			}
		}

		// Either the runtime or compilation environment doesn't contain the requested module
		return Optional.ofNullable(processingEnvironment.getElementUtils().getTypeElement(type.getCanonicalName()));
	}
}
