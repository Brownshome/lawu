package dev.brownjames.lawu.vulkan;

import de.skuzzle.semantic.Version;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.*;

import dev.brownjames.lawu.vulkan.bindings.*;

import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class VulkanInstance implements AutoCloseable {
	private static final System.Logger LOGGER = System.getLogger(String.valueOf(VulkanInstance.class.getModule().getName()));

	private final MemorySegment handle;

	private final MethodHandle getInstanceProcAddress;
	private final MethodHandle
			destroyInstance,
			enumeratePhysicalDevices;

	/**
	 * An enumeration of flag bits for the create-info structure
	 */
	public enum CreateFlag implements BitFlag {
		ENUMERATE_PORTABILITY(vulkan_h.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR());

		private final int bit;

		CreateFlag(int bit) {
			this.bit = bit;
		}

		@Override
		public int bit() {
			return bit;
		}
	}

	/**
	 * A description of a pNext member for the createInstance function
	 */
	public interface CreateNext extends NextStructure { }

	public record Builder(
			Optional<String> applicationName,
			Optional<Integer> applicationVersion,
			Optional<String> engineName,
			Optional<Integer> engineVersion,
			Collection<CreateFlag> flags,
			Collection<String> extensionNames,
			Collection<CreateNext> nexts
	) {
		public Builder() {
			this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), BitFlag.noFlags(CreateFlag.class), List.of(), List.of());
		}

		public Builder withApplicationName(String name) {
			return new Builder(Optional.of(name), applicationVersion, engineName, engineVersion, flags, extensionNames, nexts);
		}

		public Builder withApplicationVersion(int version) {
			return new Builder(applicationName, Optional.of(version), engineName, engineVersion, flags, extensionNames, nexts);
		}

		public Builder withApplicationVersion(Version version) {
			return withApplicationVersion(VulkanVersionNumber.of(version.toStable()).encoded());
		}

		public Builder withEngineName(String name) {
			return new Builder(applicationName, applicationVersion, Optional.of(name), engineVersion, flags, extensionNames, nexts);
		}

		public Builder withEngineVersion(int version) {
			return new Builder(applicationName, applicationVersion, engineName, Optional.of(version), flags, extensionNames, nexts);
		}

		public Builder withEngineVersion(Version version) {
			return withEngineVersion(VulkanVersionNumber.of(version.toStable()).encoded());
		}

		public Builder withFlag(CreateFlag flag) {
			var newFlags = BitFlag.flags(flags);
			newFlags.add(flag);
			return new Builder(applicationName, applicationVersion, engineName, engineVersion, newFlags, extensionNames, nexts);
		}

		public Builder withExtension(String extensionName) {
			var newNames = new ArrayList<>(extensionNames);
			newNames.add(extensionName);
			return new Builder(applicationName, applicationVersion, engineName, engineVersion, flags, newNames, nexts);
		}

		public Builder withPortabilityEnumeration() {
			return withFlag(CreateFlag.ENUMERATE_PORTABILITY)
					.withExtension("VK_KHR_portability_enumeration");
		}

		public Builder withNext(CreateNext next) {
			var newNexts = new ArrayList<>(nexts);
			newNexts.add(next);
			return new Builder(applicationName, applicationVersion, engineName, engineVersion, flags, extensionNames, newNexts);
		}

		public Builder withDrivers(DirectDriverLoadingMode mode, VulkanDriver... drivers) {
			return withNext(new DirectDriverLoadingList(mode, List.of(drivers)))
					.withExtension("VK_LUNARG_direct_driver_loading");
		}

		public Builder withDebugCallback(Collection<DebugUtilsMessageSeverity> severities, Collection<DebugUtilsMessageType> types, DebugUtilsMessengerCallback callback) {
			return withNext(new DebugUtilsMessengerCreateInfo(severities, types, callback))
					.withExtension("VK_EXT_debug_utils");
		}

		public VulkanInstance build() {
			if (applicationName.isEmpty() && applicationVersion.isEmpty() && engineName.isEmpty() && engineVersion.isEmpty() && flags.isEmpty() && extensionNames.isEmpty() && nexts.isEmpty()) {
				return create();
			}

			return create(applicationName.orElse(null), applicationVersion.orElse(0), engineName.orElse(null), engineVersion.orElse(0), flags, extensionNames, nexts);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static VulkanInstance create() {
		return create(MemorySegment.NULL, BitFlag.noFlags(CreateFlag.class), List.of(), List.of());
	}

	public static VulkanInstance create(String applicationName, int applicationVersion, String engineName, int engineVersion, Collection<CreateFlag> flags, Collection<String> extensionNames, Collection<? extends CreateNext> nexts) {
		try (var arena = Arena.ofConfined()) {
			var applicationInfo = VkApplicationInfo.allocate(arena);
			VkApplicationInfo.sType$set(applicationInfo, vulkan_h.VK_STRUCTURE_TYPE_APPLICATION_INFO());
			VkApplicationInfo.pNext$set(applicationInfo, MemorySegment.NULL);
			VkApplicationInfo.pApplicationName$set(applicationInfo, applicationName == null ? MemorySegment.NULL : arena.allocateUtf8String(applicationName));
			VkApplicationInfo.applicationVersion$set(applicationInfo, applicationVersion);
			VkApplicationInfo.pEngineName$set(applicationInfo, engineName == null ? MemorySegment.NULL : arena.allocateUtf8String(engineName));
			VkApplicationInfo.engineVersion$set(applicationInfo, engineVersion);
			VkApplicationInfo.apiVersion$set(applicationInfo, vulkan_h.VK_API_VERSION_1_3());

			return create(applicationInfo, flags, extensionNames, nexts);
		}
	}

	public static VulkanInstance create(MemorySegment applicationInfo, Collection<CreateFlag> flags, Collection<String> extensionNames, Collection<? extends CreateNext> nexts) {
		try (var arena = Arena.ofConfined()) {
			var instanceCreateInfo = VkInstanceCreateInfo.allocate(arena);
			VkInstanceCreateInfo.sType$set(instanceCreateInfo, vulkan_h.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO());

			VkInstanceCreateInfo.pNext$set(instanceCreateInfo, NextStructure.buildNativeStructureChain(arena, nexts));
			VkInstanceCreateInfo.flags$set(instanceCreateInfo, BitFlag.getFlagBits(flags));
			VkInstanceCreateInfo.pApplicationInfo$set(instanceCreateInfo, applicationInfo);

			VkInstanceCreateInfo.enabledExtensionCount$set(instanceCreateInfo, extensionNames.size());
			if (!extensionNames.isEmpty()) {
				var extensionNamesArray = arena.allocateArray(BindingHelper.CHAR_POINTER, extensionNames.size());

				int i = 0;
				for (var name : extensionNames) {
					extensionNamesArray.setAtIndex(BindingHelper.CHAR_POINTER, i, arena.allocateUtf8String(name));
					i++;
				}

				VkInstanceCreateInfo.ppEnabledExtensionNames$set(instanceCreateInfo, extensionNamesArray);
			}

			VkInstanceCreateInfo.enabledLayerCount$set(instanceCreateInfo, 0);
			return create(instanceCreateInfo);
		}
	}

	public static VulkanInstance create(MemorySegment instanceCreateInfo) {
		try (var arena = Arena.ofConfined()) {
			var instance = arena.allocate(vulkan_h.VkInstance);

			int result = vulkan_h.vkCreateInstance(instanceCreateInfo, MemorySegment.NULL, instance);
			Vulkan.checkResult(result);

			return new VulkanInstance(instance.get(vulkan_h.VkInstance, 0L));
		}
	}

	public VulkanInstance(MemorySegment handle) {
		this.handle = handle;

		// Lookup the lookup function
		var backupGetInstanceProcAddr = vulkan_h.vkGetInstanceProcAddr$MH().bindTo(this.handle);
		getInstanceProcAddress = getOrBackup("vkGetInstanceProcAddr",
				FunctionDescriptor.of(AddressLayout.ADDRESS, vulkan_h.VkInstance, BindingHelper.CHAR_POINTER),
				vulkan_h.vkGetInstanceProcAddr$MH(),
				backupGetInstanceProcAddr);

		// Find the rest of the functions
		destroyInstance = getOrBackup("vkDestroyInstance",
				FunctionDescriptor.ofVoid(vulkan_h.VkInstance, AddressLayout.ADDRESS.withTargetLayout(VkAllocationCallbacks.$LAYOUT())),
				vulkan_h.vkDestroyInstance$MH());
		enumeratePhysicalDevices = getOrBackup("vkEnumeratePhysicalDevices",
				FunctionDescriptor.of(JAVA_INT,
						vulkan_h.VkInstance,
						AddressLayout.ADDRESS.withTargetLayout(vulkan_h.uint32_t),
						AddressLayout.ADDRESS.withTargetLayout(vulkan_h.VkPhysicalDevice)),
				vulkan_h.vkEnumeratePhysicalDevices$MH());
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
		try (Arena arena = Arena.ofConfined()) {
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
		try (var arena = Arena.ofConfined()) {
			var deviceCount = arena.allocate(vulkan_h.uint32_t);
			int result;

			try {
				result = (int) enumeratePhysicalDevices.invokeExact(deviceCount, MemorySegment.NULL);
			} catch (Throwable e) {
				throw new AssertionError(e);
			}
			Vulkan.checkResult(result);

			var devices = arena.allocateArray(vulkan_h.VkPhysicalDevice, deviceCount.get(vulkan_h.uint32_t, 0L));

			try {
				result = (int) enumeratePhysicalDevices.invokeExact(deviceCount, devices);
			} catch (Throwable e) {
				throw new AssertionError(e);
			}
			Vulkan.checkResult(result);

			return devices.elements(vulkan_h.VkPhysicalDevice)
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
