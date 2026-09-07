package com.nxyn.aiclient.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nxyn.aiclient.domain.model.ProviderType
import com.nxyn.aiclient.ui.settings.SettingsUiState
import com.nxyn.aiclient.ui.theme.AppGradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    uiState: SettingsUiState,
    onProviderChange: (ProviderType) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val settings = uiState.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppGradients.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.height(16.dp))
        Text("✦ AI Client", style = MaterialTheme.typography.headlineSmall)
        Text("Connect your AI provider.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Text("Provider", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
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
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = settings.baseUrl,
            onValueChange = onBaseUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Base URL") },
            singleLine = true
        )
        OutlinedTextField(
            value = uiState.apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onTestConnection,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isTestingConnection
        ) {
            if (uiState.isTestingConnection) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Test Connection")
            }
        }
        uiState.connectionResult?.let { result ->
            Text(
                text = if (result.success) "✓ ${result.message}" else "✕ ${result.message}",
                color = if (result.success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }
    }
}
