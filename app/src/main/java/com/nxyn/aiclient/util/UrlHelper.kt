package com.nxyn.aiclient.util

import com.nxyn.aiclient.domain.model.ProviderType
import com.nxyn.aiclient.domain.model.defaultBaseUrl
import java.net.URI

object UrlHelper {
    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return trimmed.ifBlank { ProviderType.OPENAI_COMPATIBLE.defaultBaseUrl() }
    }

    fun buildUrl(baseUrl: String, path: String): String {
        val normalizedBase = normalizeBaseUrl(baseUrl)
        val cleanPath = path.trim().trimStart('/')
        return "$normalizedBase/$cleanPath"
    }

    fun endpoint(baseUrl: String, providerType: ProviderType, resource: String): String {
        val normalized = normalizeBaseUrl(baseUrl)
        return when (providerType) {
            ProviderType.ANTHROPIC -> when (resource) {
                "models" -> buildAnthropicUrl(normalized, "v1/models")
                "messages" -> buildAnthropicUrl(normalized, "v1/messages")
                else -> buildAnthropicUrl(normalized, resource)
            }
            ProviderType.OPENAI_COMPATIBLE -> when (resource) {
                "models" -> buildOpenAiUrl(normalized, "models")
                "chat" -> buildOpenAiUrl(normalized, "chat/completions")
                else -> buildOpenAiUrl(normalized, resource)
            }
        }
    }

    private fun buildAnthropicUrl(base: String, path: String): String {
        val lower = base.lowercase()
        return if (lower.endsWith("/v1")) {
            "$base/${path.removePrefix("v1/")}"
        } else {
            buildUrl(base, path)
        }
    }

    private fun buildOpenAiUrl(base: String, path: String): String {
        val lower = base.lowercase()
        return if (lower.endsWith("/v1")) {
            "$base/$path"
        } else {
            buildUrl(base, "v1/$path")
        }
    }

    fun isValidUrl(url: String): Boolean {
        return runCatching {
            val uri = URI(normalizeBaseUrl(url))
            uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }

    fun isHttp(url: String): Boolean {
        return normalizeBaseUrl(url).startsWith("http://", ignoreCase = true)
    }

    fun redactSecrets(text: String, secrets: List<String>): String {
        var result = text
        secrets.filter { it.isNotBlank() }.forEach { secret ->
            result = result.replace(secret, "••••••••")
        }
        return result
    }
}
