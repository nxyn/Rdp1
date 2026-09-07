package com.nxyn.aiclient.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nxyn.aiclient.domain.model.ConnectionResult
import com.nxyn.aiclient.domain.model.ModelInfo
import com.nxyn.aiclient.domain.model.ProviderSettings
import com.nxyn.aiclient.domain.model.ProviderType
import com.nxyn.aiclient.domain.model.ThemeMode
import com.nxyn.aiclient.domain.model.defaultBaseUrl
import com.nxyn.aiclient.domain.repository.ConversationRepository
import com.nxyn.aiclient.domain.repository.SettingsRepository
import com.nxyn.aiclient.util.UrlHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: ProviderSettings = ProviderSettings(),
    val apiKey: String = "",
    val apiKeyVisible: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isFetchingModels: Boolean = false,
    val connectionResult: ConnectionResult? = null,
    val models: List<ModelInfo> = emptyList(),
    val modelsError: String? = null,
    val showDiagnostics: Boolean = false,
    val showHttpWarning: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProviderSettings())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        showHttpWarning = UrlHelper.isHttp(settings.baseUrl)
                    )
                }
            }
        }
        _uiState.update { it.copy(apiKey = settingsRepository.getApiKey()) }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(apiKeyVisible = !it.apiKeyVisible) }
    }

    fun saveApiKey() {
        viewModelScope.launch {
            settingsRepository.setApiKey(_uiState.value.apiKey.trim())
        }
    }

    fun updateProvider(type: ProviderType) {
        viewModelScope.launch {
            settingsRepository.updateSettings { current ->
                current.copy(
                    providerType = type,
                    baseUrl = type.defaultBaseUrl()
                )
            }
        }
    }

    fun updateBaseUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(baseUrl = url) }
        }
    }

    fun updateModel(modelId: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(modelId = modelId, useCustomModel = false) }
        }
    }

    fun toggleCustomModel(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(useCustomModel = enabled) }
        }
    }

    fun updateCustomModelId(value: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(customModelId = value) }
        }
    }

    fun updateTemperature(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(temperature = value) }
        }
    }

    fun updateTopP(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(topP = value) }
        }
    }

    fun updateMaxTokens(value: Int?) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(maxTokens = value) }
        }
    }

    fun toggleUnlimitedTokens(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(unlimitedTokens = enabled) }
        }
    }

    fun updateSystemPrompt(value: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(systemPrompt = value) }
        }
    }

    fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(themeMode = themeMode) }
        }
    }

    fun toggleAnimations(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(animationsEnabled = enabled) }
        }
    }

    fun toggleDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(debugMode = enabled) }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            settingsRepository.setApiKey(_uiState.value.apiKey.trim())
            settingsRepository.updateSettings { it.copy(setupCompleted = true) }
        }
    }

    fun skipSetup() {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(setupCompleted = true) }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            settingsRepository.setApiKey(_uiState.value.apiKey.trim())
            _uiState.update { it.copy(isTestingConnection = true, connectionResult = null) }
            val result = settingsRepository.testConnection(_uiState.value.settings)
            _uiState.update { it.copy(isTestingConnection = false, connectionResult = result, showDiagnostics = true) }
        }
    }

    fun fetchModels() {
        viewModelScope.launch {
            settingsRepository.setApiKey(_uiState.value.apiKey.trim())
            _uiState.update { it.copy(isFetchingModels = true, modelsError = null) }
            settingsRepository.fetchModels(_uiState.value.settings)
                .onSuccess { models ->
                    _uiState.update { it.copy(isFetchingModels = false, models = models) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isFetchingModels = false,
                            modelsError = error.message ?: "Failed to fetch models"
                        )
                    }
                }
        }
    }

    fun clearAllChats(onCleared: () -> Unit) {
        viewModelScope.launch {
            conversationRepository.clearAll()
            onCleared()
        }
    }

    fun dismissDiagnostics() {
        _uiState.update { it.copy(showDiagnostics = false) }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsRepository, conversationRepository) as T
    }
}
