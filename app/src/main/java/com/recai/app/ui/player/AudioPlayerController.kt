package com.recai.app.ui.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * State representing audio playback status in the UI.
 */
data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val errorMessage: String? = null
)

/**
 * Pure helper for audio playback boundary calculations and speed formatting.
 */
object AudioPlaybackHelper {
    const val SKIP_INTERVAL_MS = 10_000L
    val AVAILABLE_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    fun calculateRewindPosition(currentMs: Long): Long {
        return (currentMs - SKIP_INTERVAL_MS).coerceAtLeast(0L)
    }

    fun calculateForwardPosition(currentMs: Long, durationMs: Long): Long {
        return (currentMs + SKIP_INTERVAL_MS).coerceAtMost(durationMs.coerceAtLeast(0L))
    }

    fun formatSpeedLabel(speed: Float): String {
        return when (speed) {
            0.5f -> "0.5×"
            0.75f -> "0.75×"
            1.0f -> "1.0×"
            1.25f -> "1.25×"
            1.5f -> "1.5×"
            1.75f -> "1.75×"
            2.0f -> "2.0×"
            else -> "${speed}×"
        }
    }
}

/**
 * Lifecycle-safe player controller managing an ExoPlayer instance.
 * Plays directly from content URIs without filesystem path conversions.
 */
class AudioPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var progressPollingJob: Job? = null

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
            if (isPlaying) {
                startPollingProgress()
            } else {
                stopPollingProgress()
                updatePosition()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playerState.value = _playerState.value.copy(
                        isBuffering = true,
                        durationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }
                Player.STATE_READY -> {
                    val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                    _playerState.value = _playerState.value.copy(
                        isBuffering = false,
                        durationMs = duration,
                        errorMessage = null
                    )
                    updatePosition()
                }
                Player.STATE_ENDED -> {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = _playerState.value.durationMs
                    )
                    stopPollingProgress()
                }
                Player.STATE_IDLE -> {
                    _playerState.value = _playerState.value.copy(isBuffering = false)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val userFriendlyMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Audio file not found or inaccessible."
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "Unsupported audio format."
                else -> "Playback error: ${error.message ?: "Unknown error"}"
            }
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = userFriendlyMessage
            )
            stopPollingProgress()
        }
    }

    /**
     * Initializes the player with the specified content URI.
     */
    fun prepare(contentUri: Uri) {
        release()
        try {
            val player = ExoPlayer.Builder(context.applicationContext).build().apply {
                addListener(playerListener)
                setMediaItem(MediaItem.fromUri(contentUri))
                setPlaybackSpeed(_playerState.value.playbackSpeed)
                prepare()
            }
            exoPlayer = player
        } catch (e: Exception) {
            _playerState.value = _playerState.value.copy(
                errorMessage = "Failed to load audio: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    fun play() {
        val player = exoPlayer ?: return
        if (player.playbackState == Player.STATE_ENDED || _playerState.value.currentPositionMs >= _playerState.value.durationMs) {
            seekTo(0L)
        }
        player.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            // If completed, seek back to start
            if (player.playbackState == Player.STATE_ENDED || _playerState.value.currentPositionMs >= _playerState.value.durationMs) {
                seekTo(0L)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, _playerState.value.durationMs.coerceAtLeast(0L))
        player.seekTo(clamped)
        _playerState.value = _playerState.value.copy(currentPositionMs = clamped)
    }

    /**
     * Skips backward by 10 seconds, stopping at 0.
     */
    fun rewind10Seconds() {
        val target = AudioPlaybackHelper.calculateRewindPosition(_playerState.value.currentPositionMs)
        seekTo(target)
    }

    /**
     * Skips forward by 10 seconds, not exceeding total duration.
     */
    fun forward10Seconds() {
        val target = AudioPlaybackHelper.calculateForwardPosition(
            _playerState.value.currentPositionMs,
            _playerState.value.durationMs
        )
        seekTo(target)
    }

    /**
     * Sets playback speed (e.g. 0.5x, 1.0x, 1.5x, 2.0x) without recreating the player.
     */
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    private fun startPollingProgress() {
        stopPollingProgress()
        progressPollingJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                updatePosition()
                delay(200L)
            }
        }
    }

    private fun stopPollingProgress() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    private fun updatePosition() {
        val current = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
        val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: _playerState.value.durationMs
        _playerState.value = _playerState.value.copy(
            currentPositionMs = current,
            durationMs = duration
        )
    }

    /**
     * Releases player and cancels background polling.
     */
    fun release() {
        stopPollingProgress()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = AudioPlayerState(playbackSpeed = _playerState.value.playbackSpeed)
    }
}

