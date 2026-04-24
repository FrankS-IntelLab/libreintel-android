package com.libreintel.data.local.dao

import androidx.room.*
import com.libreintel.data.local.entity.TreeNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for tree nodes.
 */
@Dao
interface TreeNodeDao {
    
    @Query("SELECT * FROM tree_nodes ORDER BY timestamp DESC")
    fun getAllNodes(): Flow<List<TreeNodeEntity>>
    
    @Query("SELECT * FROM tree_nodes WHERE id = :id")
    suspend fun getNodeById(id: String): TreeNodeEntity?
    
    @Query("SELECT * FROM tree_nodes WHERE parentId IS NULL ORDER BY timestamp DESC")
    fun getRootNodes(): Flow<List<TreeNodeEntity>>
    
    @Query("SELECT * FROM tree_nodes WHERE parentId = :parentId ORDER BY timestamp ASC")
    suspend fun getChildNodes(parentId: String): List<TreeNodeEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: TreeNodeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<TreeNodeEntity>)
    
    @Update
    suspend fun updateNode(node: TreeNodeEntity)
    
    @Delete
    suspend fun deleteNode(node: TreeNodeEntity)
    
    @Query("DELETE FROM tree_nodes WHERE id = :id")
    suspend fun deleteNodeById(id: String)
    
    @Query("DELETE FROM tree_nodes")
    suspend fun deleteAllNodes()
    
    @Query("SELECT * FROM tree_nodes WHERE id IN (:ids)")
    suspend fun getNodesByIds(ids: List<String>): List<TreeNodeEntity>
    
    @Query("SELECT * FROM tree_nodes WHERE parentId IS NULL ORDER BY lastAccessedAt DESC LIMIT 2")
    suspend fun getMostRecentlyAccessedRoots(): List<TreeNodeEntity>
}