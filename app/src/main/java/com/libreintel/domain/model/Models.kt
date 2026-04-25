package com.libreintel.domain.model

import java.util.Date

/**
 * Represents a node in the exploration tree.
 * Corresponds to the node structure from sidebar.js
 */
data class TreeNode(
    val id: String = generateId(),
    val parentId: String? = null,
    val title: String = "",
    val fullText: String = "",
    val sourceUrl: String? = null,
    val timestamp: Date = Date(),
    val lastAccessedAt: Date? = null,
    val children: List<TreeNode> = emptyList(),
    val chatHistory: List<ChatMessage> = emptyList()
) {
    companion object {
        fun generateId(): String {
            return System.currentTimeMillis().toString(36) + 
                   java.util.UUID.randomUUID().toString().take(4)
        }
    }
}

/**
 * Represents a chat message within a node's conversation.
 */
data class ChatMessage(
    val id: String = TreeNode.generateId(),
    val role: MessageRole = MessageRole.USER,
    val content: String = "",
    val timestamp: Date = Date()
)

enum class MessageRole {
    USER, ASSISTANT
}

/**
 * LLM API configuration settings.
 */
data class LlmConfig(
    val url: String = "",
    val key: String = "",
    val model: String = ""
) {
    companion object {
        val DASHSCOPE_PRESET = LlmConfig(
            url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            model = "qwen-plus"
        )
        
        val OPENAI_PRESET = LlmConfig(
            url = "https://api.openai.com/v1/chat/completions",
            model = "gpt-4o-mini"
        )
        
        val OLLAMA_PRESET = LlmConfig(
            url = "http://localhost:11434/api/chat",
            model = "llama2"
        )
    }
    
    fun isConfigured(): Boolean = url.isNotBlank() && key.isNotBlank()
}

/**
 * App-wide settings.
 */
data class AppSettings(
    val llmConfig: LlmConfig = LlmConfig(),
    val collapsedNodeIds: Set<String> = emptySet(),
    val pinnedParentId: String? = null
)