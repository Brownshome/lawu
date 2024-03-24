package dev.brownjames.lawu.vulkan;

import java.lang.foreign.MemorySegment;

public interface DebugUtilsMessengerCallback {
	void callback(DebugUtilsMessageSeverity severity, int messageTypes, MemorySegment callbackData);
}
