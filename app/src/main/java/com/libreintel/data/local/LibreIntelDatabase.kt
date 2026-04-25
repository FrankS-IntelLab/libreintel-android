package com.libreintel.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.libreintel.data.local.dao.TreeNodeDao
import com.libreintel.data.local.entity.Converters
import com.libreintel.data.local.entity.TreeNodeEntity

/**
 * Room database for LibreIntel.
 */
@Database(
    entities = [TreeNodeEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LibreIntelDatabase : RoomDatabase() {
    abstract fun treeNodeDao(): TreeNodeDao
    
    companion object {
        const val DATABASE_NAME = "libreintel_db"
    }
}