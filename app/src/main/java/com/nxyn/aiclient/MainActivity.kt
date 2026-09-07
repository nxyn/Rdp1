package com.nxyn.aiclient

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nxyn.aiclient.domain.model.Conversation
import com.nxyn.aiclient.ui.chat.ChatScreen
import com.nxyn.aiclient.ui.chat.ChatViewModel
import com.nxyn.aiclient.ui.chat.ChatViewModelFactory
import com.nxyn.aiclient.ui.settings.SettingsScreen
import com.nxyn.aiclient.ui.settings.SettingsViewModel
import com.nxyn.aiclient.ui.settings.SettingsViewModelFactory
import com.nxyn.aiclient.ui.setup.SetupScreen
import com.nxyn.aiclient.ui.theme.AIClientTheme
import com.nxyn.aiclient.util.ExportHelper
import com.nxyn.aiclient.util.NetworkMonitor
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AIClientApp).container

        setContent {
            val settings by container.settingsRepository.settings.collectAsState(
                initial = com.nxyn.aiclient.domain.model.ProviderSettings()
            )

            AIClientTheme(themeMode = settings.themeMode) {
                val navController = rememberNavController()
                val chatViewModel: ChatViewModel = viewModel(
                    factory = ChatViewModelFactory(
                        container.settingsRepository,
                        container.conversationRepository,
                        container.chatRepository
                    ) { NetworkMonitor.isOnline(this@MainActivity) }
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(
                        container.settingsRepository,
                        container.conversationRepository
                    )
                )

                val chatUiState by chatViewModel.uiState.collectAsState()
                val conversations by chatViewModel.conversations.collectAsState()
                val settingsUiState by settingsViewModel.uiState.collectAsState()

                val startDestination = if (settings.setupCompleted) "chat" else "setup"

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("setup") {
                        SetupScreen(
                            uiState = settingsUiState,
                            onProviderChange = settingsViewModel::updateProvider,
                            onBaseUrlChange = settingsViewModel::updateBaseUrl,
                            onApiKeyChange = settingsViewModel::updateApiKey,
                            onTestConnection = settingsViewModel::testConnection,
                            onContinue = {
                                settingsViewModel.completeSetup()
                                navController.navigate("chat") {
                                    popUpTo("setup") { inclusive = true }
                                }
                            },
                            onSkip = {
                                settingsViewModel.skipSetup()
                                navController.navigate("chat") {
                                    popUpTo("setup") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("chat") {
                        ChatScreen(
                            uiState = chatUiState,
                            conversations = conversations,
                            onOpenSettings = { navController.navigate("settings") },
                            onSelectConversation = chatViewModel::selectConversation,
                            onNewChat = {
                                chatViewModel.createNewConversation { id ->
                                    chatViewModel.selectConversation(id)
                                }
                            },
                            onComposerChange = chatViewModel::updateComposer,
                            onSend = chatViewModel::sendMessage,
                            onStop = chatViewModel::stopGeneration,
                            onRenameConversation = chatViewModel::renameConversation,
                            onDeleteConversation = { id ->
                                chatViewModel.deleteConversation(id) {}
                            },
                            onExport = { conversation -> exportConversation(conversation) }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            uiState = settingsUiState,
                            onBack = { navController.popBackStack() },
                            onProviderChange = settingsViewModel::updateProvider,
                            onBaseUrlChange = settingsViewModel::updateBaseUrl,
                            onApiKeyChange = settingsViewModel::updateApiKey,
                            onToggleApiKeyVisibility = settingsViewModel::toggleApiKeyVisibility,
                            onSaveApiKey = settingsViewModel::saveApiKey,
                            onTestConnection = settingsViewModel::testConnection,
                            onFetchModels = settingsViewModel::fetchModels,
                            onModelSelected = settingsViewModel::updateModel,
                            onToggleCustomModel = settingsViewModel::toggleCustomModel,
                            onCustomModelChange = settingsViewModel::updateCustomModelId,
                            onTemperatureChange = settingsViewModel::updateTemperature,
                            onTopPChange = settingsViewModel::updateTopP,
                            onMaxTokensChange = settingsViewModel::updateMaxTokens,
                            onToggleUnlimitedTokens = settingsViewModel::toggleUnlimitedTokens,
                            onSystemPromptChange = settingsViewModel::updateSystemPrompt,
                            onThemeChange = settingsViewModel::updateTheme,
                            onToggleAnimations = settingsViewModel::toggleAnimations,
                            onToggleDebugMode = settingsViewModel::toggleDebugMode,
                            onClearAllChats = {
                                settingsViewModel.clearAllChats {
                                    navController.navigate("chat") {
                                        popUpTo("chat") { inclusive = true }
                                    }
                                }
                            },
                            onDismissDiagnostics = settingsViewModel::dismissDiagnostics
                        )
                    }
                }
            }
        }
    }

    private fun exportConversation(conversation: Conversation) {
        val appContainer = (application as AIClientApp).container
        val markdown = runBlocking {
            val messages = appContainer.conversationRepository.getMessages(conversation.id)
            ExportHelper.toMarkdown(conversation, messages)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, conversation.title)
            putExtra(Intent.EXTRA_TEXT, markdown)
        }
        startActivity(Intent.createChooser(shareIntent, "Export Chat"))
    }
}
