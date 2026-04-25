package com.libreintel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.libreintel.domain.model.ChatMessage
import com.libreintel.domain.model.MessageRole

/**
 * Room entity for tree nodes.
 * Stores all node data including chat history.
 */
@Entity(
    tableName = "tree_nodes",
    indices = [Index(value = ["parentId"])]
)
@TypeConverters(Converters::class)
data class TreeNodeEntity(
    @PrimaryKey
    val id: String,
    val parentId: String?,
    val title: String,
    val fullText: String,
    val sourceUrl: String?,
    val timestamp: Long,
    val lastAccessedAt: Long?,
    val chatHistoryJson: String // Stored as JSON string
)

/**
 * Type converters for Room database.
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun chatMessagesToJson(messages: List<ChatMessage>): String {
        return gson.toJson(messages)
    }
    
    @TypeConverter
    fun jsonToChatMessages(json: String): List<ChatMessage> {
        return try {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Extension functions to convert between domain models and entities.
 */
fun TreeNodeEntity.toDomain(): com.libreintel.domain.model.TreeNode {
    val gson = Gson()
    val type = object : TypeToken<List<ChatMessage>>() {}.type
    val chatHistory: List<ChatMessage> = try {
        gson.fromJson(chatHistoryJson, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    
    return com.libreintel.domain.model.TreeNode(
        id = id,
        parentId = parentId,
        title = title,
        fullText = fullText,
        sourceUrl = sourceUrl,
        timestamp = java.util.Date(timestamp),
        lastAccessedAt = lastAccessedAt?.let { java.util.Date(it) },
        chatHistory = chatHistory
    )
}

fun com.libreintel.domain.model.TreeNode.toEntity(): TreeNodeEntity {
    val gson = Gson()
    return TreeNodeEntity(
        id = id,
        parentId = parentId,
        title = title,
        fullText = fullText,
        sourceUrl = sourceUrl,
        timestamp = timestamp.time,
        lastAccessedAt = lastAccessedAt?.time,
        chatHistoryJson = gson.toJson(chatHistory)
    )
}