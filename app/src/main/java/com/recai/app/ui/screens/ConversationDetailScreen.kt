package com.recai.app.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.recai.app.R
import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.Conversation
import com.recai.app.data.model.RecordingMetadata
import com.recai.app.data.model.SortOrder
import com.recai.app.data.parser.PhoneNumberFormatter
import com.recai.app.data.storage.ConversationFilterHelper
import com.recai.app.data.storage.TrackingRepository
import com.recai.app.ui.theme.RecAITheme
import com.recai.app.ui.util.PhoneActionHelper
import com.recai.app.ui.util.WhatsAppLaunchResult
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onRecordingClick: (AudioFile, SortOrder) -> Unit = { _, _ -> }
) {
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentSortOrder by rememberSaveable { mutableStateOf(SortOrder.NEWEST_FIRST) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val callerName = conversation.callerName ?: "Unknown caller"
    val phoneNumber = conversation.phoneNumber

    // Important: Conversation's actual recording count must remain based on full conversation
    val fullRecordingCount = conversation.recordingCount
    val countText = if (fullRecordingCount == 1) {
        "1 recording"
    } else {
        "$fullRecordingCount recordings"
    }

    // Back button closes search mode before leaving screen
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trackingRepo = remember { TrackingRepository(context) }
    val isTracked by trackingRepo.isTrackingEnabledFlow(phoneNumber).collectAsState(initial = true)
    var showRemoveTrackingDialog by remember { mutableStateOf(false) }

    if (showRemoveTrackingDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveTrackingDialog = false },
            title = { Text("Remove from tracking?") },
            text = {
                Text("recAI will stop showing caller notifications for this number.\n\nExisting recordings will not be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveTrackingDialog = false
                        coroutineScope.launch {
                            if (!phoneNumber.isNullOrBlank()) {
                                trackingRepo.setTrackingEnabled(phoneNumber, false, callerName)
                            }
                        }
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveTrackingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search recordings...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = "Close search"
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
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
                    actions = {
                        // Search Button
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = "Search recordings"
                            )
                        }

                        // Sort Button with Dropdown
                        Box {
                            IconButton(onClick = { isSortMenuExpanded = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sort),
                                    contentDescription = "Sort recordings"
                                )
                            }
                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Newest first",
                                            fontWeight = if (currentSortOrder == SortOrder.NEWEST_FIRST) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSortOrder == SortOrder.NEWEST_FIRST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        currentSortOrder = SortOrder.NEWEST_FIRST
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Oldest first",
                                            fontWeight = if (currentSortOrder == SortOrder.OLDEST_FIRST) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentSortOrder == SortOrder.OLDEST_FIRST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        currentSortOrder = SortOrder.OLDEST_FIRST
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Card with formatted phone number, actions & tracking
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
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
                            text = PhoneNumberFormatter.format(phoneNumber),
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

                    // Conversation-level Actions (Call / Message / WhatsApp / Add to contacts)
                    if (PhoneActionHelper.isActionablePhoneNumber(phoneNumber)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
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

                        if (!PhoneActionHelper.isKnownCaller(callerName, phoneNumber)) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                    }

                    // Tracking state & control
                    if (PhoneActionHelper.isActionablePhoneNumber(phoneNumber)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tracking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (isTracked) "● Tracking enabled" else "○ Tracking disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isTracked) {
                                TextButton(
                                    onClick = { showRemoveTrackingDialog = true }
                                ) {
                                    Text("Remove from tracking")
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            trackingRepo.setTrackingEnabled(phoneNumber ?: "", true)
                                        }
                                    }
                                ) {
                                    Text("Add to tracking")
                                }
                            }
                        }
                    }
                }
            }

            // Filter & sort recordings
            val filteredRecordings = ConversationFilterHelper.filterRecordings(
                recordings = conversation.recordings,
                query = searchQuery
            )
            val sortedRecordings = ConversationFilterHelper.sortRecordings(
                recordings = filteredRecordings,
                sortOrder = currentSortOrder
            )

            if (sortedRecordings.isEmpty() && searchQuery.isNotBlank()) {
                // Search yielded no matching recordings
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recordings match your search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching for a different date or keyword.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = sortedRecordings,
                        key = { it.uri?.toString() ?: it.name }
                    ) { recording ->
                        AudioFileItem(
                            audioFile = recording,
                            onClick = { onRecordingClick(recording, currentSortOrder) }
                        )
                    }
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
