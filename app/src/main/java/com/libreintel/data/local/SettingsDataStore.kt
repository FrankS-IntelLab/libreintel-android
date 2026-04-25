package com.libreintel.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.libreintel.domain.model.AppSettings
import com.libreintel.domain.model.LlmConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore manager for app settings.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val LLM_URL = stringPreferencesKey("llm_url")
        val LLM_KEY = stringPreferencesKey("llm_key")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val COLLAPSED_NODE_IDS = stringSetPreferencesKey("collapsed_node_ids")
        val PINNED_PARENT_ID = stringPreferencesKey("pinned_parent_id")
    }
    
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            llmConfig = LlmConfig(
                url = preferences[PreferencesKeys.LLM_URL] ?: "",
                key = preferences[PreferencesKeys.LLM_KEY] ?: "",
                model = preferences[PreferencesKeys.LLM_MODEL] ?: ""
            ),
            collapsedNodeIds = preferences[PreferencesKeys.COLLAPSED_NODE_IDS] ?: emptySet(),
            pinnedParentId = preferences[PreferencesKeys.PINNED_PARENT_ID]
        )
    }
    
    suspend fun saveLlmConfig(config: LlmConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_URL] = config.url
            preferences[PreferencesKeys.LLM_KEY] = config.key
            preferences[PreferencesKeys.LLM_MODEL] = config.model
        }
    }
    
    suspend fun saveCollapsedNodeIds(ids: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COLLAPSED_NODE_IDS] = ids
        }
    }
    
    suspend fun savePinnedParentId(id: String?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[PreferencesKeys.PINNED_PARENT_ID] = id
            } else {
                preferences.remove(PreferencesKeys.PINNED_PARENT_ID)
            }
        }
    }
}