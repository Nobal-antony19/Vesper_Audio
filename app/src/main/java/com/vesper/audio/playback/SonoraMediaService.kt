package com.vesper.audio.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vesper.audio.MainActivity
import com.vesper.audio.audio.AudioEffectController

class SonoraMediaService : MediaSessionService() {

    companion object {
        val audioEffectController = AudioEffectController()

        @Volatile
        private var activeExoPlayer: ExoPlayer? = null

        @Volatile
        private var gaplessEnabledFlag: Boolean = true

        @OptIn(UnstableApi::class)
        fun setGaplessPlayback(enabled: Boolean) {
            gaplessEnabledFlag = enabled
            activeExoPlayer?.pauseAtEndOfMediaItems = !enabled
        }
    }

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Fast startup & pre-buffering LoadControl for seamless local & Discover playback
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .setBackBuffer(/* backBufferDurationMs = */ 10_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val newPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setPauseAtEndOfMediaItems(!gaplessEnabledFlag)
            .build()

        newPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioEffectController.attachSession(audioSessionId)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val sessionId = newPlayer.audioSessionId
                if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                    audioEffectController.attachSession(sessionId)
                    audioEffectController.reapplyActiveEffects()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = newPlayer.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        audioEffectController.attachSession(sessionId)
                    }
                }
            }
        })

        player = newPlayer
        activeExoPlayer = newPlayer

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, newPlayer)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    // Trusted controllers include VESPER itself, Android system media controls,
                    // and apps holding Android's media-control privilege. Untrusted apps cannot
                    // attach to this exported service and issue playback commands.
                    return if (controller.isTrusted) {
                        super.onConnect(session, controller)
                    } else {
                        MediaSession.ConnectionResult.reject()
                    }
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        activeExoPlayer = null
        audioEffectController.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }
}
