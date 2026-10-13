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
import com.recai.app.data.model.Conversation
import com.recai.app.data.model.RecordingMetadata
import com.recai.app.ui.theme.RecAITheme
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

sealed interface ConversationsUiState {
    data object Loading : ConversationsUiState
    data class Success(val conversations: List<Conversation>) : ConversationsUiState
    data object Inaccessible : ConversationsUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    folderName: String,
    uiState: ConversationsUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onConversationClick: (Conversation) -> Unit = {},
    onResetFolderClick: () -> Unit = {}
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
                actions = {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Refresh recordings"
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
                is ConversationsUiState.Loading -> {
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

                is ConversationsUiState.Inaccessible -> {
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

                is ConversationsUiState.Success -> {
                    if (uiState.conversations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No conversations found",
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
                                items = uiState.conversations,
                                key = { it.id }
                            ) { conversation ->
                                ConversationCardItem(
                                    conversation = conversation,
                                    onClick = { onConversationClick(conversation) }
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
fun ConversationCardItem(
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val callerName = conversation.callerName ?: "Unknown caller"
    val phoneNumber = conversation.phoneNumber
    val countText = if (conversation.recordingCount == 1) {
        "1 recording"
    } else {
        "${conversation.recordingCount} recordings"
    }

    val latestRecording = conversation.latestRecording
    val latestDateText = formatLatestDate(
        latestRecording?.metadata?.recordingDateTime,
        latestRecording?.lastModified ?: 0L
    )

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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_audio_file),
                contentDescription = "Conversation",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Line 1: Caller Name
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Line 2: Phone Number (omitted if null)
                if (!phoneNumber.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Line 3: Number of recordings
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                // Line 4: Latest recording date/time
                if (latestDateText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Latest: $latestDateText",
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

private fun formatLatestDate(dateTime: LocalDateTime?, lastModifiedFallback: Long): String {
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

@Preview(name = "Conversations Screen Preview", showBackground = true)
@Composable
fun ConversationsScreenPreview() {
    RecAITheme {
        ConversationsScreen(
            folderName = "call_rec",
            uiState = ConversationsUiState.Success(
                listOf(
                    Conversation(
                        id = "phone:00919699177117",
                        phoneNumber = "00919699177117",
                        callerName = "Darshan Roommate",
                        recordings = listOf(
                            AudioFile(
                                name = "Darshan(00919699177117)_20261013205500.mp3",
                                uri = null,
                                mimeType = "audio/mpeg",
                                size = 129000L,
                                lastModified = 1728800000000L,
                                metadata = RecordingMetadata(
                                    callerName = "Darshan Roommate",
                                    phoneNumber = "00919699177117",
                                    recordingDateTime = LocalDateTime.of(2026, 10, 13, 20, 55),
                                    originalFileName = "Darshan(00919699177117)_20261013205500.mp3"
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
