# VESPER AUDIO — R8 / ProGuard Configuration
# Preserves stack trace line numbers while hiding local source file paths
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AndroidX Media3 (ExoPlayer & MediaSessionService)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room Database Entities, DAOs, and Database Implementation
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class com.vesper.audio.data.local.** { *; }

# Data Models & Serialization
-keep class com.vesper.audio.model.** { *; }
-keep class com.vesper.audio.data.remote.** { *; }

# Moshi / OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
