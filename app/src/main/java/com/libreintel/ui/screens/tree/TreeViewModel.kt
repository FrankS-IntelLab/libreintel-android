package com.libreintel.ui.screens.tree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libreintel.domain.model.AppSettings
import com.libreintel.domain.model.LlmConfig
import com.libreintel.domain.model.TreeNode
import com.libreintel.domain.repository.SettingsRepository
import com.libreintel.domain.repository.TreeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreeUiState(
    val rootNodes: List<TreeNode> = emptyList(),
    val collapsedNodeIds: Set<String> = emptySet(),
    val pinnedParentId: String? = null,
    val selectedNodeId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TreeViewModel @Inject constructor(
    private val treeRepository: TreeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TreeUiState())
    val uiState: StateFlow<TreeUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            combine(
                treeRepository.getRootNodes(),
                settingsRepository.settings
            ) { nodes, settings ->
                TreeUiState(
                    rootNodes = nodes,
                    collapsedNodeIds = settings.collapsedNodeIds,
                    pinnedParentId = settings.pinnedParentId
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun addNode(text: String, sourceUrl: String? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val node = TreeNode(
                title = text.take(50) + if (text.length > 50) "…" else "",
                fullText = text,
                sourceUrl = sourceUrl,
                parentId = state.pinnedParentId
            )
            
            val addedNode = treeRepository.addNode(node)
            
            // Unpin parent after adding if it was pinned
            if (state.pinnedParentId != null) {
                settingsRepository.savePinnedParentId(null)
            }
            
            // Generate AI title
            val settings = settingsRepository.settings.first()
            if (settings.llmConfig.isConfigured()) {
                val aiTitle = treeRepository.generateAiTitle(addedNode, settings.llmConfig)
                if (aiTitle != null) {
                    val updatedNode = addedNode.copy(title = aiTitle)
                    treeRepository.updateNode(updatedNode)
                }
            }
        }
    }
    
    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            treeRepository.deleteNode(nodeId)
        }
    }
    
    fun toggleNodeCollapse(nodeId: String) {
        viewModelScope.launch {
            val currentIds = _uiState.value.collapsedNodeIds
            val newIds = if (nodeId in currentIds) {
                currentIds - nodeId
            } else {
                currentIds + nodeId
            }
            settingsRepository.saveCollapsedNodeIds(newIds)
        }
    }
    
    fun setPinnedParent(nodeId: String?) {
        viewModelScope.launch {
            settingsRepository.savePinnedParentId(nodeId)
        }
    }
    
    fun selectNode(nodeId: String) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }
    
    fun clearSelection() {
        _uiState.update { it.copy(selectedNodeId = null) }
    }
    
    fun getNodeById(nodeId: String): TreeNode? {
        return findNodeInTree(_uiState.value.rootNodes, nodeId)
    }
    
    private fun findNodeInTree(nodes: List<TreeNode>, id: String): TreeNode? {
        for (node in nodes) {
            if (node.id == id) return node
            val found = findNodeInTree(node.children, id)
            if (found != null) return found
        }
        return null
    }
    
    // Export functions
    suspend fun exportAllAsJson(): String {
        return treeRepository.exportAllAsJson()
    }
    
    suspend fun exportBranchAsJson(nodeId: String): String {
        return treeRepository.exportBranchAsJson(nodeId)
    }
    
    suspend fun importFromJson(json: String) {
        treeRepository.importFromJson(json)
    }
}