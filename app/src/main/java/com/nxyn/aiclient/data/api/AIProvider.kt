package com.nxyn.aiclient.data.api

import com.nxyn.aiclient.domain.model.ChatRequestParams
import com.nxyn.aiclient.domain.model.ConnectionResult
import com.nxyn.aiclient.domain.model.ModelInfo
import com.nxyn.aiclient.domain.model.StreamEvent
import kotlinx.coroutines.flow.Flow

interface AIProvider {
    suspend fun testConnection(baseUrl: String, apiKey: String): ConnectionResult
    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<ModelInfo>>
    suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        params: ChatRequestParams
    ): Result<String>
    fun streamMessage(
        baseUrl: String,
        apiKey: String,
        params: ChatRequestParams
    ): Flow<StreamEvent>
}
