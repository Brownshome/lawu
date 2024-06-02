package dev.brownjames.lawu.vulkan;

import de.skuzzle.semantic.Version;
import dev.brownjames.lawu.vulkan.bindings.VkApplicationInfo;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;

import java.lang.foreign.*;
import java.util.Optional;

public record ApplicationInfo(
		Optional<String> applicationName,
		int applicationVersion,
		Optional<String> engineName,
		int engineVersion,
		Optional<VulkanVersionNumber> apiVersion
) {
	public ApplicationInfo() {
		this(Optional.empty(), 0, Optional.empty(), 0, Optional.empty());
	}

	public ApplicationInfo withApplicationName(String name) {
		assert name != null;

		return new ApplicationInfo(Optional.of(name), applicationVersion, engineName, engineVersion, apiVersion);
	}

	public ApplicationInfo withApplicationVersion(Version version) {
		return withApplicationVersion(VulkanVersionNumber.of(version.toStable()).asRaw());
	}

	public ApplicationInfo withApplicationVersion(int version) {
		return new ApplicationInfo(applicationName, version, engineName, engineVersion, apiVersion);
	}

	public ApplicationInfo withEngineName(String name) {
		assert name != null;

		return new ApplicationInfo(applicationName, engineVersion, Optional.of(name), engineVersion, apiVersion);
	}

	public ApplicationInfo withEngineVersion(Version version) {
		return withEngineVersion(VulkanVersionNumber.of(version.toStable()).asRaw());
	}

	public ApplicationInfo withEngineVersion(int version) {
		return new ApplicationInfo(applicationName, applicationVersion, engineName, version, apiVersion);
	}

	public ApplicationInfo withApiVersion(VulkanVersionNumber version) {
		return new ApplicationInfo(applicationName, applicationVersion, engineName, engineVersion, Optional.of(version));
	}

	private boolean isVulkanOnePointZero() {
		try (var arena = Arena.ofConfined()) {
			var enumerateInstanceVersionAddress = vulkan_h.vkGetInstanceProcAddr(MemorySegment.NULL, arena.allocateUtf8String("vkEnumerateInstanceVersion"));
			if (enumerateInstanceVersionAddress.address() == 0) {
				return true;
			}

			var enumerateInstance = Linker.nativeLinker().downcallHandle(enumerateInstanceVersionAddress,
					FunctionDescriptor.of(ValueLayout.JAVA_INT, AddressLayout.ADDRESS.withTargetLayout(vulkan_h.uint32_t)));

			try {
				var versionMemory = arena.allocate(vulkan_h.uint32_t);
				Vulkan.checkResult((int) enumerateInstance.invokeExact(versionMemory));
				int version = versionMemory.get(vulkan_h.uint32_t, 0);
				return version == vulkan_h.VK_API_VERSION_1_0();
			} catch (Throwable e) {
				throw new AssertionError(e);
			}
		}
	}

	public void validateVersionNumber() throws VulkanValidationException {
		if (isVulkanOnePointZero() && apiVersion.isPresent() && !apiVersion.get().equals(VulkanVersionNumber.of(vulkan_h.VK_API_VERSION_1_0()))) {
			throw new VulkanValidationException("Vulkan 1.0 implementations cannot support any API version other than 1.0");
		}
	}

	public void validate() throws VulkanValidationException {
		validateVersionNumber();
	}

	public MemorySegment createNativeStructure(Arena arena) {
		var applicationInfo = VkApplicationInfo.allocate(arena);
		VkApplicationInfo.sType$set(applicationInfo, vulkan_h.VK_STRUCTURE_TYPE_APPLICATION_INFO());
		VkApplicationInfo.pNext$set(applicationInfo, MemorySegment.NULL);
		VkApplicationInfo.pApplicationName$set(applicationInfo, applicationName.map(arena::allocateUtf8String).orElse(MemorySegment.NULL));
		VkApplicationInfo.applicationVersion$set(applicationInfo, applicationVersion);
		VkApplicationInfo.pEngineName$set(applicationInfo, engineName.map(arena::allocateUtf8String).orElse(MemorySegment.NULL));
		VkApplicationInfo.engineVersion$set(applicationInfo, engineVersion);
		VkApplicationInfo.apiVersion$set(applicationInfo, apiVersion.map(VulkanVersionNumber::asRaw).orElse(0));
		return applicationInfo;
	}
}
