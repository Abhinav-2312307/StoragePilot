package com.storagepilot.app.data.repository

import com.storagepilot.app.data.scanner.StorageScanner
import com.storagepilot.app.domain.model.ScanProgress
import com.storagepilot.app.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepositoryImpl @Inject constructor(
    private val scanner: StorageScanner,
) : ScanRepository {

    override fun getScanProgress(): Flow<ScanProgress> = scanner.progress

    override suspend fun startFullScan() = scanner.performFullScan()

    override suspend fun startIncrementalScan() = scanner.performIncrementalScan()

    override suspend fun cancelScan() = scanner.cancelScan()

    override fun isScanning(): Flow<Boolean> = scanner.isScanning

    override fun getLastScanTime(): Flow<Long?> {
        // TODO: Implement via ScanResultDao
        return kotlinx.coroutines.flow.flowOf(null)
    }
}
