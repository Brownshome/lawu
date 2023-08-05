package dev.brownjames.lawu.vulkan;

import de.skuzzle.semantic.Version;

/**
 * A Vulkan version number
 * @param variant the Vulkan variant, normally 0
 * @param major the major version number
 * @param minor the minor version number
 * @param patch the patch version number
 */
public record VulkanVersionNumber(int variant, int major, int minor, int patch) {
	private static final int DEFAULT_VARIANT = 0;
	private static final int VARIANT_BITS = 3;
	private static final int MAJOR_BITS = 7;
	private static final int MINOR_BITS = 10;
	private static final int PATCH_BITS = 12;

	public VulkanVersionNumber {
		assert Integer.compareUnsigned(variant, 1 << VARIANT_BITS) < 0;
		assert Integer.compareUnsigned(major, 1 << MAJOR_BITS) < 0;
		assert Integer.compareUnsigned(minor, 1 << MINOR_BITS) < 0;
		assert Integer.compareUnsigned(patch, 1 << PATCH_BITS) < 0;
	}

	/**
	 * Decodes a Vulkan version integer as described by {@code VK_MAKE_API_VERSION}
	 * @param version the version number to decode
	 * @return a version number object
	 */
	public static VulkanVersionNumber of(int version) {
		int patch = version & (1 << PATCH_BITS) - 1;
		version >>>= PATCH_BITS;

		int minor = version & (1 << MINOR_BITS) - 1;
		version >>>= MINOR_BITS;

		int major = version & (1 << MAJOR_BITS) - 1;
		version >>>= MAJOR_BITS;

		int variant = version;

		return new VulkanVersionNumber(variant, major, minor, patch);
	}

	/**
	 * Converts a semantic version number into a Vulkan version. The Vulkan version is assumed to have a variant of zero.
	 * @param version the semantic version to convert
	 * @return a version
	 */
	public static VulkanVersionNumber of(Version version) {
		try {
			var variant = version.isPreRelease() ? Integer.parseInt(version.getPreRelease()) : DEFAULT_VARIANT;
			return new VulkanVersionNumber(variant, version.getMajor(), version.getMinor(), version.getPatch());
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid version", nfe);
		}
	}

	/**
	 * Encodes this version according as per the {@code VK_MAKE_API_VERSION} macro
	 * @return the encoded version
	 */
	public int encoded() {
		int result = variant;

		result <<= MAJOR_BITS;
		result |= major;

		result <<= MINOR_BITS;
		result |= minor;

		result <<= PATCH_BITS;
		result |= patch;

		return result;
	}

	/**
	 * Converts this version to a semantic version number. This variant information is encoded as pre-release information
	 * @return a semantic version
	 */
	public Version makeSemanticVersion() {
		return isStandardVariant()
				? Version.create(major, minor, patch)
				: Version.create(major, minor, patch, Integer.toString(variant));
	}

	public boolean isStandardVariant() {
		return variant == DEFAULT_VARIANT;
	}
}
