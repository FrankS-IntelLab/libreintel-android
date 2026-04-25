package com.libreintel.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libreintel.domain.model.ChatMessage
import com.libreintel.domain.model.LlmConfig
import com.libreintel.domain.model.TreeNode
import com.libreintel.domain.repository.SettingsRepository
import com.libreintel.domain.repository.TreeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val currentNode: TreeNode? = null,
    val ancestorChain: List<TreeNode> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVoiceInput: Boolean = false,
    val llmConfig: LlmConfig = LlmConfig()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val treeRepository: TreeRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var nodeId: String? = null
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(llmConfig = settings.llmConfig) }
            }
        }
    }
    
    fun loadNode(id: String) {
        nodeId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val node = treeRepository.getNodeById(id)
            val ancestors = treeRepository.getAncestorChain(id)
            
            _uiState.update { 
                it.copy(
                    currentNode = node,
                    ancestorChain = ancestors,
                    messages = node?.chatHistory ?: emptyList(),
                    isLoading = false
                )
            }
        }
    }
    
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }
    
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        
        val currentNodeId = nodeId ?: return
        val config = _uiState.value.llmConfig
        
        if (!config.isConfigured()) {
            _uiState.update { it.copy(error = "Please configure LLM in settings first") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, inputText = "") }
            
            try {
                // Add user message to UI immediately
                val userMessage = ChatMessage(
                    role = com.libreintel.domain.model.MessageRole.USER,
                    content = text
                )
                
                val reply = treeRepository.sendChatMessage(currentNodeId, text, config)
                
                // Reload node to get updated chat history
                loadNode(currentNodeId)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send message",
                        inputText = text // Restore input on error
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun setVoiceInput(isVoice: Boolean) {
        _uiState.update { it.copy(isVoiceInput = isVoice) }
    }
    
    fun branchFromSelection(selectedText: String) {
        val currentNodeId = nodeId ?: return
        
        viewModelScope.launch {
            val childNode = TreeNode(
                parentId = currentNodeId,
                title = selectedText.take(50) + if (selectedText.length > 50) "…" else "",
                fullText = selectedText
            )
            
            treeRepository.addNode(childNode)
            
            // Navigate to the new child node
            loadNode(childNode.id)
        }
    }
}