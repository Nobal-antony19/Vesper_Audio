package com.vesper.audio.util

import android.content.Context
import android.net.Uri
import com.vesper.audio.model.Track
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class TrackLyrics(
    val isSynced: Boolean,
    val syncedLines: List<LyricLine> = emptyList(),
    val rawText: String = ""
)

object VesperLyricsResolver {

    private val lrcPattern = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")

    fun resolveLyrics(context: Context, track: Track): TrackLyrics? {
        // 1. Check companion .lrc file next to audio file if file path is available
        val path = track.contentUri.path.orEmpty()
        if (path.isNotBlank()) {
            val audioFile = File(path)
            if (audioFile.exists()) {
                val baseName = audioFile.nameWithoutExtension
                val parentDir = audioFile.parentFile
                if (parentDir != null) {
                    val lrcFile = File(parentDir.absolutePath, "$baseName.lrc")
                    if (lrcFile.exists() && lrcFile.canRead()) {
                        try {
                            val content = lrcFile.readText()
                            val parsed = parseLrc(content)
                            if (parsed != null) return parsed
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val txtFile = File(parentDir.absolutePath, "$baseName.txt")
                    if (txtFile.exists() && txtFile.canRead()) {
                        try {
                            val content = txtFile.readText()
                            if (content.isNotBlank()) {
                                return parseLrc(content) ?: TrackLyrics(isSynced = false, rawText = content)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        // 2. Check content Uri stream if accessible
        try {
            val uri = track.contentUri
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val parent = file.parentFile
                    if (parent != null) {
                        val lrcFile = File(parent.absolutePath, "${file.nameWithoutExtension}.lrc")
                        if (lrcFile.exists()) {
                            val parsed = parseLrc(lrcFile.readText())
                            if (parsed != null) return parsed
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun parseLrc(lrcContent: String): TrackLyrics? {
        if (lrcContent.isBlank()) return null

        val lines = lrcContent.lines()
        val syncedList = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<String>()
        var foundTimestamps = false

        for (line in lines) {
            val matcher = lrcPattern.matcher(line.trim())
            if (matcher.matches()) {
                foundTimestamps = true
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisString = matcher.group(3) ?: "0"
                val millis = when (millisString.length) {
                    1 -> (millisString.toLongOrNull() ?: 0L) * 100
                    2 -> (millisString.toLongOrNull() ?: 0L) * 10
                    else -> millisString.take(3).toLongOrNull() ?: 0L
                }
                val totalMs = (minutes * 60 + seconds) * 1000 + millis
                val text = matcher.group(4)?.trim() ?: ""
                syncedList.add(LyricLine(totalMs, text))
            } else {
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && !trimmed.startsWith("[ti:") && !trimmed.startsWith("[ar:") && !trimmed.startsWith("[al:")) {
                    plainLines.add(trimmed)
                }
            }
        }

        if (foundTimestamps && syncedList.isNotEmpty()) {
            syncedList.sortBy { it.timestampMs }
            return TrackLyrics(
                isSynced = true,
                syncedLines = syncedList,
                rawText = syncedList.joinToString("\n") { it.text }
            )
        } else if (plainLines.isNotEmpty()) {
            return TrackLyrics(
                isSynced = false,
                rawText = plainLines.joinToString("\n")
            )
        }

        return null
    }
}
