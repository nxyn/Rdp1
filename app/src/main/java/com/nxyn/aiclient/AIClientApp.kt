package com.nxyn.aiclient

import android.app.Application
import com.nxyn.aiclient.data.database.AppDatabase
import com.nxyn.aiclient.data.preferences.SecureApiKeyStorage
import com.nxyn.aiclient.data.preferences.SettingsDataStore
import com.nxyn.aiclient.domain.repository.ChatRepository
import com.nxyn.aiclient.domain.repository.ConversationRepository
import com.nxyn.aiclient.domain.repository.SettingsRepository

class AIClientApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val database = AppDatabase.get(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val secureApiKeyStorage = SecureApiKeyStorage(application)

    val settingsRepository = SettingsRepository(settingsDataStore, secureApiKeyStorage)
    val conversationRepository = ConversationRepository(database)
    val chatRepository = ChatRepository(settingsRepository, conversationRepository)
}
