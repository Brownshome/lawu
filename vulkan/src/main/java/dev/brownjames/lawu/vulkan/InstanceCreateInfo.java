package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.VkInstanceCreateInfo;
import dev.brownjames.lawu.vulkan.bindings.vulkan_h;
import dev.brownjames.lawu.vulkan.debugutils.*;
import dev.brownjames.lawu.vulkan.directdriverloading.DirectDriverLoadingExtension;
import dev.brownjames.lawu.vulkan.directdriverloading.DirectDriverLoadingList;
import dev.brownjames.lawu.vulkan.directdriverloading.DirectDriverLoadingMode;
import dev.brownjames.lawu.vulkan.directdriverloading.VulkanDriver;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record InstanceCreateInfo(
		Optional<ApplicationInfo> applicationInfo,
		Collection<Flag> flags,
		Collection<String> extensionNames,
		Collection<String> layerNames,
		Collection<Next> nexts
) {
	/**
	 * An enumeration of flag bits for the create-info structure
	 */
	public enum Flag implements BitFlag {
		ENUMERATE_PORTABILITY(vulkan_h.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR());

		private final int bit;

		Flag(int bit) {
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
	public interface Next extends NextStructure { }

	public InstanceCreateInfo() {
		this(Optional.empty(), BitFlag.noFlags(Flag.class), List.of(), List.of(), List.of());
	}

	public InstanceCreateInfo withApplicationInfo(ApplicationInfo info) {
		return new InstanceCreateInfo(Optional.of(info), flags, extensionNames, layerNames, nexts);
	}

	public InstanceCreateInfo withFlag(Flag flag) {
		var newFlags = BitFlag.flags(flags);
		newFlags.add(flag);
		return new InstanceCreateInfo(applicationInfo, newFlags, extensionNames, layerNames, nexts);
	}

	public InstanceCreateInfo withExtension(String extensionName) {
		var newNames = new ArrayList<>(extensionNames);
		newNames.add(extensionName);
		return new InstanceCreateInfo(applicationInfo, flags, newNames, layerNames, nexts);
	}

	public InstanceCreateInfo withLayer(String layerName) {
		var newNames = new ArrayList<>(layerNames);
		newNames.add(layerName);
		return new InstanceCreateInfo(applicationInfo, flags, extensionNames, newNames, nexts);
	}

	public InstanceCreateInfo withPortabilityEnumeration() {
		return withFlag(Flag.ENUMERATE_PORTABILITY)
				.withExtension("VK_KHR_portability_enumeration");
	}

	public InstanceCreateInfo withNext(Next next) {
		var newNexts = new ArrayList<>(nexts);
		newNexts.add(next);
		return new InstanceCreateInfo(applicationInfo, flags, extensionNames, layerNames, newNexts);
	}

	public InstanceCreateInfo withDrivers(DirectDriverLoadingMode mode, VulkanDriver... drivers) {
		return withNext(new DirectDriverLoadingList(mode, List.of(drivers)))
				.withExtension(DirectDriverLoadingExtension.extensionName());
	}

	public InstanceCreateInfo withDebugCallback(Collection<DebugUtilsMessageSeverity> severities, Collection<DebugUtilsMessageType> types, DebugUtilsMessengerCallback callback) {
		return withDebugCallback(new DebugUtilsMessengerCreateInfo(severities, types, callback));
	}

	public InstanceCreateInfo withDebugCallback(DebugUtilsMessengerCreateInfo messengerCreateInfo) {
		return withNext(messengerCreateInfo)
				.withExtension(DebugUtilsExtension.extensionName());
	}

	public InstanceCreateInfo withValidationLayers() {
		return withLayer("VK_LAYER_KHRONOS_validation");
	}

	public void validateLayers() throws UnsupportedLayerException {
		var allLayers = LayerProperties.all();

		var unmatchedLayers = layerNames.stream()
				.filter(name -> allLayers.stream().noneMatch(layer -> layer.name().equals(name)))
				.toList();

		if (!unmatchedLayers.isEmpty()) {
			throw new UnsupportedLayerException(unmatchedLayers, allLayers);
		}
	}

	public void validateExtensions() throws UnsupportedExtensionException {
		var allExtensions = layerNames.stream()
				.flatMap(name -> ExtensionProperties.forLayer(name).stream())
				.collect(Collectors.toCollection(ArrayList::new));
		allExtensions.addAll(ExtensionProperties.vulkanOrImplicit());
		
		var unmatchedExtensions = extensionNames.stream()
				.filter(name -> allExtensions.stream().noneMatch(extension -> extension.name().equals(name)))
				.toList();
		
		if (!unmatchedExtensions.isEmpty()) {
			throw new UnsupportedExtensionException(unmatchedExtensions, allExtensions);
		}
	}

	/**
	 * Checks the layers and extensions for validity then builds the instance
	 * @return a vulkan instance
	 * @throws UnsupportedLayerException if layers are not supported
	 * @throws UnsupportedExtensionException if the requested extensions are not supported
	 */
	public VulkanInstance validate() throws VulkanValidationException {
		if (applicationInfo.isPresent()) {
			applicationInfo.get().validate();
		}

		validateLayers();
		validateExtensions();

		return build();
	}

	public VulkanInstance build() {
		try (var arena = Arena.ofConfined()) {
			return VulkanInstance.create(createNativeStructure(arena));
		}
	}

	public MemorySegment createNativeStructure(Arena arena) {
		var instanceCreateInfo = VkInstanceCreateInfo.allocate(arena);
		VkInstanceCreateInfo.sType$set(instanceCreateInfo, vulkan_h.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO());

		VkInstanceCreateInfo.pNext$set(instanceCreateInfo, NextStructure.buildNativeStructureChain(arena, nexts).head());
		VkInstanceCreateInfo.flags$set(instanceCreateInfo, BitFlag.getFlagBits(flags));
		VkInstanceCreateInfo.pApplicationInfo$set(instanceCreateInfo, applicationInfo
				.map(info -> info.createNativeStructure(arena))
				.orElse(MemorySegment.NULL));

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

		VkInstanceCreateInfo.enabledLayerCount$set(instanceCreateInfo, layerNames.size());
		if (!layerNames.isEmpty()) {
			var layerNamesArray = arena.allocateArray(BindingHelper.CHAR_POINTER, layerNames.size());

			int i = 0;
			for (var name : layerNames) {
				layerNamesArray.setAtIndex(BindingHelper.CHAR_POINTER, i, arena.allocateUtf8String(name));
				i++;
			}

			VkInstanceCreateInfo.ppEnabledLayerNames$set(instanceCreateInfo, layerNamesArray);
		}

		return instanceCreateInfo;
	}
}
