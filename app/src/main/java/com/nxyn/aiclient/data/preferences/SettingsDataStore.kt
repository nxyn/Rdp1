package com.nxyn.aiclient.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nxyn.aiclient.domain.model.ProviderSettings
import com.nxyn.aiclient.domain.model.ProviderType
import com.nxyn.aiclient.domain.model.ThemeMode
import com.nxyn.aiclient.domain.model.defaultBaseUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val PROVIDER = stringPreferencesKey("provider")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL_ID = stringPreferencesKey("model_id")
        val USE_CUSTOM_MODEL = booleanPreferencesKey("use_custom_model")
        val CUSTOM_MODEL_ID = stringPreferencesKey("custom_model_id")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_P = floatPreferencesKey("top_p")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
        val UNLIMITED_TOKENS = booleanPreferencesKey("unlimited_tokens")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val THEME = stringPreferencesKey("theme")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val DEBUG = booleanPreferencesKey("debug")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    val settings: Flow<ProviderSettings> = context.dataStore.data.map { prefs ->
        val provider = ProviderType.fromRaw(prefs[Keys.PROVIDER] ?: ProviderType.OPENAI_COMPATIBLE.name)
        ProviderSettings(
            providerType = provider,
            baseUrl = prefs[Keys.BASE_URL] ?: provider.defaultBaseUrl(),
            modelId = prefs[Keys.MODEL_ID] ?: "",
            useCustomModel = prefs[Keys.USE_CUSTOM_MODEL] ?: false,
            customModelId = prefs[Keys.CUSTOM_MODEL_ID] ?: "",
            temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
            topP = prefs[Keys.TOP_P] ?: 1.0f,
            maxTokens = prefs[Keys.MAX_TOKENS],
            unlimitedTokens = prefs[Keys.UNLIMITED_TOKENS] ?: true,
            systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: "You are a helpful AI assistant.",
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.DARK.name),
            animationsEnabled = prefs[Keys.ANIMATIONS] ?: true,
            debugMode = prefs[Keys.DEBUG] ?: false,
            setupCompleted = prefs[Keys.SETUP_COMPLETED] ?: false
        )
    }

    suspend fun update(transform: (ProviderSettings) -> ProviderSettings) {
        context.dataStore.edit { prefs ->
            val current = ProviderSettings(
                providerType = ProviderType.fromRaw(prefs[Keys.PROVIDER] ?: ProviderType.OPENAI_COMPATIBLE.name),
                baseUrl = prefs[Keys.BASE_URL] ?: ProviderType.OPENAI_COMPATIBLE.defaultBaseUrl(),
                modelId = prefs[Keys.MODEL_ID] ?: "",
                useCustomModel = prefs[Keys.USE_CUSTOM_MODEL] ?: false,
                customModelId = prefs[Keys.CUSTOM_MODEL_ID] ?: "",
                temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
                topP = prefs[Keys.TOP_P] ?: 1.0f,
                maxTokens = prefs[Keys.MAX_TOKENS],
                unlimitedTokens = prefs[Keys.UNLIMITED_TOKENS] ?: true,
                systemPrompt = prefs[Keys.SYSTEM_PROMPT] ?: "You are a helpful AI assistant.",
                themeMode = ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.DARK.name),
                animationsEnabled = prefs[Keys.ANIMATIONS] ?: true,
                debugMode = prefs[Keys.DEBUG] ?: false,
                setupCompleted = prefs[Keys.SETUP_COMPLETED] ?: false
            )
            val updated = transform(current)
            prefs[Keys.PROVIDER] = updated.providerType.name
            prefs[Keys.BASE_URL] = updated.baseUrl
            prefs[Keys.MODEL_ID] = updated.modelId
            prefs[Keys.USE_CUSTOM_MODEL] = updated.useCustomModel
            prefs[Keys.CUSTOM_MODEL_ID] = updated.customModelId
            prefs[Keys.TEMPERATURE] = updated.temperature
            prefs[Keys.TOP_P] = updated.topP
            updated.maxTokens?.let { prefs[Keys.MAX_TOKENS] = it }
            prefs[Keys.UNLIMITED_TOKENS] = updated.unlimitedTokens
            prefs[Keys.SYSTEM_PROMPT] = updated.systemPrompt
            prefs[Keys.THEME] = updated.themeMode.name
            prefs[Keys.ANIMATIONS] = updated.animationsEnabled
            prefs[Keys.DEBUG] = updated.debugMode
            prefs[Keys.SETUP_COMPLETED] = updated.setupCompleted
        }
    }
}
