package com.nxyn.aiclient.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nxyn.aiclient.domain.model.ChatMessage
import com.nxyn.aiclient.domain.model.Conversation
import com.nxyn.aiclient.domain.model.MessageRole
import com.nxyn.aiclient.domain.model.ProviderSettings
import com.nxyn.aiclient.domain.model.StreamEvent
import com.nxyn.aiclient.domain.repository.ChatRepository
import com.nxyn.aiclient.domain.repository.ConversationRepository
import com.nxyn.aiclient.domain.repository.SettingsRepository
import com.nxyn.aiclient.util.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversationId: Long? = null,
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val settings: ProviderSettings = ProviderSettings(),
    val composerText: String = "",
    val isGenerating: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isOnline: Boolean = true
)

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository,
    private val isOnlineProvider: () -> Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val conversations = conversationRepository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var streamJob: Job? = null
    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings, isOnline = isOnlineProvider()) }
            }
        }
    }

    fun selectConversation(conversationId: Long) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            val conversation = conversationRepository.getConversation(conversationId)
            conversationRepository.observeMessages(conversationId).collect { messages ->
                _uiState.update {
                    it.copy(
                        conversationId = conversationId,
                        conversation = conversation,
                        messages = messages,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun createNewConversation(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val settings = _uiState.value.settings
            val id = conversationRepository.createConversation(settings)
            onCreated(id)
            selectConversation(id)
        }
    }

    fun updateComposer(text: String) {
        _uiState.update { it.copy(composerText = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.composerText.trim()
        if (text.isBlank() || state.isGenerating) return
        if (!isOnlineProvider()) {
            _uiState.update { it.copy(errorMessage = "No internet connection. Reconnect and try again.") }
            return
        }
        if (state.settings.effectiveModelId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Select or enter a model ID in Settings.") }
            return
        }
        if (settingsRepository.getApiKey().isBlank()) {
            _uiState.update { it.copy(errorMessage = "Add your API key in Settings.") }
            return
        }

        viewModelScope.launch {
            val conversationId = state.conversationId ?: run {
                val id = conversationRepository.createConversation(state.settings)
                selectConversation(id)
                id
            }

            val userMessage = ChatMessage(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = text
            )
            val userId = conversationRepository.addMessage(userMessage)
            conversationRepository.ensureTitleFromFirstMessage(conversationId, text)
            conversationRepository.updateConversationModel(conversationId, state.settings.effectiveModelId)

            val assistantMessage = ChatMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = "",
                isStreaming = true
            )
            val assistantId = conversationRepository.addMessage(assistantMessage)

            _uiState.update {
                it.copy(
                    composerText = "",
                    isGenerating = true,
                    statusMessage = "✦ Thinking...",
                    errorMessage = null
                )
            }

            val currentHistory = state.messages + userMessage.copy(id = userId)

            streamJob = viewModelScope.launch {
                var content = ""
                var usedFallback = false
                chatRepository.streamResponse(state.settings, conversationId, currentHistory)
                    .collect { event ->
                        when (event) {
                            is StreamEvent.Delta -> {
                                content += event.text
                                conversationRepository.updateMessage(
                                    assistantMessage.copy(id = assistantId, content = content, isStreaming = true)
                                )
                                _uiState.update { it.copy(statusMessage = null) }
                            }
                            is StreamEvent.FallbackNotice -> {
                                usedFallback = true
                                _uiState.update { it.copy(statusMessage = event.message) }
                            }
                            is StreamEvent.Error -> {
                                _uiState.update {
                                    it.copy(
                                        errorMessage = event.message,
                                        isGenerating = false,
                                        statusMessage = null
                                    )
                                }
                            }
                            StreamEvent.Completed -> {
                                conversationRepository.updateMessage(
                                    assistantMessage.copy(
                                        id = assistantId,
                                        content = content,
                                        isStreaming = false,
                                        usedFallback = usedFallback
                                    )
                                )
                                _uiState.update {
                                    it.copy(isGenerating = false, statusMessage = null)
                                }
                            }
                        }
                    }
            }
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { it.copy(isGenerating = false, statusMessage = null) }
    }

    fun renameConversation(id: Long, title: String) {
        viewModelScope.launch {
            conversationRepository.renameConversation(id, title)
        }
    }

    fun deleteConversation(id: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
            if (_uiState.value.conversationId == id) {
                _uiState.value = ChatUiState(settings = _uiState.value.settings)
            }
            onDeleted()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class ChatViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository,
    private val chatRepository: ChatRepository,
    private val isOnlineProvider: () -> Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(
            settingsRepository,
            conversationRepository,
            chatRepository,
            isOnlineProvider
        ) as T
    }
}
