package com.storagepilot.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.storagepilot.app.domain.model.AppStorageInfo
import com.storagepilot.app.domain.repository.AppStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Singleton

@Singleton
class AppStorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppStorageRepository {

    private val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    private val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    private fun hasUsageStatsPermission(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getInstalledApps(): Flow<List<AppStorageInfo>> = flow {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val hasPermission = hasUsageStatsPermission()
        
        val apps = packages.mapNotNull { appInfo ->
            try {
                var appBytes = 0L
                var dataBytes = 0L
                var cacheBytes = 0L

                if (hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val userHandle = Process.myUserHandle()
                        val stats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, appInfo.packageName, userHandle)
                        appBytes = stats.appBytes
                        cacheBytes = stats.cacheBytes
                        
                        // Prevent double-counting media files via MediaProvider
                        val isMediaProvider = appInfo.packageName in listOf(
                            "com.android.providers.media",
                            "com.android.providers.media.module",
                            "com.android.providers.downloads",
                            "com.android.providers.downloads.ui",
                            "com.google.android.providers.media.module"
                        )
                        
                        dataBytes = if (isMediaProvider) 0L else stats.dataBytes
                    } catch (e: Exception) {
                        // Fallback if querying fails even with permission
                        appBytes = File(appInfo.sourceDir).length()
                    }
                } else {
                    appBytes = File(appInfo.sourceDir).length()
                }

                AppStorageInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    appSizeBytes = appBytes,
                    dataSizeBytes = dataBytes,
                    cacheSizeBytes = cacheBytes,
                    totalSizeBytes = appBytes + dataBytes + cacheBytes,
                    lastUsedTimestamp = 0L, // Requires UsageStatsManager for accurate times
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.totalSizeBytes }

        emit(apps)
    }.flowOn(Dispatchers.IO)

    override fun getCacheHeavyApps(minCacheBytes: Long): Flow<List<AppStorageInfo>> = flow {
        getInstalledApps().collect { apps ->
            emit(apps.filter { it.cacheSizeBytes >= minCacheBytes }.sortedByDescending { it.cacheSizeBytes })
        }
    }.flowOn(Dispatchers.IO)

    override fun getRarelyUsedApps(daysSinceLastUse: Int): Flow<List<AppStorageInfo>> = flow {
        emit(emptyList()) // Placeholder, requires UsageStatsManager integration
    }

    override suspend fun refreshAppData() {
        // Triggers re-fetch by consumers
    }
}
