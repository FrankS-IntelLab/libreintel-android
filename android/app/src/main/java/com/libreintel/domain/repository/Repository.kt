package com.libreintel.domain.repository

import com.libreintel.domain.model.AppSettings
import com.libreintel.domain.model.ChatMessage
import com.libreintel.domain.model.LlmConfig
import com.libreintel.domain.model.TreeNode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for tree nodes.
 */
interface TreeRepository {
    fun getAllNodes(): Flow<List<TreeNode>>
    fun getRootNodes(): Flow<List<TreeNode>>
    suspend fun getNodeById(id: String): TreeNode?
    suspend fun addNode(node: TreeNode): TreeNode
    suspend fun updateNode(node: TreeNode)
    suspend fun deleteNode(id: String)
    suspend fun addChatMessage(nodeId: String, message: ChatMessage)
    suspend fun getAncestorChain(nodeId: String): List<TreeNode>
    suspend fun generateAiTitle(node: TreeNode, config: LlmConfig): String?
    suspend fun sendChatMessage(nodeId: String, userMessage: String, config: LlmConfig): String
    suspend fun exportAllAsJson(): String
    suspend fun exportBranchAsJson(nodeId: String): String
    suspend fun importFromJson(json: String)
    suspend fun clearAll()
}

/**
 * Repository interface for settings.
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun saveLlmConfig(config: LlmConfig)
    suspend fun saveCollapsedNodeIds(ids: Set<String>)
    suspend fun savePinnedParentId(id: String?)
}