package com.storagepilot.app.core.di

import com.storagepilot.app.data.repository.*
import com.storagepilot.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindScanRepository(impl: ScanRepositoryImpl): ScanRepository

    @Binds
    @Singleton
    abstract fun bindRecycleBinRepository(impl: RecycleBinRepositoryImpl): RecycleBinRepository

    @Binds
    @Singleton
    abstract fun bindCleanupStatsRepository(impl: CleanupStatsRepositoryImpl): CleanupStatsRepository

    @Binds
    @Singleton
    abstract fun bindAppStorageRepository(impl: AppStorageRepositoryImpl): AppStorageRepository
}
