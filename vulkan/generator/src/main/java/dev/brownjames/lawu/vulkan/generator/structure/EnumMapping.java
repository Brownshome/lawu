package dev.brownjames.lawu.vulkan.generator.structure;

sealed interface EnumMapping extends NameMapping
		permits EnumGenerationRequest, FlagBitConversionMapping { }
