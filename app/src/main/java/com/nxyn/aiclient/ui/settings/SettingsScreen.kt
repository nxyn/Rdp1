package com.nxyn.aiclient.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nxyn.aiclient.domain.model.ProviderType
import com.nxyn.aiclient.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onProviderChange: (ProviderType) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onSaveApiKey: () -> Unit,
    onTestConnection: () -> Unit,
    onFetchModels: () -> Unit,
    onModelSelected: (String) -> Unit,
    onToggleCustomModel: (Boolean) -> Unit,
    onCustomModelChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTopPChange: (Float) -> Unit,
    onMaxTokensChange: (Int?) -> Unit,
    onToggleUnlimitedTokens: (Boolean) -> Unit,
    onSystemPromptChange: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onToggleDebugMode: (Boolean) -> Unit,
    onClearAllChats: () -> Unit,
    onDismissDiagnostics: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("Connection") {
                Text("Provider", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ProviderType.entries.forEachIndexed { index, provider ->
                        SegmentedButton(
                            selected = settings.providerType == provider,
                            onClick = { onProviderChange(provider) },
                            shape = SegmentedButtonDefaults.itemShape(index, ProviderType.entries.size)
                        ) {
                            Text(provider.displayName)
                        }
                    }
                }
                OutlinedTextField(
                    value = settings.baseUrl,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true
                )
                if (uiState.showHttpWarning) {
                    Text(
                        "Warning: HTTP is insecure. API keys and conversation data may be transmitted without encryption.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = onApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    visualTransformation = if (uiState.apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onToggleApiKeyVisibility) {
                            Icon(
                                if (uiState.apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle API key visibility"
                            )
                        }
                    },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        onSaveApiKey()
                        onTestConnection()
                    }, enabled = !uiState.isTestingConnection) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp))
                        } else {
                            Text("Test Connection")
                        }
                    }
                }
                uiState.connectionResult?.let { result ->
                    Text(
                        text = if (result.success) "✓ ${result.message}" else "✕ ${result.message}",
                        color = if (result.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                    result.possibleFix?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SettingsSection("Models") {
                Button(onClick = onFetchModels, enabled = !uiState.isFetchingModels) {
                    Text(if (uiState.isFetchingModels) "Fetching models..." else "Fetch Models")
                }
                uiState.modelsError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                if (uiState.models.isNotEmpty()) {
                    Text("Selected Model", style = MaterialTheme.typography.labelLarge)
                    uiState.models.forEach { model ->
                        TextButton(onClick = { onModelSelected(model.id) }) {
                            Text(
                                text = if (settings.modelId == model.id) "• ${model.id}" else model.id,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = settings.useCustomModel, onCheckedChange = onToggleCustomModel)
                    Text("Use custom model")
                }
                if (settings.useCustomModel) {
                    OutlinedTextField(
                        value = settings.customModelId,
                        onValueChange = onCustomModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model ID") },
                        singleLine = true
                    )
                }
            }

            SettingsSection("Parameters") {
                Text("Temperature ${settings.temperature}")
                Slider(
                    value = settings.temperature,
                    onValueChange = onTemperatureChange,
                    valueRange = 0f..2f
                )
                Text("Top-p ${settings.topP}")
                Slider(
                    value = settings.topP,
                    onValueChange = onTopPChange,
                    valueRange = 0f..1f
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = settings.unlimitedTokens, onCheckedChange = onToggleUnlimitedTokens)
                    Text("Unlimited max tokens")
                }
                if (!settings.unlimitedTokens) {
                    OutlinedTextField(
                        value = settings.maxTokens?.toString().orEmpty(),
                        onValueChange = { value ->
                            onMaxTokensChange(value.toIntOrNull())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Max tokens") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = settings.systemPrompt,
                    onValueChange = onSystemPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("System prompt") },
                    minLines = 3
                )
            }

            SettingsSection("Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                ThemeMode.entries.forEach { theme ->
                    TextButton(onClick = { onThemeChange(theme) }) {
                        Text(if (settings.themeMode == theme) "• ${theme.name}" else theme.name)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Animations")
                    Switch(checked = settings.animationsEnabled, onCheckedChange = onToggleAnimations)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Debug mode")
                    Switch(checked = settings.debugMode, onCheckedChange = onToggleDebugMode)
                }
            }

            SettingsSection("Data") {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("Clear All Chats", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (uiState.showDiagnostics && uiState.connectionResult != null) {
        val result = uiState.connectionResult
        AlertDialog(
            onDismissRequest = onDismissDiagnostics,
            title = { Text("Connection Diagnostics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Endpoint\n${settings.baseUrl}")
                    Text("Provider\n${settings.providerType.displayName}")
                    Text("Result\n${if (result.success) "✓ Success" else "✕ Failed"}")
                    result.httpStatus?.let { Text("HTTP Status\n$it") }
                    Text("Message\n${result.message}")
                    result.possibleFix?.let { Text("Possible fix\n$it") }
                    result.responseTimeMs?.let { Text("Response time\n${it}ms") }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissDiagnostics) { Text("Close") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all chats?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAllChats()
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        content()
    }
}
