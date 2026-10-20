package com.recai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recai.app.R
import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.RecordingMetadata
import com.recai.app.data.parser.PhoneNumberFormatter
import com.recai.app.ui.theme.RecAITheme
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

sealed interface FileBrowserUiState {
    data object Loading : FileBrowserUiState
    data class Success(val files: List<AudioFile>) : FileBrowserUiState
    data object Inaccessible : FileBrowserUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    folderName: String,
    uiState: FileBrowserUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onResetFolderClick: () -> Unit = {},
    onRecordingClick: (AudioFile) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folderName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is FileBrowserUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scanning recordings...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is FileBrowserUiState.Inaccessible -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Folder is no longer accessible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onResetFolderClick) {
                            Text("Select Another Folder")
                        }
                    }
                }

                is FileBrowserUiState.Success -> {
                    if (uiState.files.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No audio recordings found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = uiState.files,
                                key = { it.uri.toString() }
                            ) { audioFile ->
                                AudioFileItem(
                                    audioFile = audioFile,
                                    onClick = { onRecordingClick(audioFile) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioFileItem(
    audioFile: AudioFile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val metadata = audioFile.metadata
    val titleText = metadata?.callerName ?: "Unknown caller"
    val phoneNumber = metadata?.phoneNumber
    val dateTimeText = formatRecordingDateTime(metadata?.recordingDateTime, audioFile.lastModified)

    val extension = audioFile.name.substringAfterLast('.', "").uppercase(Locale.ROOT)
    val formattedSize = formatFileSize(audioFile.size)
    val fileDetailsText = buildList {
        if (extension.isNotBlank()) add(extension)
        if (formattedSize.isNotBlank()) add(formattedSize)
    }.joinToString(" • ")

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_audio_file),
                contentDescription = "Audio File",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Line 1: Caller Name or Fallback
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Line 2: Phone number (omitted if null)
                if (!phoneNumber.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = PhoneNumberFormatter.format(phoneNumber),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Line 3: Date & Time (parsed recording timestamp or fallback to file modified date)
                if (dateTimeText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateTimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Line 4: Format & File Size (e.g. MP3 • 129 KB)
                if (fileDetailsText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = fileDetailsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
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

@Preview(name = "Success State with Metadata", showBackground = true)
@Composable
fun FileBrowserPreviewSuccess() {
    RecAITheme {
        FileBrowserScreen(
            folderName = "call_rec",
            uiState = FileBrowserUiState.Success(
                listOf(
                    AudioFile(
                        name = "Darshan Roommate(0091XXXXXXXXXX)_20261013200200.mp3",
                        uri = android.net.Uri.EMPTY,
                        mimeType = "audio/mpeg",
                        size = 132096L,
                        lastModified = 1728800000000L,
                        metadata = RecordingMetadata(
                            callerName = "Darshan Roommate",
                            phoneNumber = "0091XXXXXXXXXX",
                            recordingDateTime = LocalDateTime.of(2026, 10, 13, 20, 2),
                            originalFileName = "Darshan Roommate(0091XXXXXXXXXX)_20261013200200.mp3"
                        )
                    ),
                    AudioFile(
                        name = "Recording_20261013153000.wav",
                        uri = android.net.Uri.EMPTY,
                        mimeType = "audio/wav",
                        size = 31200000L,
                        lastModified = 1728700000000L,
                        metadata = RecordingMetadata(
                            callerName = "Recording",
                            phoneNumber = null,
                            recordingDateTime = LocalDateTime.of(2026, 10, 13, 15, 30),
                            originalFileName = "Recording_20261013153000.wav"
                        )
                    )
                )
            )
        )
    }
}

@Preview(name = "Empty State", showBackground = true)
@Composable
fun FileBrowserPreviewEmpty() {
    RecAITheme {
        FileBrowserScreen(
            folderName = "call_rec",
            uiState = FileBrowserUiState.Success(emptyList())
        )
    }
}
