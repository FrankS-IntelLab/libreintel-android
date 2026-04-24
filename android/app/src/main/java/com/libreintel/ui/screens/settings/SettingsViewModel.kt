package com.libreintel.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libreintel.domain.model.AppSettings
import com.libreintel.domain.model.LlmConfig
import com.libreintel.domain.repository.SettingsRepository
import com.libreintel.domain.repository.TreeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val llmConfig: LlmConfig = LlmConfig(),
    val selectedPreset: String = "custom",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val treeRepository: TreeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                val preset = when {
                    settings.llmConfig.url.contains("dashscope") -> "dashscope"
                    settings.llmConfig.url.contains("openai") -> "openai"
                    settings.llmConfig.url.contains("localhost") -> "ollama"
                    else -> "custom"
                }
                
                _uiState.update { 
                    it.copy(
                        llmConfig = settings.llmConfig,
                        selectedPreset = preset
                    ) 
                }
            }
        }
    }
    
    fun updateUrl(url: String) {
        _uiState.update { it.copy(llmConfig = it.llmConfig.copy(url = url), selectedPreset = "custom") }
    }
    
    fun updateKey(key: String) {
        _uiState.update { it.copy(llmConfig = it.llmConfig.copy(key = key)) }
    }
    
    fun updateModel(model: String) {
        _uiState.update { it.copy(llmConfig = it.llmConfig.copy(model = model), selectedPreset = "custom") }
    }
    
    fun selectPreset(preset: String) {
        val config = when (preset) {
            "dashscope" -> LlmConfig.DASHSCOPE_PRESET
            "openai" -> LlmConfig.OPENAI_PRESET
            "ollama" -> LlmConfig.OLLAMA_PRESET
            else -> _uiState.value.llmConfig
        }
        
        _uiState.update { 
            it.copy(
                llmConfig = config,
                selectedPreset = preset
            ) 
        }
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }
            
            try {
                settingsRepository.saveLlmConfig(_uiState.value.llmConfig)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                
                // Reset success indicator after delay
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(saveSuccess = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    suspend fun exportAllData(): String {
        return treeRepository.exportAllAsJson()
    }
    
    suspend fun importData(json: String) {
        treeRepository.importFromJson(json)
    }
    
    suspend fun clearAllData() {
        treeRepository.clearAll()
    }
}