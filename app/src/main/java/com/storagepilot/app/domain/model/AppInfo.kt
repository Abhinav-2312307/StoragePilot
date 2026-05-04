package com.storagepilot.app.domain.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
    val totalSizeBytes: Long,
    val cacheSizeBytes: Long,
    val dataSizeBytes: Long = 0L,
    val appSizeBytes: Long = 0L,
)
