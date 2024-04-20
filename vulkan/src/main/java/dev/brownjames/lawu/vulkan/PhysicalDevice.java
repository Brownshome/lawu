package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.*;
import dev.brownjames.lawu.vulkan.getphysicaldeviceproperties2.GetPhysicalDeviceProperties2Extension;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PhysicalDevice implements VulkanHandle {
	public enum Type {
		OTHER(vulkan_h.VK_PHYSICAL_DEVICE_TYPE_OTHER()),
		INTEGRATED_GPU(vulkan_h.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU()),
		DISCRETE_GPU(vulkan_h.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU()),
		VIRTUAL_GPU(vulkan_h.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU()),
		CPU(vulkan_h.VK_PHYSICAL_DEVICE_TYPE_CPU());

		private final int value;

		Type(int value) {
			this.value = value;
		}

		public static Type of(int value) {
			assert Arrays.stream(values()).allMatch(e -> e.ordinal() == e.value());
			return values()[value];
		}

		public int value() {
			return value;
		}
	}

	public record Properties(
			VulkanVersionNumber apiVersion,
			int driverVersion,
			int vendorId,
			int deviceId,
			Type deviceType,
			String deviceName,
			PipelineCacheUUID pipelineCacheUUID,
			MemorySegment limits,
			MemorySegment sparseProperties
	) {
		public static Properties of(MemorySegment raw) {
			return new Properties(
					VulkanVersionNumber.of(VkPhysicalDeviceProperties.apiVersion$get(raw)),
					VkPhysicalDeviceProperties.driverVersion$get(raw),
					VkPhysicalDeviceProperties.vendorID$get(raw),
					VkPhysicalDeviceProperties.deviceID$get(raw),
					Type.of(VkPhysicalDeviceProperties.deviceType$get(raw)),
					VkPhysicalDeviceProperties.deviceName$slice(raw).getUtf8String(0),
					PipelineCacheUUID.of(VkPhysicalDeviceProperties.pipelineCacheUUID$slice(raw)),
					VkPhysicalDeviceProperties.limits$slice(raw),
					VkPhysicalDeviceProperties.sparseProperties$slice(raw)
			);
		}

		public void asNative(MemorySegment destination) {
			VkPhysicalDeviceProperties.apiVersion$set(destination, apiVersion.encoded());
			VkPhysicalDeviceProperties.driverVersion$set(destination, driverVersion);
			VkPhysicalDeviceProperties.vendorID$set(destination, vendorId);
			VkPhysicalDeviceProperties.deviceID$set(destination, deviceId);
			VkPhysicalDeviceProperties.deviceType$set(destination, deviceType.value());
			VkPhysicalDeviceProperties.deviceName$slice(destination).setUtf8String(0, deviceName);
			pipelineCacheUUID.asNative(VkPhysicalDeviceProperties.pipelineCacheUUID$slice(destination));
			VkPhysicalDeviceProperties.limits$slice(destination).copyFrom(limits);
			VkPhysicalDeviceProperties.sparseProperties$slice(destination).copyFrom(sparseProperties);
		}

		public MemorySegment asNative(SegmentAllocator allocator) {
			var raw = VkPhysicalDeviceProperties.allocate(allocator);
			asNative(raw);
			return raw;
		}

		public MemorySegment asNative() {
			return asNative(Arena.ofAuto());
		}
	}

	private final MemorySegment handle;
	private final VulkanInstance instance;

	public PhysicalDevice(MemorySegment handle, VulkanInstance instance) {
		this.handle = handle;
		this.instance = instance;
	}

	@Override
	public MemorySegment handle() {
		return handle;
	}

	public Properties getProperties(Arena arena) {
		var properties = VkPhysicalDeviceProperties.allocate(arena);
		instance.getPhysicalDeviceProperties(handle, properties);
		return Properties.of(properties);
	}

	public Properties getProperties() {
		return getProperties(Arena.ofAuto());
	}

	/**
	 * A next structure that can be used for getting properties
	 */
	public interface PropertiesNext extends NextStructure { }

	public record GetPropertiesResult(Properties properties, List<MemorySegment> nexts) { }

	private GetPropertiesResult getProperties(Arena arena, List<? extends PropertiesNext> nexts, Consumer<? super MemorySegment> propertiesPopulator) {
		var properties = VkPhysicalDeviceProperties2.allocate(arena);
		VkPhysicalDeviceProperties2.sType$set(properties, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2());

		var chain = NextStructure.buildNativeStructureChain(arena, nexts);
		VkPhysicalDeviceProperties2.pNext$set(properties, chain.head());

		propertiesPopulator.accept(properties);

		return new GetPropertiesResult(Properties.of(VkPhysicalDeviceProperties2.properties$slice(properties)), chain.items());
	}

	public GetPropertiesResult getProperties(List<? extends PropertiesNext> nexts) {
		return getProperties(Arena.ofAuto(), nexts);
	}

	public GetPropertiesResult getProperties(Arena arena, List<? extends PropertiesNext> nexts) {
		return getProperties(arena, nexts, properties -> instance.getPhysicalDeviceProperties2(handle, properties));
	}

	public GetPropertiesResult getProperties(List<? extends PropertiesNext> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getProperties(Arena.ofAuto(), nexts, extension);
	}

	public GetPropertiesResult getProperties(Arena arena, List<? extends PropertiesNext> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getProperties(arena, nexts, properties -> extension.getPhysicalDeviceProperties2(handle, properties));
	}

	public MemorySegment getFeatures(Arena arena) {
		var features = VkPhysicalDeviceFeatures.allocate(arena);
		instance.getPhysicalDeviceFeatures(handle, features);
		return features;
	}

	public MemorySegment getFeatures() {
		return getFeatures(Arena.ofAuto());
	}

	public interface FeaturesNext extends NextStructure { }

	public record GetFeaturesResult(MemorySegment features, List<MemorySegment> nexts) { }

	private GetFeaturesResult getFeatures(Arena arena, List<? extends FeaturesNext> nexts, Consumer<? super MemorySegment> featuresPopulator) {
		var features = VkPhysicalDeviceFeatures2.allocate(arena);
		VkPhysicalDeviceFeatures2.sType$set(features, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2());

		var chain = NextStructure.buildNativeStructureChain(arena, nexts);
		VkPhysicalDeviceFeatures2.pNext$set(features, chain.head());

		featuresPopulator.accept(features);

		return new GetFeaturesResult(features, chain.items());
	}

	public GetFeaturesResult getFeatures(Arena arena, List<? extends FeaturesNext> nexts) {
		return getFeatures(arena, nexts, features -> instance.getPhysicalDeviceFeatures2(handle, features));
	}

	public GetFeaturesResult getFeatures(List<? extends FeaturesNext> nexts) {
		return getFeatures(Arena.ofAuto(), nexts);
	}

	public GetFeaturesResult getFeatures(Arena arena, List<? extends FeaturesNext> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getFeatures(arena, nexts, features -> extension.getPhysicalDeviceFeatures2(handle, features));
	}

	public GetFeaturesResult getFeatures(List<? extends FeaturesNext> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getFeatures(Arena.ofAuto(), nexts, extension);
	}

	public ImageFormatProperties getImageFormatProperties(Arena arena, int format, int type, int imageTiling, int imageUsageFlags, int imageCreateFlags) {
		var properties = VkImageFormatProperties.allocate(arena);
		instance.getPhysicalDeviceImageFormatProperties(handle, format, type, imageTiling, imageUsageFlags, imageCreateFlags, properties);
		return ImageFormatProperties.of(properties);
	}

	public ImageFormatProperties getImageFormatProperties(int format, int type, int imageTiling, int imageUsageFlags, int imageCreateFlags) {
		return getImageFormatProperties(Arena.ofAuto(), format, type, imageTiling, imageUsageFlags, imageCreateFlags);
	}

	public interface ImageFormatPropertiesNext extends NextStructure { }
	public interface ImageFormatInfoNext extends NextStructure { }

	public record GetImageFormatPropertiesResult(ImageFormatProperties properties, List<MemorySegment> nexts) { }

	private GetImageFormatPropertiesResult getImageFormatProperties(Arena arena,
	                                                                 MemorySegment info,
	                                                                 List<? extends ImageFormatPropertiesNext> nexts,
	                                                                 BiConsumer<? super MemorySegment, ? super MemorySegment> propertiesPopulator) {
		var properties = VkImageFormatProperties2.allocate(arena);
		VkImageFormatProperties2.sType$set(properties, vulkan_h.VK_STRUCTURE_TYPE_IMAGE_FORMAT_PROPERTIES_2());

		var chain = NextStructure.buildNativeStructureChain(arena, nexts);
		VkImageFormatProperties2.pNext$set(properties, chain.head());

		propertiesPopulator.accept(info, properties);

		return new GetImageFormatPropertiesResult(ImageFormatProperties.of(VkImageFormatProperties2.imageFormatProperties$slice(properties)), chain.items());
	}

	private GetImageFormatPropertiesResult getImageFormatProperties(Arena arena, int format, int type, int imageTiling, int imageUsageFlags, int imageCreateFlags,
	                                                                List<? extends ImageFormatInfoNext> infoNexts, List<? extends ImageFormatPropertiesNext> nexts, BiConsumer<? super MemorySegment, ? super MemorySegment> propertiesPopulator) {
		try (var temporaryArena = Arena.ofConfined()) {
			var information = VkPhysicalDeviceImageFormatInfo2.allocate(temporaryArena);
			VkPhysicalDeviceImageFormatInfo2.sType$set(information, vulkan_h.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGE_FORMAT_INFO_2());

			var chain = NextStructure.buildNativeStructureChain(temporaryArena, infoNexts);
			VkPhysicalDeviceImageFormatInfo2.pNext$set(information, chain.head());

			VkPhysicalDeviceImageFormatInfo2.format$set(information, format);
			VkPhysicalDeviceImageFormatInfo2.type$set(information, type);
			VkPhysicalDeviceImageFormatInfo2.tiling$set(information, imageTiling);
			VkPhysicalDeviceImageFormatInfo2.usage$set(information, imageUsageFlags);
			VkPhysicalDeviceImageFormatInfo2.flags$set(information, imageCreateFlags);

			return getImageFormatProperties(arena, information, nexts, propertiesPopulator);
		}
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(int format, int type, int imageTiling, int imageUsageFlags,
	                                                               int imageCreateFlags, List<? extends ImageFormatInfoNext> infoNexts, List<? extends ImageFormatPropertiesNext> nexts) {
		return getImageFormatProperties(Arena.ofAuto(), format, type, imageTiling, imageUsageFlags, imageCreateFlags, infoNexts, nexts);
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(Arena arena, int format, int type, int imageTiling, int imageUsageFlags,
	                                                               int imageCreateFlags, List<? extends ImageFormatInfoNext> infoNexts, List<? extends ImageFormatPropertiesNext> nexts) {
		return getImageFormatProperties(arena, format, type, imageTiling, imageUsageFlags, imageCreateFlags,
				infoNexts, nexts, (info, properties) -> instance.getPhysicalDeviceImageFormatProperties2(handle, info, properties));
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(int format, int type, int imageTiling, int imageUsageFlags,
	                                                               int imageCreateFlags, List<? extends ImageFormatInfoNext> infoNexts, List<? extends ImageFormatPropertiesNext> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getImageFormatProperties(Arena.ofAuto(), format, type, imageTiling, imageUsageFlags, imageCreateFlags, infoNexts, nexts, extension);
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(Arena arena, int format, int type, int imageTiling, int imageUsageFlags,
	                                                               int imageCreateFlags, List<? extends ImageFormatInfoNext> infoNexts, List<? extends ImageFormatPropertiesNext> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getImageFormatProperties(arena, format, type, imageTiling, imageUsageFlags, imageCreateFlags,
				infoNexts, nexts, (info, properties) -> extension.getPhysicalDeviceImageFormatProperties2(handle, info, properties));
	}
}
