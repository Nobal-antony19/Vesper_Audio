package com.vesper.audio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FavoriteTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        CustomEqPresetEntity::class,
        ScannedFolderEntity::class,
        CachedTrackEntity::class,
        DiscoverCachedTrackEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class SonoraDatabase : RoomDatabase() {
    abstract fun sonoraDao(): SonoraDao

    companion object {
        @Volatile
        private var INSTANCE: SonoraDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scanned_folders` (
                        `uriString` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`uriString`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `listening_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackId` TEXT NOT NULL,
                        `trackTitle` TEXT NOT NULL,
                        `artistName` TEXT NOT NULL,
                        `albumName` TEXT NOT NULL,
                        `durationPlayedMs` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_tracks` (
                        `id` TEXT NOT NULL,
                        `mediaStoreId` INTEGER NOT NULL,
                        `contentUriString` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `trackNumber` INTEGER NOT NULL,
                        `discNumber` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `genre` TEXT NOT NULL,
                        `albumArtist` TEXT NOT NULL,
                        `artworkUriString` TEXT,
                        `dateAdded` INTEGER NOT NULL,
                        `dateModified` INTEGER NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_tracks_mediaStoreId` ON `cached_tracks` (`mediaStoreId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_tracks_artist` ON `cached_tracks` (`artist`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_tracks_album` ON `cached_tracks` (`album`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `downloads` (
                        `id` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `bytes` INTEGER NOT NULL,
                        `total` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `finishedAt` INTEGER,
                        `error` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Safe non-destructive migration from v5 to v6:
         * Drops retired tables (playback_history, listening_events, downloads)
         * while preserving favorites, playlists, playlist_tracks, eq_presets, scanned_folders, and cached_tracks.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `playback_history`")
                db.execSQL("DROP TABLE IF EXISTS `listening_events`")
                db.execSQL("DROP TABLE IF EXISTS `downloads`")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cached_tracks` ADD COLUMN `isUserOverride` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `discover_cache` (
                        `providerId` TEXT NOT NULL,
                        `cacheKey` TEXT NOT NULL,
                        `providerTrackId` TEXT NOT NULL,
                        `namespacedId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `artworkUrl` TEXT,
                        `durationMs` INTEGER NOT NULL,
                        `genre` TEXT NOT NULL,
                        `releaseYear` INTEGER NOT NULL,
                        `isExplicit` INTEGER NOT NULL,
                        `sortIndex` INTEGER NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        PRIMARY KEY(`providerId`, `cacheKey`, `providerTrackId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_discover_cache_providerId_cacheKey` ON `discover_cache` (`providerId`, `cacheKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_discover_cache_fetchedAt` ON `discover_cache` (`fetchedAt`)")
            }
        }

        fun getInstance(context: Context): SonoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SonoraDatabase::class.java,
                    "sonora_music.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

