package dev.brownjames.lawu.vulkan.generator.structure;

import java.util.List;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;

final class NameUtilities {
	private static final String VULKAN_FUNCTION_POINTER_PREFIX = "PFN_vk";
	private static final String VULKAN_PREFIX = "Vk";
	private static final String BIT_FLAG_SUFFIX = "Flags";
	static final NumberFormat SPELL_OUT_FORMATTER = new RuleBasedNumberFormat(RuleBasedNumberFormat.SPELLOUT);

	private NameUtilities() { }

	static boolean isVulkanCoreName(CharSequence name) {
		return isVulkanName(name) && !isExtensionName(name);
	}

	static boolean isCoreVulkanFunctionPointerName(CharSequence name) {
		return isFunctionPointerName(name) && !isExtensionName(name);
	}

	private static boolean isFunctionPointerName(CharSequence name) {
		return name.toString().startsWith(VULKAN_FUNCTION_POINTER_PREFIX);
	}

	static boolean isExtensionName(CharSequence name) {
		return Character.isUpperCase(name.charAt(name.length() - 1)) && Character.isUpperCase(name.charAt(name.length() - 2));
	}

	private static boolean isVulkanName(CharSequence name) {
		return name.toString().startsWith(VULKAN_PREFIX);
	}

	static CharSequence convertStructureName(CharSequence nativeName) {
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

	static CharSequence convertBitFlagsName(CharSequence nativeName) {
		var result = new StringBuilder(convertStructureName(nativeName));

		removeSuffix(result, BIT_FLAG_SUFFIX);
		result.append("Flag");

		return result;
	}

	static CharSequence convertEnumName(CharSequence nativeName) {
		return convertStructureName(nativeName);
	}

	static void removeSuffix(StringBuilder builder, String suffix) {
		removeIfMatched(builder, suffix, builder.length() - suffix.length(), builder.length());
	}

	static void removePrefix(StringBuilder builder, String prefix) {
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
}
