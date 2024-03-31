package dev.brownjames.lawu.vulkan;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;

import dev.brownjames.lawu.vulkan.bindings.*;

public final class Vulkan {
	public interface GlobalFunctionLookup extends FunctionLookup.FromMemorySegment {
		InstanceFunctionLookup instanceFunctionLookup(MemorySegment instance);

		default InstanceFunctionLookup instanceFunctionLookup(VulkanInstance instance) {
			assert instance != null;

			return instanceFunctionLookup(instance.handle());
		}

		static GlobalFunctionLookup from(PFN_vkGetInstanceProcAddr getInstanceProcAddr) {
			return new GlobalFunctionLookup() {
				static Optional<MemorySegment> convertToOptional(MemorySegment address) {
					assert address != null;

					if (address.address() == 0L) {
						return Optional.empty();
					}

					return Optional.of(address);
				}

				@Override
				public InstanceFunctionLookup instanceFunctionLookup(MemorySegment instance) {
					assert instance != null;

					return name -> convertToOptional(getInstanceProcAddr.apply(instance, name));
				}

				@Override
				public Optional<MemorySegment> lookup(MemorySegment name) {
					assert name != null;

					return convertToOptional(getInstanceProcAddr.apply(MemorySegment.NULL, name));
				}
			};
		}

		static GlobalFunctionLookup exportedEndpoint() {
			return from(vulkan_h::vkGetInstanceProcAddr);
		}
	}

	private static final GlobalFunctionLookup globalFunctionLookup = GlobalFunctionLookup.exportedEndpoint()
			.lookup("vkGetInstanceProcAddr")
			.map(address -> GlobalFunctionLookup.from(PFN_vkGetInstanceProcAddr.ofAddress(address, Arena.global())))
			/*
			 * @note james.brown 31 March 2024
			 * Pre-1.2 Vulkan loaders will not be able to look up "vkGetInstanceProcAddr" with NULL, fall back to the exported version
			 */
			.orElse(GlobalFunctionLookup.exportedEndpoint());

	private static final PFN_vkCreateInstance createInstance = globalFunctionLookup()
			.lookup("vkCreateInstance")
			.map(address -> PFN_vkCreateInstance.ofAddress(address, Arena.global()))
			.orElseThrow();

	public static GlobalFunctionLookup globalFunctionLookup() {
		return globalFunctionLookup;
	}

	public static int checkResult(int result) {
		if (result < 0) {
			throw new VulkanException(result);
		}

		return result;
	}

	public static MemorySegment createInstance(MemorySegment instanceCreateInfo) {
		try (var arena = Arena.ofConfined()) {
			var instance = arena.allocate(vulkan_h.VkInstance);

			int result = createInstance.apply(instanceCreateInfo, MemorySegment.NULL, instance);
			Vulkan.checkResult(result);

			return instance.get(vulkan_h.VkInstance, 0L);
		}
	}
}
