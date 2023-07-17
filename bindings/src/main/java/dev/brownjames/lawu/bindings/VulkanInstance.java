package dev.brownjames.lawu.bindings;

import de.skuzzle.semantic.Version;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Optional;

import static dev.brownjames.lawu.bindings.vulkan_h.*;

public final class VulkanInstance implements AutoCloseable {
	private static final System.Logger LOGGER = System.getLogger(String.valueOf(VulkanInstance.class.getModule().getName()));

	private final MemorySegment handle;

	private final MethodHandle getInstanceProcAddress;
	private final MethodHandle
			destroyInstance,
			enumeratePhysicalDevices;

	public record Builder(Optional<String> applicationName, Optional<Integer> applicationVersion, Optional<String> engineName, Optional<Integer> engineVersion) {
		public Builder() {
			this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
		}

		public Builder withApplicationName(String name) {
			return new Builder(Optional.of(name), applicationVersion, engineName, engineVersion);
		}

		public Builder withApplicationVersion(int version) {
			return new Builder(applicationName, Optional.of(version), engineName, engineVersion);
		}

		public Builder withApplicationVersion(Version version) {
			return withApplicationVersion(VulkanVersionNumber.of(version.toStable()).encoded());
		}

		public Builder withEngineName(String name) {
			return new Builder(applicationName, applicationVersion, Optional.of(name), engineVersion);
		}

		public Builder withEngineVersion(int version) {
			return new Builder(applicationName, applicationVersion, engineName, Optional.of(version));
		}

		public Builder withEngineVersion(Version version) {
			return withEngineVersion(VulkanVersionNumber.of(version.toStable()).encoded());
		}

		public VulkanInstance build() {
			if (applicationName.isEmpty() && applicationVersion.isEmpty() && engineName.isEmpty() && engineVersion.isEmpty()) {
				return create();
			}

			return create(applicationName.orElse(null), applicationVersion.orElse(0), engineName.orElse(null), engineVersion.orElse(0));
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static VulkanInstance create() {
		return create(MemorySegment.NULL);
	}

	public static VulkanInstance create(String applicationName, int applicationVersion, String engineName, int engineVersion) {
		try (var arena = Arena.openConfined()) {
			var applicationInfo = VkApplicationInfo.allocate(arena);
			VkApplicationInfo.sType$set(applicationInfo, VK_STRUCTURE_TYPE_APPLICATION_INFO());
			VkApplicationInfo.pNext$set(applicationInfo, MemorySegment.NULL);
			VkApplicationInfo.pApplicationName$set(applicationInfo, applicationName == null ? MemorySegment.NULL : arena.allocateUtf8String(applicationName));
			VkApplicationInfo.applicationVersion$set(applicationInfo, applicationVersion);
			VkApplicationInfo.pEngineName$set(applicationInfo, engineName == null ? MemorySegment.NULL : arena.allocateUtf8String(engineName));
			VkApplicationInfo.engineVersion$set(applicationInfo, engineVersion);
			VkApplicationInfo.apiVersion$set(applicationInfo, VK_API_VERSION_1_3());

			return create(applicationInfo);
		}
	}

	private static VulkanInstance create(MemorySegment applicationInfo) {
		try (var arena = Arena.openConfined()) {
			var instanceCreateInfo = VkInstanceCreateInfo.allocate(arena);
			VkInstanceCreateInfo.sType$set(instanceCreateInfo, VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO());
			VkInstanceCreateInfo.pNext$set(instanceCreateInfo, MemorySegment.NULL);
			VkInstanceCreateInfo.flags$set(instanceCreateInfo, 0);
			VkInstanceCreateInfo.pApplicationInfo$set(instanceCreateInfo, applicationInfo);
			VkInstanceCreateInfo.enabledExtensionCount$set(instanceCreateInfo, 0);
			VkInstanceCreateInfo.enabledLayerCount$set(instanceCreateInfo, 0);

			var instance = arena.allocate(VkInstance);

			int result = vkCreateInstance(instanceCreateInfo, MemorySegment.NULL, instance);
			Vulkan.checkResult(result);

			return new VulkanInstance(instance.get(VkInstance, 0L));
		}
	}

	public VulkanInstance(MemorySegment handle) {
		this.handle = handle;

		// Lookup the lookup function
		var backupGetInstanceProcAddr = vkGetInstanceProcAddr$MH().bindTo(this.handle);
		getInstanceProcAddress = getOrBackup("vkGetInstanceProcAddr",
				constants$0.vkGetInstanceProcAddr$FUNC,
				vkGetInstanceProcAddr$MH(),
				backupGetInstanceProcAddr);

		// Find the rest of the functions
		destroyInstance = getOrBackup("vkDestroyInstance",
				constants$0.vkDestroyInstance$FUNC,
				vkDestroyInstance$MH());
		enumeratePhysicalDevices = getOrBackup("vkEnumeratePhysicalDevices",
				constants$0.vkEnumeratePhysicalDevices$FUNC,
				vkEnumeratePhysicalDevices$MH());
	}

	public MemorySegment handle() {
		return handle;
	}

	private MethodHandle getOrBackup(String name, FunctionDescriptor descriptor, MethodHandle backup, MethodHandle lookupFunction) {
		return lookupSymbol(name, lookupFunction)
				.map(address -> Linker.nativeLinker().downcallHandle(address, descriptor))
				.orElseGet(() -> {
					LOGGER.log(System.Logger.Level.WARNING, "Unable to lookup {0}, performance may degrade", name);
					return backup;
				}).bindTo(handle);
	}

	private MethodHandle getOrBackup(String name, FunctionDescriptor descriptor, MethodHandle backup) {
		return getOrBackup(name, descriptor, backup, getInstanceProcAddress);
	}

	private Optional<MemorySegment> lookupSymbol(String s, MethodHandle lookupFunction) {
		try (Arena arena = Arena.openConfined()) {
			MemorySegment address = (MemorySegment) lookupFunction.invokeExact(arena.allocateUtf8String(s));
			if (address.address() == 0L) {
				return Optional.empty();
			} else {
				return Optional.of(address);
			}
		} catch (Throwable e) {
			throw new AssertionError(e);
		}
	}

	public List<VulkanPhysicalDevice> allPhysicalDevices() {
		try (var arena = Arena.openConfined()) {
			var deviceCount = arena.allocate(uint32_t);
			int result;

			try {
				result = (int) enumeratePhysicalDevices.invokeExact(deviceCount, MemorySegment.NULL);
			} catch (Throwable e) {
				throw new AssertionError(e);
			}
			Vulkan.checkResult(result);

			var devices = arena.allocateArray(VkPhysicalDevice, deviceCount.get(uint32_t, 0L));

			try {
				result = (int) enumeratePhysicalDevices.invokeExact(deviceCount, devices);
			} catch (Throwable e) {
				throw new AssertionError(e);
			}
			Vulkan.checkResult(result);

			return devices.elements(VkPhysicalDevice)
					.map(VulkanPhysicalDevice::new)
					.toList();
		}
	}

	@Override
	public void close() {
		try {
			destroyInstance.invokeExact(MemorySegment.NULL);
		} catch (Throwable e) {
			throw new AssertionError(e);
		}
	}
}
