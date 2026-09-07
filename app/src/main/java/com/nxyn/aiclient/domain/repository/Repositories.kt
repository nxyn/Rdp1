package com.nxyn.aiclient.domain.repository

import com.nxyn.aiclient.data.api.ProviderFactory
import com.nxyn.aiclient.data.database.AppDatabase
import com.nxyn.aiclient.data.database.toDomain
import com.nxyn.aiclient.data.database.toEntity
import com.nxyn.aiclient.data.preferences.SecureApiKeyStorage
import com.nxyn.aiclient.data.preferences.SettingsDataStore
import com.nxyn.aiclient.domain.model.ChatMessage
import com.nxyn.aiclient.domain.model.ChatRequestParams
import com.nxyn.aiclient.domain.model.ConnectionResult
import com.nxyn.aiclient.domain.model.Conversation
import com.nxyn.aiclient.domain.model.MessageRole
import com.nxyn.aiclient.domain.model.ModelInfo
import com.nxyn.aiclient.domain.model.ProviderSettings
import com.nxyn.aiclient.domain.model.StreamEvent
import com.nxyn.aiclient.domain.model.defaultBaseUrl
import com.nxyn.aiclient.util.UrlHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDataStore: SettingsDataStore,
    private val secureApiKeyStorage: SecureApiKeyStorage
) {
    val settings: Flow<ProviderSettings> = settingsDataStore.settings

    fun getApiKey(): String = secureApiKeyStorage.getApiKey()

    suspend fun updateSettings(transform: (ProviderSettings) -> ProviderSettings) {
        settingsDataStore.update(transform)
    }

    suspend fun setApiKey(apiKey: String) {
        secureApiKeyStorage.setApiKey(apiKey)
    }

    suspend fun testConnection(settings: ProviderSettings): ConnectionResult {
        if (!UrlHelper.isValidUrl(settings.baseUrl)) {
            return ConnectionResult(
                success = false,
                message = "Invalid URL",
                possibleFix = "Enter a valid http or https URL."
            )
        }
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return ConnectionResult(
                success = false,
                message = "Missing API key",
                possibleFix = "Enter your API key."
            )
        }
        return ProviderFactory.create(settings.providerType)
            .testConnection(settings.baseUrl, apiKey)
    }

    suspend fun fetchModels(settings: ProviderSettings): Result<List<ModelInfo>> {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API key is required"))
        }
        return ProviderFactory.create(settings.providerType)
            .fetchModels(settings.baseUrl, apiKey)
    }
}

class ConversationRepository(
    private val database: AppDatabase
) {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()

    fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        messageDao.observeForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun getConversation(id: Long): Conversation? =
        conversationDao.getById(id)?.toDomain()

    suspend fun createConversation(settings: ProviderSettings): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            Conversation(
                title = "New Chat",
                providerType = settings.providerType,
                modelId = settings.effectiveModelId,
                createdAt = now,
                updatedAt = now
            ).toEntity()
        )
    }

    suspend fun renameConversation(id: Long, title: String) {
        val current = conversationDao.getById(id) ?: return
        conversationDao.update(current.copy(title = title.trim(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    suspend fun clearAll() {
        conversationDao.deleteAll()
    }

    suspend fun addMessage(message: ChatMessage): Long {
        val id = messageDao.insert(message.toEntity())
        conversationDao.getById(message.conversationId)?.let { conversation ->
            conversationDao.update(conversation.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun updateMessage(message: ChatMessage) {
        messageDao.update(message.toEntity())
    }

    suspend fun updateConversationModel(conversationId: Long, modelId: String) {
        conversationDao.getById(conversationId)?.let { conversation ->
            conversationDao.update(conversation.copy(modelId = modelId, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun getMessages(conversationId: Long): List<ChatMessage> =
        messageDao.getForConversation(conversationId).map { it.toDomain() }

    suspend fun ensureTitleFromFirstMessage(conversationId: Long, content: String) {
        val conversation = conversationDao.getById(conversationId) ?: return
        if (conversation.title == "New Chat") {
            val title = content.lineSequence().firstOrNull()?.take(48)?.trim().orEmpty()
            if (title.isNotBlank()) {
                conversationDao.update(conversation.copy(title = title, updatedAt = System.currentTimeMillis()))
            }
        }
    }
}

class ChatRepository(
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository
) {
    fun streamResponse(
        settings: ProviderSettings,
        conversationId: Long,
        history: List<ChatMessage>
    ): Flow<StreamEvent> {
        val apiKey = settingsRepository.getApiKey()
        val params = ChatRequestParams(
            modelId = settings.effectiveModelId,
            messages = history,
            systemPrompt = settings.systemPrompt,
            temperature = settings.temperature,
            topP = settings.topP,
            maxTokens = settings.maxTokens,
            unlimitedTokens = settings.unlimitedTokens
        )
        return ProviderFactory.create(settings.providerType)
            .streamMessage(settings.baseUrl, apiKey, params)
    }

    suspend fun sendMessage(
        settings: ProviderSettings,
        history: List<ChatMessage>
    ): Result<String> {
        val apiKey = settingsRepository.getApiKey()
        val params = ChatRequestParams(
            modelId = settings.effectiveModelId,
            messages = history,
            systemPrompt = settings.systemPrompt,
            temperature = settings.temperature,
            topP = settings.topP,
            maxTokens = settings.maxTokens,
            unlimitedTokens = settings.unlimitedTokens
        )
        return ProviderFactory.create(settings.providerType)
            .sendMessage(settings.baseUrl, apiKey, params)
    }
}
