package com.storagepilot.app.core.di

import android.content.Context
import androidx.room.Room
import com.storagepilot.app.data.local.StoragePilotDatabase
import com.storagepilot.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StoragePilotDatabase {
        return Room.databaseBuilder(
            context,
            StoragePilotDatabase::class.java,
            "storage_pilot.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFileIndexDao(db: StoragePilotDatabase): FileIndexDao = db.fileIndexDao()

    @Provides
    fun provideScanResultDao(db: StoragePilotDatabase): ScanResultDao = db.scanResultDao()

    @Provides
    fun provideRecycleBinDao(db: StoragePilotDatabase): RecycleBinDao = db.recycleBinDao()

    @Provides
    fun provideCleanupStatsDao(db: StoragePilotDatabase): CleanupStatsDao = db.cleanupStatsDao()
}
