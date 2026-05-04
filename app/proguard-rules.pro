# StoragePilot ProGuard Rules

# Keep Room entities
-keep class com.storagepilot.app.data.local.entity.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# Keep Compose
-dontwarn androidx.compose.**

# Keep Coil
-dontwarn coil.**
