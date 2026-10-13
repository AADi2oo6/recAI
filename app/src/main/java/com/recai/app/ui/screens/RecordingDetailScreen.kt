package com.recai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recai.app.R
import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.RecordingMetadata
import com.recai.app.ui.player.AudioPlayerController
import com.recai.app.ui.theme.RecAITheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(
    audioFile: AudioFile,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Controller lifecycle bound to screen
    val playerController = remember {
        AudioPlayerController(context, coroutineScope)
    }

    DisposableEffect(audioFile.uri) {
        playerController.prepare(audioFile.uri)
        onDispose {
            playerController.release()
        }
    }

    val playerState by playerController.playerState.collectAsState()

    // Manage seeking state while user is dragging slider
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var draggingProgress by remember { mutableFloatStateOf(0f) }

    val metadata = audioFile.metadata
    val callerName = metadata?.callerName ?: "Unknown caller"
    val phoneNumber = metadata?.phoneNumber
    val dateTimeText = formatRecordingDateTime(metadata?.recordingDateTime, audioFile.lastModified)
    val extension = audioFile.name.substringAfterLast('.', "").uppercase(Locale.ROOT)
    val fileSizeText = formatFileSize(audioFile.size)
    val fileFormatAndSize = buildList {
        if (extension.isNotBlank()) add(extension)
        if (fileSizeText.isNotBlank()) add(fileSizeText)
    }.joinToString(" • ")

    // Surface playback errors via Snackbar
    LaunchedEffect(playerState.errorMessage) {
        val error = playerState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(message = error)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recording",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = callerName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!phoneNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (dateTimeText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = dateTimeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (fileFormatAndSize.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = fileFormatAndSize,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Playback Error alert (if any)
            if (playerState.errorMessage != null) {
                Text(
                    text = playerState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Play / Pause Button
            FilledIconButton(
                onClick = { playerController.togglePlayPause() },
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (playerState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            id = if (playerState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Progress Slider
            val duration = playerState.durationMs
            val currentPos = playerState.currentPositionMs

            val sliderValue = when {
                duration <= 0L -> 0f
                isUserDraggingSlider -> draggingProgress
                else -> (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }

            Slider(
                value = sliderValue,
                onValueChange = { newProgress ->
                    isUserDraggingSlider = true
                    draggingProgress = newProgress
                },
                onValueChangeFinished = {
                    if (duration > 0L) {
                        val seekTargetMs = (draggingProgress * duration).toLong()
                        playerController.seekTo(seekTargetMs)
                    }
                    isUserDraggingSlider = false
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps: Current Position vs Total Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayCurrentMs = if (isUserDraggingSlider && duration > 0L) {
                    (draggingProgress * duration).toLong()
                } else {
                    currentPos
                }
                Text(
                    text = formatTime(displayCurrentMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Analyse Recording Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Analysis coming soon")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Analyse recording",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    if (timeMs <= 0L) return "0:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

private fun formatRecordingDateTime(dateTime: LocalDateTime?, lastModifiedFallback: Long): String {
    if (dateTime != null) {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.getDefault())
        return dateTime.format(formatter)
    }
    if (lastModifiedFallback > 0) {
        val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return formatter.format(Date(lastModifiedFallback))
    }
    return ""
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

@Preview(name = "Recording Detail Preview", showBackground = true)
@Composable
fun RecordingDetailPreview() {
    RecAITheme {
        RecordingDetailScreen(
            audioFile = AudioFile(
                name = "Darshan Roommate(00919699177117)_20261013202700.mp3",
                uri = android.net.Uri.EMPTY,
                mimeType = "audio/mpeg",
                size = 132096L,
                lastModified = 1728800000000L,
                metadata = RecordingMetadata(
                    callerName = "Darshan Roommate",
                    phoneNumber = "00919699177117",
                    recordingDateTime = LocalDateTime.of(2026, 10, 13, 20, 27),
                    originalFileName = "Darshan Roommate(00919699177117)_20261013202700.mp3"
                )
            )
        )
    }
}
