package com.nxyn.aiclient.util

import com.nxyn.aiclient.domain.model.ChatMessage
import com.nxyn.aiclient.domain.model.Conversation
import com.nxyn.aiclient.domain.model.MessageRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {
    private val json = Json { prettyPrint = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun toMarkdown(conversation: Conversation, messages: List<ChatMessage>): String {
        val builder = StringBuilder()
        builder.appendLine("# ${conversation.title}")
        builder.appendLine()
        builder.appendLine("- Provider: ${conversation.providerType.displayName}")
        builder.appendLine("- Model: ${conversation.modelId}")
        builder.appendLine("- Exported: ${dateFormat.format(Date())}")
        builder.appendLine()
        messages.forEach { message ->
            val label = when (message.role) {
                MessageRole.USER -> "You"
                MessageRole.ASSISTANT -> "Assistant"
                MessageRole.SYSTEM -> "System"
            }
            builder.appendLine("## $label")
            builder.appendLine()
            builder.appendLine(message.content)
            builder.appendLine()
        }
        return builder.toString().trim()
    }

    fun toJson(conversation: Conversation, messages: List<ChatMessage>): String {
        val payload = buildJsonObject {
            put("title", conversation.title)
            put("provider", conversation.providerType.name)
            put("model", conversation.modelId)
            put("timestamp", conversation.updatedAt)
            put(
                "messages",
                buildJsonArray {
                    messages.forEach { message ->
                        add(
                            buildJsonObject {
                                put("role", message.role.name.lowercase())
                                put("content", message.content)
                                put("timestamp", message.createdAt)
                            }
                        )
                    }
                }
            )
        }
        return json.encodeToString(payload)
    }
}
