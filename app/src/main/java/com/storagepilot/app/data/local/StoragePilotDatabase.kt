package com.storagepilot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.storagepilot.app.data.local.dao.*
import com.storagepilot.app.data.local.entity.*

@Database(
    entities = [
        FileIndexEntity::class,
        ScanSessionEntity::class,
        RecycleBinEntity::class,
        CleanupStatEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StoragePilotDatabase : RoomDatabase() {
    abstract fun fileIndexDao(): FileIndexDao
    abstract fun scanResultDao(): ScanResultDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun cleanupStatsDao(): CleanupStatsDao
}
