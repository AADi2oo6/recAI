package com.recai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.recai.app.data.parser.PhoneNumberFormatter
import com.recai.app.data.storage.TrackingRepository
import com.recai.app.ui.player.AudioPlaybackHelper
import com.recai.app.ui.player.AudioPlayerController
import com.recai.app.ui.theme.RecAITheme
import com.recai.app.ui.util.PhoneActionHelper
import com.recai.app.ui.util.RecordingActionHelper
import com.recai.app.ui.util.RecordingNavigationHelper
import com.recai.app.ui.util.WhatsAppLaunchResult
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
    recordings: List<AudioFile> = listOf(audioFile),
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onManageTrackingClick: () -> Unit = {},
    onNavigateToRecording: (AudioFile) -> Unit = {},
    onRecordingDeleted: ((deletedFile: AudioFile) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Controller lifecycle bound to screen
    val playerController = remember {
        AudioPlayerController(context, coroutineScope)
    }

    DisposableEffect(audioFile.uri) {
        val uri = audioFile.uri
        if (uri != null) {
            playerController.prepare(uri)
        }
        onDispose {
            playerController.release()
        }
    }

    val playerState by playerController.playerState.collectAsState()

    // Tracking state
    val trackingRepo = remember { TrackingRepository(context) }
    val isTracked by trackingRepo.isTrackingEnabledFlow(audioFile.metadata?.phoneNumber).collectAsState(initial = true)

    // Manage seeking state while user is dragging slider
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var draggingProgress by remember { mutableFloatStateOf(0f) }

    // Playback speed dropdown state
    var isSpeedMenuExpanded by remember { mutableStateOf(false) }

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    val metadata = audioFile.metadata
    val callerName = metadata?.callerName ?: "Unknown caller"
    val phoneNumber = metadata?.phoneNumber
    val formattedPhone = PhoneNumberFormatter.format(phoneNumber)
    val dateTimeText = formatRecordingDateTime(metadata?.recordingDateTime, audioFile.lastModified)
    val extension = audioFile.name.substringAfterLast('.', "").uppercase(Locale.ROOT)
    val fileSizeText = formatFileSize(audioFile.size)
    val fileFormatAndSize = buildList {
        if (extension.isNotBlank()) add(extension)
        if (fileSizeText.isNotBlank()) add(fileSizeText)
    }.joinToString(" • ")

    val originalFilename = metadata?.originalFileName ?: audioFile.name

    // Previous / Next playlist indexing
    val currentIndex = remember(recordings, audioFile) {
        RecordingNavigationHelper.calculateCurrentIndex(recordings, audioFile)
    }
    val hasPrevious = remember(currentIndex) {
        RecordingNavigationHelper.hasPrevious(currentIndex)
    }
    val hasNext = remember(currentIndex, recordings.size) {
        RecordingNavigationHelper.hasNext(currentIndex, recordings.size)
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recording?") },
            text = {
                Text("Are you sure you want to permanently delete this recording?\n\nThis action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        val uri = audioFile.uri
                        val success = RecordingActionHelper.deleteRecording(context, uri)
                        if (success) {
                            playerController.release()
                            onRecordingDeleted?.invoke(audioFile)
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Could not delete recording.")
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Header Metadata Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                            text = formattedPhone,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (dateTimeText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Playback Error Alert (if any)
            if (playerState.errorMessage != null) {
                Text(
                    text = playerState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // 3. Audio Playback Controls
            val duration = playerState.durationMs
            val currentPos = playerState.currentPositionMs

            // Play / Pause / Skip Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -10 Seconds Button
                IconButton(
                    onClick = { playerController.rewind10Seconds() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(
                        text = "↶ 10s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Play / Pause Button
                FilledIconButton(
                    onClick = { playerController.togglePlayPause() },
                    modifier = Modifier.size(68.dp),
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
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // +10 Seconds Button
                IconButton(
                    onClick = { playerController.forward10Seconds() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(
                        text = "10s ↷",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timestamps: 0:07 / 0:19
            val displayCurrentMs = if (isUserDraggingSlider && duration > 0L) {
                (draggingProgress * duration).toLong()
            } else {
                currentPos
            }
            Text(
                text = "${formatTime(displayCurrentMs)} / ${formatTime(duration)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Slider
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

            Spacer(modifier = Modifier.height(4.dp))

            // Playback Speed Selector
            Box {
                OutlinedButton(
                    onClick = { isSpeedMenuExpanded = true },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = AudioPlaybackHelper.formatSpeedLabel(playerState.playbackSpeed),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = isSpeedMenuExpanded,
                    onDismissRequest = { isSpeedMenuExpanded = false }
                ) {
                    AudioPlaybackHelper.AVAILABLE_SPEEDS.forEach { speed ->
                        val isSelected = playerState.playbackSpeed == speed
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isSelected) "✓  ${AudioPlaybackHelper.formatSpeedLabel(speed)}" else "    ${AudioPlaybackHelper.formatSpeedLabel(speed)}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                playerController.setPlaybackSpeed(speed)
                                isSpeedMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 4. Previous / Next Recording Navigation
            if (recordings.size > 1) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasPrevious && currentIndex > 0) {
                                onNavigateToRecording(recordings[currentIndex - 1])
                            }
                        },
                        enabled = hasPrevious,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("← Previous")
                    }

                    Text(
                        text = "${if (currentIndex >= 0) currentIndex + 1 else 1} of ${recordings.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            if (hasNext && currentIndex in 0 until recordings.size - 1) {
                                onNavigateToRecording(recordings[currentIndex + 1])
                            }
                        },
                        enabled = hasNext,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Next →")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Actions Section (Call, Message, WhatsApp, Share, Delete, Add to contacts)
            SectionHeader(title = "Actions")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val hasActionablePhone = PhoneActionHelper.isActionablePhoneNumber(phoneNumber)
                    val isUnknownCaller = hasActionablePhone && !PhoneActionHelper.isKnownCaller(callerName, phoneNumber)

                    if (hasActionablePhone) {
                        // Row 1: Direct communication actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val success = PhoneActionHelper.openDialer(context, phoneNumber)
                                    if (!success) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Unable to open dialer")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Call")
                            }

                            OutlinedButton(
                                onClick = {
                                    val success = PhoneActionHelper.openSms(context, phoneNumber)
                                    if (!success) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Unable to open messaging app")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Message")
                            }

                            OutlinedButton(
                                onClick = {
                                    when (PhoneActionHelper.openWhatsApp(context, phoneNumber)) {
                                        WhatsAppLaunchResult.SUCCESS -> {}
                                        WhatsAppLaunchResult.NOT_INSTALLED -> {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("WhatsApp is not installed.")
                                            }
                                        }
                                        WhatsAppLaunchResult.INVALID_NUMBER -> {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Invalid phone number for WhatsApp.")
                                            }
                                        }
                                        WhatsAppLaunchResult.FAILED -> {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Unable to open WhatsApp.")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WhatsApp")
                            }
                        }
                    }

                    // Row 2: Add to contacts if unknown caller
                    if (isUnknownCaller) {
                        OutlinedButton(
                            onClick = {
                                val success = PhoneActionHelper.openAddContact(context, phoneNumber, callerName)
                                if (!success) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Unable to open Contacts")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add to contacts")
                        }
                    }

                    // Row 3: Share and Delete file actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val success = RecordingActionHelper.shareRecording(context, audioFile)
                                if (!success) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Unable to share recording")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share")
                        }

                        OutlinedButton(
                            onClick = {
                                showDeleteDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Tracking Section
            if (PhoneActionHelper.isActionablePhoneNumber(phoneNumber)) {
                SectionHeader(title = "Tracking")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTracked) "Tracking enabled" else "Tracking disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = onManageTrackingClick
                        ) {
                            Text("Manage tracking")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 7. Recording Information Section
            SectionHeader(title = "Recording information")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    InfoItemRow(label = "Caller", value = callerName)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItemRow(label = "Phone", value = formattedPhone)
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItemRow(
                        label = "Date",
                        value = formatDateOnly(metadata?.recordingDateTime, audioFile.lastModified)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItemRow(
                        label = "Time",
                        value = formatTimeOnly(metadata?.recordingDateTime, audioFile.lastModified)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItemRow(
                        label = "Format",
                        value = extension.ifBlank { "Unknown format" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItemRow(
                        label = "Size",
                        value = fileSizeText.ifBlank { "Size unavailable" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 8. Original File Section
            SectionHeader(title = "Original file")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = originalFilename,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 9. Analyse Recording Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Analysis coming soon")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Analyse recording",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun InfoItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.US)
        return dateTime.format(formatter)
    }
    if (lastModifiedFallback > 0) {
        val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
        return formatter.format(Date(lastModifiedFallback))
    }
    return ""
}

private fun formatDateOnly(dateTime: LocalDateTime?, lastModifiedFallback: Long): String {
    if (dateTime != null) {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
        return dateTime.format(formatter)
    }
    if (lastModifiedFallback > 0) {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US)
        return formatter.format(Date(lastModifiedFallback))
    }
    return "Date unavailable"
}

private fun formatTimeOnly(dateTime: LocalDateTime?, lastModifiedFallback: Long): String {
    if (dateTime != null) {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        return dateTime.format(formatter)
    }
    if (lastModifiedFallback > 0) {
        val formatter = SimpleDateFormat("h:mm a", Locale.US)
        return formatter.format(Date(lastModifiedFallback))
    }
    return "Time unavailable"
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
                uri = null,
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
