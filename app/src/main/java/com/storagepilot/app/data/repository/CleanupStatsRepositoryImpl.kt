package com.storagepilot.app.data.repository

import com.storagepilot.app.data.local.dao.CleanupStatsDao
import com.storagepilot.app.data.local.entity.CleanupStatEntity
import com.storagepilot.app.domain.model.CleanupSession
import com.storagepilot.app.domain.repository.CleanupStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupStatsRepositoryImpl @Inject constructor(
    private val cleanupStatsDao: CleanupStatsDao,
) : CleanupStatsRepository {

    override fun getLifetimeStats(): Flow<CleanupSession> =
        cleanupStatsDao.getLifetimeStats().map { projection ->
            CleanupSession(
                timestamp = System.currentTimeMillis(),
                bytesFreed = projection.bytesFreed,
                filesDeleted = projection.filesDeleted,
                sessionType = "lifetime",
            )
        }

    override fun getRecentSessions(limit: Int): Flow<List<CleanupSession>> =
        cleanupStatsDao.getRecentSessions(limit).map { list ->
            list.map { entity ->
                CleanupSession(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    bytesFreed = entity.bytesFreed,
                    filesDeleted = entity.filesDeleted,
                    sessionType = entity.sessionType,
                )
            }
        }

    override suspend fun recordCleanup(bytesFreed: Long, filesDeleted: Int, sessionType: String) {
        cleanupStatsDao.insert(
            CleanupStatEntity(
                timestamp = System.currentTimeMillis(),
                bytesFreed = bytesFreed,
                filesDeleted = filesDeleted,
                sessionType = sessionType,
            )
        )
    }
}
