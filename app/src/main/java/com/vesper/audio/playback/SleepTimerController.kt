package com.vesper.audio.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0L,
    val isEndOfTrack: Boolean = false,
    val initialMinutes: Int = 0,
    val fadeOut: Boolean = true
) {
    val formattedRemaining: String
        get() {
            if (!isActive) return "Off"
            if (isEndOfTrack) return "End of track"
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
}

class SleepTimerController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _timerState = MutableStateFlow(SleepTimerState())
    val timerState: StateFlow<SleepTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var onFinishCallback: (() -> Unit)? = null

    fun startTimer(
        minutes: Int,
        fadeOut: Boolean = true,
        onTimerFinished: () -> Unit
    ) {
        cancelTimer()
        if (minutes <= 0) return

        onFinishCallback = onTimerFinished
        val totalSeconds = minutes * 60L
        _timerState.value = SleepTimerState(
            isActive = true,
            remainingSeconds = totalSeconds,
            isEndOfTrack = false,
            initialMinutes = minutes,
            fadeOut = fadeOut
        )

        timerJob = scope.launch {
            var currentRemaining = totalSeconds
            while (isActive && currentRemaining > 0) {
                delay(1000L)
                currentRemaining--
                _timerState.value = _timerState.value.copy(remainingSeconds = currentRemaining)
            }
            if (isActive && currentRemaining <= 0) {
                _timerState.value = SleepTimerState(isActive = false)
                onFinishCallback?.invoke()
            }
        }
    }

    fun startEndOfTrackTimer(onTimerFinished: () -> Unit) {
        cancelTimer()
        onFinishCallback = onTimerFinished
        _timerState.value = SleepTimerState(
            isActive = true,
            remainingSeconds = 0L,
            isEndOfTrack = true,
            initialMinutes = 0,
            fadeOut = true
        )
    }

    /**
     * Called by the player client when a track transition occurs,
     * triggering sleep if "End of track" was selected.
     */
    fun onTrackEnded() {
        if (_timerState.value.isActive && _timerState.value.isEndOfTrack) {
            _timerState.value = SleepTimerState(isActive = false)
            onFinishCallback?.invoke()
        }
    }

    fun addMinutes(additionalMinutes: Int) {
        val current = _timerState.value
        if (!current.isActive) {
            startTimer(additionalMinutes, fadeOut = true, onTimerFinished = onFinishCallback ?: {})
            return
        }
        val newSeconds = current.remainingSeconds + (additionalMinutes * 60L)
        _timerState.value = current.copy(remainingSeconds = newSeconds)
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = SleepTimerState(isActive = false)
    }
}
