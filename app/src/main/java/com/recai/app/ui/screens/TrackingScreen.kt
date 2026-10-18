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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.recai.app.data.model.Conversation
import com.recai.app.data.model.TrackingEntry
import com.recai.app.data.parser.PhoneNumberFormatter
import com.recai.app.data.parser.PhoneNumberMatcher
import com.recai.app.data.storage.TrackingRepository
import com.recai.app.ui.theme.RecAITheme
import com.recai.app.ui.util.PhoneActionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    conversations: List<Conversation>,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trackingRepo = remember { TrackingRepository(context) }
    val untrackedSet by trackingRepo.untrackedNumbersFlow.collectAsState(initial = emptySet())
    val untrackedNames by trackingRepo.untrackedCallerNamesFlow.collectAsState(initial = emptyMap())

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Build tracking entries from conversations plus any custom untracked numbers
    val conversationEntries = remember(conversations, untrackedSet) {
        conversations
            .filter { PhoneActionHelper.isActionablePhoneNumber(it.phoneNumber) }
            .map { conv ->
                val norm = PhoneNumberMatcher.normalize(conv.phoneNumber)
                val isTracked = !untrackedSet.contains(norm)
                TrackingEntry(
                    phoneNumber = conv.phoneNumber ?: "",
                    displayName = conv.callerName,
                    isTracked = isTracked,
                    recordingCount = conv.recordingCount
                )
            }
    }

    // Include numbers from untrackedSet that do not have existing recordings
    val allEntries = remember(conversationEntries, untrackedSet, untrackedNames) {
        val convNorms = conversationEntries.map { it.normalizedPhoneNumber }.toSet()
        val extraUntracked = untrackedSet
            .filterNot { it in convNorms }
            .map { norm ->
                TrackingEntry(
                    phoneNumber = norm,
                    displayName = untrackedNames[norm],
                    isTracked = false,
                    recordingCount = 0
                )
            }
        conversationEntries + extraUntracked
    }

    // Filter by search query
    val filteredEntries = remember(allEntries, searchQuery) {
        if (searchQuery.isBlank()) {
            allEntries
        } else {
            val query = searchQuery.trim().lowercase()
            allEntries.filter { entry ->
                entry.displayName?.lowercase()?.contains(query) == true ||
                        entry.phoneNumber.contains(query) ||
                        entry.displayPhoneNumber.lowercase().contains(query) ||
                        entry.normalizedPhoneNumber.contains(query)
            }
        }
    }

    val trackedList = remember(filteredEntries) { filteredEntries.filter { it.isTracked } }
    val untrackedList = remember(filteredEntries) { filteredEntries.filter { !it.isTracked } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tracking",
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
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search tracked callers...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No callers match your search" else "No callers recorded yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Tracked Callers Section
                    if (trackedList.isNotEmpty()) {
                        item(key = "header_tracked") {
                            Text(
                                text = "Tracked callers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(
                            items = trackedList,
                            key = { "tracked_${it.normalizedPhoneNumber}" }
                        ) { entry ->
                            TrackingItemCard(
                                entry = entry,
                                onToggle = { newChecked ->
                                    coroutineScope.launch {
                                        trackingRepo.setTrackingEnabled(
                                            phoneNumber = entry.phoneNumber,
                                            enabled = newChecked,
                                            callerName = entry.displayName
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // 2. Not Tracked Section
                    if (untrackedList.isNotEmpty()) {
                        item(key = "header_untracked") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Not tracked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(
                            items = untrackedList,
                            key = { "untracked_${it.normalizedPhoneNumber}" }
                        ) { entry ->
                            TrackingItemCard(
                                entry = entry,
                                onToggle = { newChecked ->
                                    coroutineScope.launch {
                                        trackingRepo.setTrackingEnabled(
                                            phoneNumber = entry.phoneNumber,
                                            enabled = newChecked,
                                            callerName = entry.displayName
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingItemCard(
    entry: TrackingEntry,
    onToggle: (Boolean) -> Unit
) {
    val callerName = entry.displayName ?: "Unknown caller"
    val countText = when (entry.recordingCount) {
        0 -> "No previous recordings"
        1 -> "1 recording"
        else -> "${entry.recordingCount} recordings"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.displayPhoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.isTracked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            Switch(
                checked = entry.isTracked,
                onCheckedChange = onToggle
            )
        }
    }
}

@Preview(name = "TrackingScreen Preview", showBackground = true)
@Composable
fun TrackingScreenPreview() {
    RecAITheme {
        TrackingScreen(
            conversations = listOf(
                Conversation(
                    id = "1",
                    callerName = "Darshan Roommate",
                    phoneNumber = "00919699177117",
                    recordings = emptyList()
                ),
                Conversation(
                    id = "2",
                    callerName = "Ishwar Tatkal Ta",
                    phoneNumber = "00918830714901",
                    recordings = emptyList()
                )
            )
        )
    }
}
