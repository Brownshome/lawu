package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.*;
import dev.brownjames.lawu.vulkan.getphysicaldeviceproperties2.GetPhysicalDeviceProperties2Extension;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class PhysicalDevice implements VulkanHandle {
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

	public PhysicalDeviceProperties getProperties() {
		try (var arena = Arena.ofConfined()) {
			var properties = VkPhysicalDeviceProperties.allocate(arena);
			instance.getPhysicalDeviceProperties(handle, properties);
			return PhysicalDeviceProperties.of(properties);
		}
	}

	public record GetPropertiesResult(PhysicalDeviceProperties properties, List<PhysicalDeviceProperties2.Next> nexts) { }

	private GetPropertiesResult getProperties(Arena arena, List<? extends PhysicalDeviceProperties2.Next> nexts, Consumer<? super MemorySegment> propertiesPopulator) {
		var properties = PhysicalDeviceProperties2.allocate(arena);
		var nativeNexts = nexts.stream().<PhysicalDeviceProperties2.Next>map(n -> n.asNative(arena)).toList();
		properties.addNext(arena, nativeNexts);

		propertiesPopulator.accept(properties.raw());

		return new GetPropertiesResult(properties.properties(), nativeNexts);
	}

	public GetPropertiesResult getProperties(List<? extends PhysicalDeviceProperties2.Next> nexts) {
		return getProperties(Arena.ofAuto(), nexts);
	}

	public GetPropertiesResult getProperties(Arena arena, List<? extends PhysicalDeviceProperties2.Next> nexts) {
		return getProperties(arena, nexts, properties -> instance.getPhysicalDeviceProperties2(handle, properties));
	}

	public GetPropertiesResult getProperties(List<? extends PhysicalDeviceProperties2.Next> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getProperties(Arena.ofAuto(), nexts, extension);
	}

	public GetPropertiesResult getProperties(Arena arena, List<? extends PhysicalDeviceProperties2.Next> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getProperties(arena, nexts, properties -> extension.getPhysicalDeviceProperties2(handle, properties));
	}

	public PhysicalDeviceFeatures getFeatures() {
		try (var arena = Arena.ofConfined()) {
			var features = VkPhysicalDeviceFeatures.allocate(arena);
			instance.getPhysicalDeviceFeatures(handle, features);
			return PhysicalDeviceFeatures.of(features);
		}
	}

	public record GetFeaturesResult(PhysicalDeviceFeatures features, List<PhysicalDeviceFeatures2.Next> nexts) { }

	private GetFeaturesResult getFeatures(Arena arena, List<? extends PhysicalDeviceFeatures2.Next> nexts, Consumer<? super MemorySegment> featuresPopulator) {
		var features = PhysicalDeviceFeatures2.allocate(arena);
		var nativeNexts = nexts.stream().<PhysicalDeviceFeatures2.Next>map(n -> n.asNative(arena)).toList();
		features.addNext(arena, nativeNexts);

		featuresPopulator.accept(features.raw());

		return new GetFeaturesResult(features.features(), nativeNexts);
	}

	public GetFeaturesResult getFeatures(Arena arena, List<? extends PhysicalDeviceFeatures2.Next> nexts) {
		return getFeatures(arena, nexts, features -> instance.getPhysicalDeviceFeatures2(handle, features));
	}

	public GetFeaturesResult getFeatures(List<? extends PhysicalDeviceFeatures2.Next> nexts) {
		return getFeatures(Arena.ofAuto(), nexts);
	}

	public GetFeaturesResult getFeatures(Arena arena, List<? extends PhysicalDeviceFeatures2.Next> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getFeatures(arena, nexts, features -> extension.getPhysicalDeviceFeatures2(handle, features));
	}

	public GetFeaturesResult getFeatures(List<? extends PhysicalDeviceFeatures2.Next> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getFeatures(Arena.ofAuto(), nexts, extension);
	}

	public ImageFormatProperties getImageFormatProperties(int format, int type, int imageTiling, int imageUsageFlags, int imageCreateFlags) {
		try (var arena = Arena.ofConfined()) {
			var properties = VkImageFormatProperties.allocate(arena);
			instance.getPhysicalDeviceImageFormatProperties(handle, format, type, imageTiling, imageUsageFlags, imageCreateFlags, properties);
			return ImageFormatProperties.of(properties);
		}
	}

	public record GetImageFormatPropertiesResult(ImageFormatProperties properties, List<ImageFormatProperties2.Next> nexts) { }

	private GetImageFormatPropertiesResult getImageFormatProperties(Arena arena,
	                                                                 MemorySegment info,
	                                                                 List<? extends ImageFormatProperties2.Next> nexts,
	                                                                 BiConsumer<? super MemorySegment, ? super MemorySegment> propertiesPopulator) {
		var properties = ImageFormatProperties2.allocate(arena);
		var nativeNexts = nexts.stream().<ImageFormatProperties2.Next>map(n -> n.asNative(arena)).toList();

		propertiesPopulator.accept(info, properties.raw());

		return new GetImageFormatPropertiesResult(properties.imageFormatProperties(), nativeNexts);
	}

	private GetImageFormatPropertiesResult getImageFormatProperties(Arena arena, Format format, ImageType type, ImageTiling imageTiling, Collection<ImageUsageFlag> imageUsageFlags, Collection<ImageCreateFlag> imageCreateFlags,
	                                                                List<? extends PhysicalDeviceImageFormatInfo2.Next> infoNexts, List<? extends ImageFormatProperties2.Next> nexts, BiConsumer<? super MemorySegment, ? super MemorySegment> propertiesPopulator) {
		try (var temporaryArena = Arena.ofConfined()) {
			var information = PhysicalDeviceImageFormatInfo2
					.of(StructureType.PHYSICAL_DEVICE_IMAGE_FORMAT_INFO_2, format, type, imageTiling, imageUsageFlags, imageCreateFlags)
					.asNative(temporaryArena);

			var nativeNexts = infoNexts.stream().<PhysicalDeviceImageFormatInfo2.Next>map(n -> n.asNative(temporaryArena)).toList();
			information.addNext(temporaryArena, nativeNexts);

			return getImageFormatProperties(arena, information.raw(), nexts, propertiesPopulator);
		}
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(Format format, ImageType type, ImageTiling imageTiling, Collection<ImageUsageFlag> imageUsageFlags,
	                                                               Collection<ImageCreateFlag> imageCreateFlags, List<? extends PhysicalDeviceImageFormatInfo2.Next> infoNexts, List<? extends ImageFormatProperties2.Next> nexts) {
		return getImageFormatProperties(Arena.ofAuto(), format, type, imageTiling, imageUsageFlags, imageCreateFlags, infoNexts, nexts);
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(Arena arena, Format format, ImageType type, ImageTiling imageTiling, Collection<ImageUsageFlag> imageUsageFlags,
	                                                               Collection<ImageCreateFlag> imageCreateFlags, List<? extends PhysicalDeviceImageFormatInfo2.Next> infoNexts, List<? extends ImageFormatProperties2.Next> nexts) {
		return getImageFormatProperties(arena, format, type, imageTiling, imageUsageFlags, imageCreateFlags,
				infoNexts, nexts, (info, properties) -> instance.getPhysicalDeviceImageFormatProperties2(handle, info, properties));
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(Format format, ImageType type, ImageTiling imageTiling, Collection<ImageUsageFlag> imageUsageFlags,
	                                                               Collection<ImageCreateFlag> imageCreateFlags, List<? extends PhysicalDeviceImageFormatInfo2.Next> infoNexts, List<? extends ImageFormatProperties2.Next> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getImageFormatProperties(Arena.ofAuto(), format, type, imageTiling, imageUsageFlags, imageCreateFlags, infoNexts, nexts, extension);
	}

	public GetImageFormatPropertiesResult getImageFormatProperties(Arena arena, Format format, ImageType type, ImageTiling imageTiling, Collection<ImageUsageFlag> imageUsageFlags,
	                                                               Collection<ImageCreateFlag> imageCreateFlags, List<? extends PhysicalDeviceImageFormatInfo2.Next> infoNexts, List<? extends ImageFormatProperties2.Next> nexts, GetPhysicalDeviceProperties2Extension extension) {
		return getImageFormatProperties(arena, format, type, imageTiling, imageUsageFlags, imageCreateFlags,
				infoNexts, nexts, (info, properties) -> extension.getPhysicalDeviceImageFormatProperties2(handle, info, properties));
	}

	public List<QueueFamilyProperties> getQueueFamilyProperties() {
		try (var arena = Arena.ofConfined()) {
			var familyCount = arena.allocate(vulkan_h.uint32_t);
			instance.getPhysicalDeviceQueueFamilyProperties(handle, familyCount, MemorySegment.NULL);

			var properties = VkQueueFamilyProperties.allocateArray(familyCount.get(vulkan_h.uint32_t, 0L), arena);
			instance.getPhysicalDeviceQueueFamilyProperties(handle, familyCount, properties);

			return properties.elements(VkQueueFamilyProperties.$LAYOUT())
					.<QueueFamilyProperties>map(QueueFamilyProperties::of)
					.toList();
		}
	}
}
