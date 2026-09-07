package com.nxyn.aiclient.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nxyn.aiclient.domain.model.MessageRole
import com.nxyn.aiclient.domain.model.ProviderType

@Entity(
    tableName = "conversations",
    indices = [Index("updatedAt")]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val providerType: String,
    val modelId: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId"), Index("createdAt")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val createdAt: Long,
    val usedFallback: Boolean = false
)

fun ConversationEntity.toDomain() = com.nxyn.aiclient.domain.model.Conversation(
    id = id,
    title = title,
    providerType = ProviderType.fromRaw(providerType),
    modelId = modelId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun com.nxyn.aiclient.domain.model.Conversation.toEntity() = ConversationEntity(
    id = id,
    title = title,
    providerType = providerType.name,
    modelId = modelId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MessageEntity.toDomain(isStreaming: Boolean = false) = com.nxyn.aiclient.domain.model.ChatMessage(
    id = id,
    conversationId = conversationId,
    role = MessageRole.valueOf(role),
    content = content,
    createdAt = createdAt,
    isStreaming = isStreaming,
    usedFallback = usedFallback
)

fun com.nxyn.aiclient.domain.model.ChatMessage.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    createdAt = createdAt,
    usedFallback = usedFallback
)
