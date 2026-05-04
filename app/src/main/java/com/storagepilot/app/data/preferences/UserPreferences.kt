package com.storagepilot.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "storage_pilot_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
        private val KEY_RECYCLE_BIN_DAYS = intPreferencesKey("recycle_bin_days")
        private val KEY_LARGE_FILE_THRESHOLD_MB = intPreferencesKey("large_file_threshold_mb")
        private val KEY_SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
        private val KEY_LIFETIME_BYTES_FREED = longPreferencesKey("lifetime_bytes_freed")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }
    val lastScanTime: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_SCAN_TIME] ?: 0L }
    val recycleBinDays: Flow<Int> = context.dataStore.data.map { it[KEY_RECYCLE_BIN_DAYS] ?: 30 }
    val largeFileThresholdMb: Flow<Int> = context.dataStore.data.map { it[KEY_LARGE_FILE_THRESHOLD_MB] ?: 50 }
    val showHiddenFiles: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_HIDDEN_FILES] ?: false }
    val lifetimeBytesFreed: Flow<Long> = context.dataStore.data.map { it[KEY_LIFETIME_BYTES_FREED] ?: 0L }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }

    suspend fun setLastScanTime(time: Long) {
        context.dataStore.edit { it[KEY_LAST_SCAN_TIME] = time }
    }

    suspend fun setRecycleBinDays(days: Int) {
        context.dataStore.edit { it[KEY_RECYCLE_BIN_DAYS] = days }
    }

    suspend fun setLargeFileThreshold(mb: Int) {
        context.dataStore.edit { it[KEY_LARGE_FILE_THRESHOLD_MB] = mb }
    }

    suspend fun setShowHiddenFiles(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_HIDDEN_FILES] = show }
    }

    suspend fun addBytesFreed(bytes: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_LIFETIME_BYTES_FREED] ?: 0L
            prefs[KEY_LIFETIME_BYTES_FREED] = current + bytes
        }
    }
}
