package com.nxyn.aiclient.domain.model

enum class ProviderType(val displayName: String) {
    ANTHROPIC("Anthropic"),
    OPENAI_COMPATIBLE("OpenAI-compatible");

    companion object {
        fun fromRaw(value: String): ProviderType =
            entries.firstOrNull { it.name == value } ?: OPENAI_COMPATIBLE
    }
}

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long = 0,
    val role: MessageRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val usedFallback: Boolean = false
)

data class Conversation(
    val id: Long = 0,
    val title: String,
    val providerType: ProviderType,
    val modelId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProviderSettings(
    val providerType: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val baseUrl: String = ProviderType.OPENAI_COMPATIBLE.defaultBaseUrl(),
    val modelId: String = "",
    val useCustomModel: Boolean = false,
    val customModelId: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int? = null,
    val unlimitedTokens: Boolean = true,
    val systemPrompt: String = "You are a helpful AI assistant.",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val animationsEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val setupCompleted: Boolean = false
) {
    val effectiveModelId: String
        get() = if (useCustomModel && customModelId.isNotBlank()) customModelId else modelId
}

data class ConnectionResult(
    val success: Boolean,
    val httpStatus: Int? = null,
    val message: String,
    val possibleFix: String? = null,
    val responseTimeMs: Long? = null,
    val debugInfo: DebugInfo? = null
)

data class DebugInfo(
    val requestUrl: String,
    val httpStatus: Int?,
    val responseTimeMs: Long,
    val responseHeaders: Map<String, String>,
    val sanitizedBody: String?
)

data class ModelInfo(
    val id: String,
    val displayName: String = id
)

data class ChatRequestParams(
    val modelId: String,
    val messages: List<ChatMessage>,
    val systemPrompt: String,
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int?,
    val unlimitedTokens: Boolean
)

sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data object Completed : StreamEvent()
    data class FallbackNotice(val message: String) : StreamEvent()
    data class Error(val message: String, val httpStatus: Int? = null) : StreamEvent()
}

fun ProviderType.defaultBaseUrl(): String = when (this) {
    ProviderType.ANTHROPIC -> "https://api.anthropic.com"
    ProviderType.OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
}
