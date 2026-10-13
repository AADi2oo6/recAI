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
    val errorMessage: String? = null
)

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
        exoPlayer?.play()
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
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val clamped = positionMs.coerceIn(0L, _playerState.value.durationMs)
        player.seekTo(clamped)
        _playerState.value = _playerState.value.copy(currentPositionMs = clamped)
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
        _playerState.value = AudioPlayerState()
    }
}
