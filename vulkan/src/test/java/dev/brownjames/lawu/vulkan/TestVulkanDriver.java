package dev.brownjames.lawu.vulkan;

import dev.brownjames.lawu.vulkan.bindings.*;

import java.awt.geom.Area;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * This driver only implements the minimum needed to test the direct driver loading. This may break, as it is not a compliant driver.
 */
record TestVulkanDriver() implements VulkanDriver {
	private static final System.Logger LOGGER = System.getLogger(String.valueOf(TestVulkanDriver.class.getModule().getName()));

	private MemorySegment makeUpcall(Object callable, FunctionDescriptor descriptor) {
		Method callMethod = Arrays.stream(callable.getClass().getDeclaredMethods())
				.filter(m -> m.getName().equals("call"))
				.findFirst()
				.orElseThrow();

		try {
			return Linker.nativeLinker().upcallStub(
					MethodHandles.lookup().unreflect(callMethod).bindTo(callable),
					descriptor,
					Arena.global());
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public MemorySegment getInstanceProcAddressPointer() {
		return makeUpcall(new Object() {
			MemorySegment call(MemorySegment instance, MemorySegment nativeName) {
				var name = nativeName.getUtf8String(0);

				return switch (name) {
					case "vk_icdNegotiateLoaderICDInterfaceVersion" -> makeUpcall(new Object() {
						int call(MemorySegment supportedVersion) {
							return vulkan_h.VK_SUCCESS();
						}
					}, FunctionDescriptor.of(ValueLayout.JAVA_INT,
							AddressLayout.ADDRESS.withTargetLayout(vulkan_h.uint32_t)));
					case "vkEnumerateInstanceVersion" -> makeUpcall(new Object() {
						int call(MemorySegment apiVersion) {
							apiVersion.set(vulkan_h.uint32_t, 0, vulkan_h.VK_VERSION_1_3());
							return vulkan_h.VK_SUCCESS();
						}
					}, FunctionDescriptor.of(ValueLayout.JAVA_INT,
							AddressLayout.ADDRESS.withTargetLayout(vulkan_h.uint32_t)));
					case "vkEnumerateInstanceExtensionProperties" -> makeUpcall(new Object() {
						int call(MemorySegment layerName, MemorySegment propertyCount, MemorySegment properties) {
							propertyCount.set(vulkan_h.uint32_t, 0, 0);
							return vulkan_h.VK_SUCCESS();
						}
					}, FunctionDescriptor.of(ValueLayout.JAVA_INT,
							BindingHelper.CHAR_POINTER,
							AddressLayout.ADDRESS.withTargetLayout(vulkan_h.uint32_t),
							AddressLayout.ADDRESS.withTargetLayout(VkExtensionProperties.$LAYOUT())));
					case "vkCreateInstance" -> makeUpcall(new Object() {
						int call(MemorySegment createInfo, MemorySegment allocator, MemorySegment instance) {
							return vulkan_h.VK_SUCCESS();
						}
					}, FunctionDescriptor.of(ValueLayout.JAVA_INT,
							AddressLayout.ADDRESS.withTargetLayout(VkInstanceCreateInfo.$LAYOUT()),
							AddressLayout.ADDRESS.withTargetLayout(VkAllocationCallbacks.$LAYOUT()),
							AddressLayout.ADDRESS.withTargetLayout(vulkan_h.VkInstance)));
					default -> makeUpcall(new Object() {
						void call() {
							throw new UnsupportedOperationException(name);
						}
					}, FunctionDescriptor.ofVoid());
				};
			}
		}, FunctionDescriptor.of(AddressLayout.ADDRESS, vulkan_h.VkInstance, BindingHelper.CHAR_POINTER));
	}
}
