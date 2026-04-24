package com.libreintel.data.repository

import com.google.gson.Gson
import com.libreintel.data.local.LibreIntelDatabase
import com.libreintel.data.local.SettingsDataStore
import com.libreintel.data.local.entity.toDomain
import com.libreintel.data.local.entity.toEntity
import com.libreintel.data.remote.ChatApiService
import com.libreintel.data.remote.ChatCompletionRequest
import com.libreintel.data.remote.ChatMessageDto
import com.libreintel.domain.model.AppSettings
import com.libreintel.domain.model.ChatMessage
import com.libreintel.domain.model.LlmConfig
import com.libreintel.domain.model.MessageRole
import com.libreintel.domain.model.TreeNode
import com.libreintel.domain.repository.SettingsRepository
import com.libreintel.domain.repository.TreeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TreeRepository.
 */
@Singleton
class TreeRepositoryImpl @Inject constructor(
    private val database: LibreIntelDatabase,
    private val chatApiService: ChatApiService,
    private val settingsDataStore: SettingsDataStore
) : TreeRepository {
    
    private val dao = database.treeNodeDao()
    private val gson = Gson()
    
    override fun getAllNodes(): Flow<List<TreeNode>> {
        return dao.getAllNodes().map { entities ->
            buildTreeStructure(entities.map { it.toDomain() })
        }
    }
    
    override fun getRootNodes(): Flow<List<TreeNode>> {
        return dao.getRootNodes().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getNodeById(id: String): TreeNode? {
        return dao.getNodeById(id)?.toDomain()
    }
    
    override suspend fun addNode(node: TreeNode): TreeNode {
        dao.insertNode(node.toEntity())
        return node
    }
    
    override suspend fun updateNode(node: TreeNode) {
        dao.updateNode(node.toEntity())
    }
    
    override suspend fun deleteNode(id: String) {
        // First delete all descendants recursively
        deleteDescendants(id)
        dao.deleteNodeById(id)
    }
    
    private suspend fun deleteDescendants(parentId: String) {
        val children = dao.getChildNodes(parentId)
        children.forEach { child ->
            deleteDescendants(child.id)
            dao.deleteNodeById(child.id)
        }
    }
    
    override suspend fun addChatMessage(nodeId: String, message: ChatMessage) {
        val node = dao.getNodeById(nodeId)?.toDomain() ?: return
        val updatedNode = node.copy(chatHistory = node.chatHistory + message)
        dao.updateNode(updatedNode.toEntity())
    }
    
    override suspend fun getAncestorChain(nodeId: String): List<TreeNode> {
        val chain = mutableListOf<TreeNode>()
        var currentId: String? = nodeId
        
        while (currentId != null) {
            val node = dao.getNodeById(currentId)?.toDomain() ?: break
            chain.add(0, node)
            currentId = node.parentId
        }
        
        return chain
    }
    
    override suspend fun generateAiTitle(node: TreeNode, config: LlmConfig): String? {
        if (!config.isConfigured()) return null
        
        return try {
            val request = ChatCompletionRequest(
                model = config.model,
                messages = listOf(
                    ChatMessageDto("system", "Generate a concise one-line title (max 8 words) summarizing this text. Return ONLY the title, nothing else."),
                    ChatMessageDto("user", node.fullText)
                )
            )
            
            val response = chatApiService.chatCompletion(
                authorization = "Bearer ${config.key}",
                request = request
            )
            
            if (response.isSuccessful) {
                response.body()?.choices?.firstOrNull()?.message?.content?.trim()
                    ?.replace(Regex("^[\"']|[\"']$"), "")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun sendChatMessage(nodeId: String, userMessage: String, config: LlmConfig): String {
        if (!config.isConfigured()) {
            throw IllegalStateException("LLM not configured")
        }
        
        val node = dao.getNodeById(nodeId)?.toDomain() 
            ?: throw IllegalArgumentException("Node not found")
        
        // Build ancestor chain for context
        val ancestors = getAncestorChain(nodeId)
        val contextChain = ancestors.joinToString(" → ") { 
            "\"${it.fullText.take(200)}\"" 
        }
        
        val systemPrompt = buildString {
            append("You are a study assistant. The user is exploring a chain of concepts:\n\n")
            append("Exploration path: $contextChain\n\n")
            append("Current focus:\n\"${node.fullText}\"\n\n")
            append("Answer their questions concisely.")
        }
        
        // Build messages with history
        val messages = mutableListOf(ChatMessageDto("system", systemPrompt))
        
        // Add conversation history
        node.chatHistory.forEach { msg ->
            messages.add(ChatMessageDto(
                role = if (msg.role == MessageRole.USER) "user" else "assistant",
                content = msg.content
            ))
        }
        
        // Add current user message
        messages.add(ChatMessageDto("user", userMessage))
        
        val request = ChatCompletionRequest(
            model = config.model,
            messages = messages
        )
        
        val response = chatApiService.chatCompletion(
            authorization = "Bearer ${config.key}",
            request = request
        )
        
        if (response.isSuccessful) {
            val reply = response.body()?.choices?.firstOrNull()?.message?.content 
                ?: throw Exception("Empty response")
            
            // Save user message
            val userMsg = ChatMessage(
                role = MessageRole.USER,
                content = userMessage
            )
            
            // Save assistant response
            val assistantMsg = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = reply
            )
            
            val updatedNode = node.copy(
                chatHistory = node.chatHistory + userMsg + assistantMsg,
                lastAccessedAt = java.util.Date()
            )
            dao.updateNode(updatedNode.toEntity())
            
            return reply
        } else {
            throw Exception("API error: ${response.code()} - ${response.message()}")
        }
    }
    
    override suspend fun exportAllAsJson(): String {
        val allEntities = dao.getAllNodes().first()
        val allNodes = allEntities.map { it.toDomain() }
        val roots = buildTreeStructure(allNodes)
        return gson.toJson(roots)
    }
    
    override suspend fun exportBranchAsJson(nodeId: String): String {
        val node = dao.getNodeById(nodeId)?.toDomain() ?: return "[]"
        
        // Get all descendants
        val allEntities = dao.getAllNodes().first()
        val allNodes = allEntities.map { it.toDomain() }
        
        // Find the node and its subtree
        fun findSubtree(nodes: List<TreeNode>, targetId: String): TreeNode? {
            for (node in nodes) {
                if (node.id == targetId) return node
                val found = findSubtree(node.children, targetId)
                if (found != null) return found
            }
            return null
        }
        
        val subtree = findSubtree(allNodes, nodeId)
        return gson.toJson(listOf(subtree))
    }
    
    override suspend fun importFromJson(json: String) {
        try {
            val type = object : com.google.gson.reflect.TypeToken<List<TreeNode>>() {}.type
            val importedNodes: List<TreeNode> = gson.fromJson(json, type)
            
            // Insert all imported nodes
            val entities = importedNodes.flatMap { node ->
                collectAllNodes(node)
            }.map { it.toEntity() }
            
            dao.insertNodes(entities)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON format: ${e.message}")
        }
    }
    
    private fun collectAllNodes(node: TreeNode): List<TreeNode> {
        val nodes = mutableListOf(node)
        node.children.forEach { child ->
            nodes.addAll(collectAllNodes(child))
        }
        return nodes
    }
    
    override suspend fun clearAll() {
        dao.deleteAllNodes()
    }
    
    /**
     * Build tree structure from flat list of nodes.
     */
    private fun buildTreeStructure(nodes: List<TreeNode>): List<TreeNode> {
        val nodeMap = nodes.associateBy { it.id }
        val roots = mutableListOf<TreeNode>()
        
        nodes.forEach { node ->
            if (node.parentId == null) {
                roots.add(buildSubtree(node, nodeMap))
            } else if (!nodeMap.containsKey(node.parentId)) {
                // Parent doesn't exist, treat as root
                roots.add(buildSubtree(node, nodeMap))
            }
        }
        
        return roots
    }
    
    private fun buildSubtree(node: TreeNode, nodeMap: Map<String, TreeNode>): TreeNode {
        val children = nodeMap.values.filter { it.parentId == node.id }
        return node.copy(
            children = children.map { buildSubtree(it, nodeMap) }
        )
    }
}

/**
 * Implementation of SettingsRepository.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {
    
    override val settings: Flow<AppSettings> = settingsDataStore.settingsFlow
    
    override suspend fun saveLlmConfig(config: LlmConfig) {
        settingsDataStore.saveLlmConfig(config)
    }
    
    override suspend fun saveCollapsedNodeIds(ids: Set<String>) {
        settingsDataStore.saveCollapsedNodeIds(ids)
    }
    
    override suspend fun savePinnedParentId(id: String?) {
        settingsDataStore.savePinnedParentId(id)
    }
}