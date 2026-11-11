package com.vesper.audio.data.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.vesper.audio.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates playable local stereo WAV master demo tracks into internal storage.
 * Ensures that even in an empty emulator or fresh device, real Android audio decoding,
 * real audioSessionId, hardware audio effects, and notification controls can be exercised immediately.
 */
class DemoAudioProvider(private val context: Context) {

    suspend fun getOrGenerateDemoTracks(): List<Track> = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "sonora_demo_tracks").apply { mkdirs() }

        val demoList = listOf(
            DemoSpec(
                id = "demo_apocalypse",
                filename = "apocalypse_ambient.wav",
                title = "Apocalypse (Acoustic Ambient)",
                artist = "Cigarettes After Sex (Tribute)",
                album = "Ambient Master Sessions",
                durationSeconds = 45,
                baseFreq = 220.0,
                waveformType = 0
            ),
            DemoSpec(
                id = "demo_after_hours",
                filename = "after_hours_synth.wav",
                title = "After Hours (Midnight Pulse)",
                artist = "The Weeknd (Tribute)",
                album = "Neon & Gold Vol. 1",
                durationSeconds = 40,
                baseFreq = 165.0,
                waveformType = 1
            ),
            DemoSpec(
                id = "demo_random_access",
                filename = "random_access_electronic.wav",
                title = "Random Access (Electro Groove)",
                artist = "Daft Punk (Tribute)",
                album = "Memory Archives",
                durationSeconds = 50,
                baseFreq = 130.0,
                waveformType = 2
            ),
            DemoSpec(
                id = "demo_meteora",
                filename = "meteora_resonance.wav",
                title = "Meteora (Harmonic Drift)",
                artist = "Linkin Park (Tribute)",
                album = "Hybrid Resonance",
                durationSeconds = 35,
                baseFreq = 196.0,
                waveformType = 0
            )
        )

        demoList.map { spec ->
            demoDir.mkdirs()
            val file = File(demoDir, spec.filename)
            if (!file.exists() || file.length() < 1000) {
                generateWavFile(file, spec)
            }

            Track(
                id = spec.id,
                title = spec.title,
                artist = spec.artist,
                album = spec.album,
                durationMs = spec.durationSeconds * 1000L,
                contentUri = Uri.fromFile(file),
                artworkUri = null,
                trackNumber = 1,
                year = 2024,
                dateAdded = System.currentTimeMillis() / 1000,
                sizeBytes = file.length()
            )
        }
    }

    private data class DemoSpec(
        val id: String,
        val filename: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Int,
        val baseFreq: Double,
        val waveformType: Int
    )

    private fun generateWavFile(targetFile: File, spec: DemoSpec) {
        val sampleRate = 44100
        val numChannels = 2
        val bytesPerSample = 2
        val totalSamples = sampleRate * spec.durationSeconds
        val dataSize = totalSamples * numChannels * bytesPerSample

        FileOutputStream(targetFile).use { out ->
            // RIFF header
            out.write("RIFF".toByteArray())
            out.write(intToBytes(36 + dataSize))
            out.write("WAVE".toByteArray())

            // fmt subchunk
            out.write("fmt ".toByteArray())
            out.write(intToBytes(16)) // Subchunk1Size for PCM
            out.write(shortToBytes(1.toShort())) // AudioFormat 1 = PCM
            out.write(shortToBytes(numChannels.toShort()))
            out.write(intToBytes(sampleRate))
            out.write(intToBytes(sampleRate * numChannels * bytesPerSample)) // ByteRate
            out.write(shortToBytes((numChannels * bytesPerSample).toShort())) // BlockAlign
            out.write(shortToBytes((bytesPerSample * 8).toShort())) // BitsPerSample

            // data subchunk
            out.write("data".toByteArray())
            out.write(intToBytes(dataSize))

            // Generate melodic harmonic audio samples
            val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            val twoPi = 2.0 * PI

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = when {
                    t < 1.0 -> (t / 1.0)
                    t > (spec.durationSeconds - 2.0) -> ((spec.durationSeconds - t) / 2.0).coerceAtLeast(0.0)
                    else -> 1.0
                }

                // Smooth chords and harmonics
                val tone1 = sin(twoPi * spec.baseFreq * t)
                val tone2 = 0.5 * sin(twoPi * (spec.baseFreq * 1.5) * t)
                val tone3 = 0.3 * sin(twoPi * (spec.baseFreq * 2.0) * t)
                val sampleValue = ((tone1 + tone2 + tone3) * 0.5 * envelope * 24000.0).toInt().coerceIn(-32767, 32767).toShort()

                if (buffer.remaining() < 4) {
                    out.write(buffer.array(), 0, buffer.position())
                    buffer.clear()
                }

                // Stereo Left & Right
                buffer.putShort(sampleValue)
                buffer.putShort(sampleValue)
            }

            if (buffer.position() > 0) {
                out.write(buffer.array(), 0, buffer.position())
            }
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}
