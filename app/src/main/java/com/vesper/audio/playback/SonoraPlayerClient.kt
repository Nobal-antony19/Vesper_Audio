package com.vesper.audio.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vesper.audio.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SonoraPlayerClient(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    companion object {
        /**
         * Pure circular next index calculation for manual user Next transport action.
         * Returns null when queue is empty; 0 for single-item queue; wraps last -> 0.
         */
        internal fun nextIndex(currentIndex: Int, size: Int): Int? {
            if (size <= 0) return null
            if (currentIndex < 0 || currentIndex >= size) return 0
            return if (currentIndex + 1 >= size) 0 else currentIndex + 1
        }

        /**
         * Pure circular previous index calculation for manual user Previous transport action.
         * Returns null when queue is empty; 0 for single-item queue; wraps 0 -> last.
         */
        internal fun previousIndex(currentIndex: Int, size: Int): Int? {
            if (size <= 0) return null
            if (currentIndex < 0 || currentIndex >= size) return 0
            return if (currentIndex - 1 < 0) size - 1 else currentIndex - 1
        }
    }

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    // Pending command buffer to eliminate dropped commands during asynchronous connection
    private val pendingCommands = mutableListOf<(MediaController) -> Unit>()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var progressJob: Job? = null
    private val trackMap = mutableMapOf<String, Track>()

    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 4000L

    val sleepTimerController = SleepTimerController(scope)

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, SonoraMediaService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                val mediaController = future.get()
                controller = mediaController
                setupListener(mediaController)

                // Flush pending commands
                val pending = pendingCommands.toList()
                pendingCommands.clear()
                pending.forEach { command ->
                    command(mediaController)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, context.mainExecutor)
    }

    private fun executeOrQueue(command: (MediaController) -> Unit) {
        val player = controller
        if (player != null) {
            command(player)
        } else {
            pendingCommands.add(command)
        }
    }

    private var isTransitioning = false
    private var consecutiveErrorCount = 0

    private fun setupListener(player: MediaController) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    _isBuffering.value = false
                    _playbackError.value = null
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                isTransitioning = false
                consecutiveErrorCount = 0
                val mediaId = mediaItem?.mediaId
                val resolved = mediaId?.let { trackMap[it] }
                if (resolved != null) {
                    _currentTrack.value = resolved
                }
                _durationMs.value = player.duration.coerceAtLeast(resolved?.durationMs ?: 0L)
                _playbackPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                if (!crossfadeEnabled) {
                    player.volume = SonoraMediaService.audioEffectController.preampGainScalar.value
                }
                sleepTimerController.onTrackEnded()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    val dur = player.duration
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                    _playbackPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    _isBuffering.value = false
                    _isPlaying.value = false
                    stopProgressTracker()
                } else if (playbackState == Player.STATE_IDLE) {
                    _isBuffering.value = false
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isTransitioning = false
                _isBuffering.value = false
                consecutiveErrorCount++
                _isPlaying.value = false
                stopProgressTracker()
                val maxSkips = _queue.value.size.coerceIn(1, 3)
                if (consecutiveErrorCount <= maxSkips && _queue.value.size > 1) {
                    isTransitioning = true
                    val curIdx = _queue.value.indexOfFirst { it.id == _currentTrack.value?.id }
                    val nextIdx = nextIndex(curIdx, _queue.value.size) ?: 0
                    val fallbackTrack = _queue.value.getOrNull(nextIdx)
                    if (fallbackTrack != null && fallbackTrack.id != _currentTrack.value?.id) {
                        _currentTrack.value = fallbackTrack
                        _durationMs.value = fallbackTrack.durationMs
                        _playbackPositionMs.value = 0L
                        if (nextIdx in 0 until player.mediaItemCount) {
                            player.seekToDefaultPosition(nextIdx)
                            player.prepare()
                            player.play()
                            return
                        }
                    }
                }
                _playbackError.value = "Unable to play track: ${error.errorCodeName}"
                player.stop()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })

        // Synchronize initial state
        _isPlaying.value = player.isPlaying
        _isBuffering.value = (player.playbackState == Player.STATE_BUFFERING)
        _shuffleMode.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode
        val currentId = player.currentMediaItem?.mediaId
        _currentTrack.value = currentId?.let { trackMap[it] }
    }

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    fun setGaplessPlayback(enabled: Boolean) {
        SonoraMediaService.setGaplessPlayback(enabled)
    }

    fun setCrossfade(enabled: Boolean, durationSec: Int) {
        crossfadeEnabled = enabled
        crossfadeDurationMs = (durationSec.coerceIn(2, 12) * 1000L)
        if (!enabled) {
            val baseScalar = SonoraMediaService.audioEffectController.preampGainScalar.value
            executeOrQueue { it.volume = baseScalar }
        }
    }

    fun applyPreampScalar(scalar: Float) {
        val clamped = scalar.coerceIn(0.25f, 1.0f)
        executeOrQueue { player ->
            if (!crossfadeEnabled) {
                player.volume = clamped
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                controller?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    _playbackPositionMs.value = pos
                    val dur = player.duration
                    val baseScalar = SonoraMediaService.audioEffectController.preampGainScalar.value
                    if (dur > 0) {
                        _durationMs.value = dur
                        if (crossfadeEnabled && player.isPlaying && dur > crossfadeDurationMs * 2) {
                            val timeRemaining = dur - pos
                            if (timeRemaining in 0..crossfadeDurationMs && player.hasNextMediaItem()) {
                                val fraction = (timeRemaining.toFloat() / crossfadeDurationMs.toFloat()).coerceIn(0.08f, 1f) * baseScalar
                                player.volume = fraction
                            } else if (pos < crossfadeDurationMs) {
                                val fraction = (pos.toFloat() / crossfadeDurationMs.toFloat()).coerceIn(0.12f, 1f) * baseScalar
                                player.volume = fraction
                            } else {
                                if (player.volume != baseScalar) player.volume = baseScalar
                            }
                        } else {
                            if (player.volume != baseScalar) player.volume = baseScalar
                        }
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
        controller?.let { player ->
            _playbackPositionMs.value = player.currentPosition.coerceAtLeast(0L)
        }
    }

    private fun buildMediaItem(t: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(t.title)
            .setArtist(t.artist)
            .setAlbumTitle(t.album)
            .setArtworkUri(t.artworkUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(t.id)
            .setUri(t.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun isTrackPlayable(t: Track): Boolean {
        return com.vesper.audio.util.SecurityValidator.isValidMediaUri(t.contentUri)
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        val validQueue = newQueue.filter { isTrackPlayable(it) }.distinctBy { it.id }
        val playableTarget = when {
            isTrackPlayable(track) -> track
            validQueue.isNotEmpty() -> validQueue.first()
            else -> {
                _playbackError.value = "Selected audio file is unavailable or missing."
                _isPlaying.value = false
                _isBuffering.value = false
                return
            }
        }

        val dedupedQueue = if (validQueue.any { it.id == playableTarget.id }) {
            validQueue
        } else {
            listOf(playableTarget) + validQueue
        }

        val previousQueue = _queue.value
        val isSameQueue = previousQueue.size == dedupedQueue.size &&
            previousQueue.indices.all { previousQueue[it].id == dedupedQueue[it].id }

        dedupedQueue.forEach { trackMap[it.id] = it }
        trackMap[playableTarget.id] = playableTarget
        _queue.value = dedupedQueue
        _currentTrack.value = playableTarget
        _isPlaying.value = true
        _durationMs.value = playableTarget.durationMs
        _playbackPositionMs.value = 0L
        _playbackError.value = null
        consecutiveErrorCount = 0

        val targetIndex = dedupedQueue.indexOfFirst { it.id == playableTarget.id }.coerceAtLeast(0)

        executeOrQueue { player ->
            player.volume = SonoraMediaService.audioEffectController.preampGainScalar.value
            if (isSameQueue && player.mediaItemCount == dedupedQueue.size && targetIndex < player.mediaItemCount) {
                // Seek directly within active playlist without rebuilding MediaItems or dropping buffers
                player.seekToDefaultPosition(targetIndex)
                if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    player.prepare()
                }
                player.play()
            } else {
                val mediaItems = dedupedQueue.map { buildMediaItem(it) }
                player.setMediaItems(mediaItems, targetIndex, 0L)
                player.prepare()
                player.play()
            }
            startProgressTracker()
        }
    }

    fun restorePlayback(track: Track, restoredQueue: List<Track>, positionMs: Long, shouldPlay: Boolean) {
        val cleanQueue = restoredQueue.filter { isTrackPlayable(it) }.distinctBy { it.id }.ifEmpty {
            if (isTrackPlayable(track)) listOf(track) else return
        }
        cleanQueue.forEach { trackMap[it.id] = it }
        _queue.value = cleanQueue
        _currentTrack.value = track
        _isPlaying.value = shouldPlay
        _durationMs.value = track.durationMs
        _playbackPositionMs.value = positionMs

        executeOrQueue { player ->
            player.volume = SonoraMediaService.audioEffectController.preampGainScalar.value
            val mediaItems = cleanQueue.map { buildMediaItem(it) }
            val startIndex = cleanQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            player.setMediaItems(mediaItems, startIndex, positionMs)
            player.prepare()
            if (shouldPlay) {
                player.play()
                startProgressTracker()
            }
        }
    }

    fun addToQueue(track: Track) {
        if (!isTrackPlayable(track)) return
        trackMap[track.id] = track
        val q = _queue.value.toMutableList()
        if (q.none { it.id == track.id }) {
            q.add(track)
            _queue.value = q
            executeOrQueue { player ->
                player.addMediaItem(buildMediaItem(track))
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
            }
        }
    }

    fun playNext(track: Track) {
        if (!isTrackPlayable(track)) return
        trackMap[track.id] = track
        val current = _currentTrack.value
        val q = _queue.value.toMutableList()
        val existingIdx = q.indexOfFirst { it.id == track.id }
        if (existingIdx >= 0) {
            q.removeAt(existingIdx)
        }
        val currentIndex = if (current != null) q.indexOfFirst { it.id == current.id } else -1
        val insertIndex = if (currentIndex >= 0 && currentIndex < q.size) currentIndex + 1 else 0

        q.add(insertIndex, track)
        _queue.value = q

        executeOrQueue { player ->
            if (existingIdx in 0 until player.mediaItemCount) {
                player.removeMediaItem(existingIdx)
            }
            val playerIndex = (player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount)
            player.addMediaItem(playerIndex, buildMediaItem(track))
        }
    }

    fun togglePlayPause() {
        val playingNow = !_isPlaying.value
        _isPlaying.value = playingNow
        executeOrQueue { player ->
            player.volume = SonoraMediaService.audioEffectController.preampGainScalar.value
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    player.prepare()
                }
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val pos = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        _playbackPositionMs.value = pos
        executeOrQueue { player ->
            player.volume = SonoraMediaService.audioEffectController.preampGainScalar.value
            player.seekTo(pos)
        }
    }

    private fun jumpToQueueIndex(targetIndex: Int) {
        val currentQueue = _queue.value
        if (targetIndex !in currentQueue.indices) return
        val targetTrack = currentQueue[targetIndex]

        trackMap[targetTrack.id] = targetTrack
        _currentTrack.value = targetTrack
        _isPlaying.value = true
        _durationMs.value = targetTrack.durationMs
        _playbackPositionMs.value = 0L
        _playbackError.value = null
        consecutiveErrorCount = 0

        executeOrQueue { player ->
            player.volume = SonoraMediaService.audioEffectController.preampGainScalar.value
            if (player.mediaItemCount == currentQueue.size && targetIndex < player.mediaItemCount) {
                player.seekToDefaultPosition(targetIndex)
                if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    player.prepare()
                }
                player.play()
            } else {
                val mediaItems = currentQueue.map { buildMediaItem(it) }
                player.setMediaItems(mediaItems, targetIndex, 0L)
                player.prepare()
                player.play()
            }
            startProgressTracker()
        }
    }

    fun skipNext() {
        val currentQueue = queue.value
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == currentTrack.value?.id }
        val targetIndex = nextIndex(currentIndex, currentQueue.size) ?: return
        jumpToQueueIndex(targetIndex)
    }

    fun skipPrevious() {
        val currentQueue = queue.value
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == currentTrack.value?.id }
        if (currentIndex == -1) return

        // Studio Player Behavior: If more than 3 seconds in, restart current track
        if (playbackPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val targetIndex = previousIndex(currentIndex, currentQueue.size) ?: return
        jumpToQueueIndex(targetIndex)
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleMode.value
        _shuffleMode.value = newShuffle
        executeOrQueue { player ->
            player.shuffleModeEnabled = newShuffle
        }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        executeOrQueue { player ->
            player.repeatMode = nextMode
        }
    }

    fun removeQueueItem(index: Int) {
        val q = _queue.value.toMutableList()
        if (index in q.indices) {
            q.removeAt(index)
            _queue.value = q
        }
        executeOrQueue { player ->
            if (index in 0 until player.mediaItemCount) {
                player.removeMediaItem(index)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val q = _queue.value.toMutableList()
        if (fromIndex in q.indices && toIndex in q.indices) {
            val item = q.removeAt(fromIndex)
            q.add(toIndex, item)
            _queue.value = q
        }
        executeOrQueue { player ->
            if (fromIndex in 0 until player.mediaItemCount && toIndex in 0 until player.mediaItemCount) {
                player.moveMediaItem(fromIndex, toIndex)
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentTrack.value = null
        _isPlaying.value = false
        executeOrQueue { player ->
            player.clearMediaItems()
        }
    }

    fun startSleepTimer(minutes: Int, fadeOut: Boolean = true) {
        sleepTimerController.startTimer(minutes, fadeOut) {
            scope.launch {
                val baseScalar = SonoraMediaService.audioEffectController.preampGainScalar.value
                if (fadeOut) {
                    for (step in 10 downTo 1) {
                        executeOrQueue { it.volume = (step / 10f) * baseScalar }
                        delay(300L)
                    }
                }
                executeOrQueue {
                    it.pause()
                    it.volume = baseScalar
                }
            }
        }
    }

    fun startEndOfTrackSleepTimer() {
        sleepTimerController.startEndOfTrackTimer {
            executeOrQueue { it.pause() }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerController.cancelTimer()
        executeOrQueue { it.volume = SonoraMediaService.audioEffectController.preampGainScalar.value }
    }

    fun addSleepTimerMinutes(minutes: Int) {
        sleepTimerController.addMinutes(minutes)
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        pendingCommands.clear()
    }
}
