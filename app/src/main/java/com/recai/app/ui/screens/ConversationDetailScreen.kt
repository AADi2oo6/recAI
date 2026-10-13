package com.recai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recai.app.R
import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.Conversation
import com.recai.app.data.model.RecordingMetadata
import com.recai.app.ui.theme.RecAITheme
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onRecordingClick: (AudioFile) -> Unit = {}
) {
    val callerName = conversation.callerName ?: "Unknown caller"
    val phoneNumber = conversation.phoneNumber
    val countText = if (conversation.recordingCount == 1) {
        "1 recording"
    } else {
        "${conversation.recordingCount} recordings"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = callerName,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Card with phone number & count
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (!phoneNumber.isNullOrBlank()) {
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // List of audio recordings for this conversation
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = conversation.recordings,
                    key = { it.uri?.toString() ?: it.name }
                ) { recording ->
                    AudioFileItem(
                        audioFile = recording,
                        onClick = { onRecordingClick(recording) }
                    )
                }
            }
        }
    }
}

@Preview(name = "Conversation Detail Preview", showBackground = true)
@Composable
fun ConversationDetailPreview() {
    RecAITheme {
        ConversationDetailScreen(
            conversation = Conversation(
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
    }
}
