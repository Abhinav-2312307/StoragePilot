package com.storagepilot.app.data.repository

import com.storagepilot.app.data.local.dao.ScanResultDao
import com.storagepilot.app.data.scanner.StorageScanner
import com.storagepilot.app.domain.model.ScanProgress
import com.storagepilot.app.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepositoryImpl @Inject constructor(
    private val scanner: StorageScanner,
    private val scanResultDao: ScanResultDao,
) : ScanRepository {

    override fun getScanProgress(): Flow<ScanProgress> = scanner.progress

    override suspend fun startFullScan() = scanner.performFullScan()

    override suspend fun startIncrementalScan() = scanner.performIncrementalScan()

    override suspend fun cancelScan() = scanner.cancelScan()

    override fun isScanning(): Flow<Boolean> = scanner.isScanning

    override fun getLastScanTime(): Flow<Long?> = flow {
        val session = scanResultDao.getLatestSession()
        emit(if (session?.status == "complete") session.endTime else null)
    }
}

