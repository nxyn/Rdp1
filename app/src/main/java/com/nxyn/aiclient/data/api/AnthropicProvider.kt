package com.nxyn.aiclient.data.api

import com.nxyn.aiclient.domain.model.ChatRequestParams
import com.nxyn.aiclient.domain.model.ConnectionResult
import com.nxyn.aiclient.domain.model.MessageRole
import com.nxyn.aiclient.domain.model.ModelInfo
import com.nxyn.aiclient.domain.model.ProviderType
import com.nxyn.aiclient.domain.model.StreamEvent
import com.nxyn.aiclient.util.ErrorMapper
import com.nxyn.aiclient.util.SseParser
import com.nxyn.aiclient.util.UrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AnthropicProvider(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AIProvider {

    private companion object {
        const val ANTHROPIC_VERSION = "2023-06-01"
        const val DEFAULT_MAX_TOKENS = 4096
    }

    override suspend fun testConnection(baseUrl: String, apiKey: String): ConnectionResult =
        withContext(Dispatchers.IO) {
            val url = UrlHelper.endpoint(baseUrl, ProviderType.ANTHROPIC, "models")
            executeLightweightGet(url, apiKey)
        }

    override suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<ModelInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = UrlHelper.endpoint(baseUrl, ProviderType.ANTHROPIC, "models")
                val request = buildGetRequest(url, apiKey)
                NetworkClient.client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IllegalStateException(ErrorMapper.mapHttpError(response.code, body).message)
                    }
                    parseModels(body)
                }
            }
        }

    override suspend fun sendMessage(
        baseUrl: String,
        apiKey: String,
        params: ChatRequestParams
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = UrlHelper.endpoint(baseUrl, ProviderType.ANTHROPIC, "messages")
            val payload = buildPayload(params, stream = false)
            val request = buildPostRequest(url, apiKey, payload)
            NetworkClient.client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(ErrorMapper.mapHttpError(response.code, body).message)
                }
                parseCompletion(body)
            }
        }
    }

    override fun streamMessage(
        baseUrl: String,
        apiKey: String,
        params: ChatRequestParams
    ): Flow<StreamEvent> = flow {
        val url = UrlHelper.endpoint(baseUrl, ProviderType.ANTHROPIC, "messages")
        val payload = buildPayload(params, stream = true)
        val request = buildPostRequest(url, apiKey, payload)
        val call = NetworkClient.client.newCall(request)
        var receivedContent = false

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    emit(StreamEvent.Error(ErrorMapper.mapHttpError(response.code, body).message, response.code))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(StreamEvent.Error("Empty response body"))
                    return@flow
                }

                body.source().buffer().use { source ->
                    SseParser.parse(source).collect { data ->
                        val delta = parseStreamEvent(data)
                        if (delta.isNotEmpty()) {
                            receivedContent = true
                            emit(StreamEvent.Delta(delta))
                        }
                    }
                }
                emit(StreamEvent.Completed)
            }
        } catch (e: Exception) {
            if (call.isCanceled()) {
                emit(StreamEvent.Completed)
            } else if (!receivedContent) {
                emit(StreamEvent.FallbackNotice("Streaming unavailable. Using standard response..."))
                val fallback = sendMessage(baseUrl, apiKey, params)
                fallback.fold(
                    onSuccess = {
                        emit(StreamEvent.Delta(it))
                        emit(StreamEvent.Completed)
                    },
                    onFailure = {
                        emit(StreamEvent.Error(it.message ?: "Request failed"))
                    }
                )
            } else {
                emit(StreamEvent.Error(e.message ?: "Streaming failed"))
            }
        }
    }.catch { e ->
        emit(StreamEvent.Error(e.message ?: "Streaming failed"))
    }.flowOn(Dispatchers.IO)

    private fun executeLightweightGet(url: String, apiKey: String): ConnectionResult {
        val request = buildGetRequest(url, apiKey)
        val start = System.currentTimeMillis()
        return try {
            NetworkClient.client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - start
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ConnectionResult(
                        success = true,
                        httpStatus = response.code,
                        message = "Connection successful",
                        responseTimeMs = elapsed
                    )
                } else {
                    ErrorMapper.mapHttpError(response.code, body).copy(responseTimeMs = elapsed)
                }
            }
        } catch (e: Exception) {
            ErrorMapper.mapNetworkError(e)
        }
    }

    private fun buildGetRequest(url: String, apiKey: String): Request =
        Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .get()
            .build()

    private fun buildPostRequest(url: String, apiKey: String, payload: String): Request =
        Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

    private fun resolveMaxTokens(params: ChatRequestParams): Int {
        return when {
            !params.unlimitedTokens && params.maxTokens != null -> params.maxTokens
            else -> DEFAULT_MAX_TOKENS
        }
    }

    private fun buildPayload(params: ChatRequestParams, stream: Boolean): String {
        val messages = buildJsonArray {
            params.messages.filter { it.role != MessageRole.SYSTEM }.forEach { message ->
                add(
                    buildJsonObject {
                        put("role", if (message.role == MessageRole.USER) "user" else "assistant")
                        put(
                            "content",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("type", "text")
                                        put("text", message.content)
                                    }
                                )
                            }
                        )
                    }
                )
            }
        }

        return buildJsonObject {
            put("model", params.modelId)
            put("messages", messages)
            put("max_tokens", resolveMaxTokens(params))
            put("temperature", params.temperature)
            put("top_p", params.topP)
            put("stream", stream)
            if (params.systemPrompt.isNotBlank()) {
                put("system", params.systemPrompt)
            }
        }.toString()
    }

    private fun parseModels(body: String): List<ModelInfo> {
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"]?.jsonArray ?: return emptyList()
        return data.mapNotNull { item ->
            val id = item.jsonObject["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            ModelInfo(id = id)
        }.sortedBy { it.id }
    }

    private fun parseCompletion(body: String): String {
        val root = json.parseToJsonElement(body).jsonObject
        return root["content"]?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()
    }

    private fun parseStreamEvent(data: String): String {
        return runCatching {
            val root = json.parseToJsonElement(data).jsonObject
            when (root["type"]?.jsonPrimitive?.contentOrNull) {
                "content_block_delta" -> root["delta"]?.jsonObject
                    ?.get("text")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    .orEmpty()
                else -> ""
            }
        }.getOrDefault("")
    }
}
