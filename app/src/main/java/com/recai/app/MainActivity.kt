package com.recai.app

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.recai.app.data.model.AudioFile
import com.recai.app.data.model.Conversation
import com.recai.app.data.parser.PhoneNumberMatcher
import com.recai.app.data.model.SortOrder
import com.recai.app.data.storage.ConversationFilterHelper
import com.recai.app.data.storage.FileRepository
import com.recai.app.data.storage.FolderPreferences
import com.recai.app.ui.screens.ConversationDetailScreen
import com.recai.app.ui.screens.ConversationsScreen
import com.recai.app.ui.screens.ConversationsUiState
import com.recai.app.ui.screens.HomeScreen
import com.recai.app.ui.screens.RecordingDetailScreen
import com.recai.app.ui.screens.TrackingScreen
import com.recai.app.ui.theme.RecAITheme
import com.recai.app.ui.util.RecordingNavigationHelper
import kotlinx.coroutines.launch

private enum class Screen {
    HOME,
    CONVERSATIONS,
    CONVERSATION_DETAIL,
    RECORDING_DETAIL,
    TRACKING
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
        const val NAV_TARGET_CONVERSATION_DETAIL = "CONVERSATION_DETAIL"
        const val NAV_TARGET_CONVERSATIONS = "CONVERSATIONS"
    }

    data class NotificationNavTarget(
        val conversationId: String?,
        val phoneNumber: String?,
        val navigateTo: String?
    )

    private val navTargetState = mutableStateOf<NotificationNavTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        extractIntentExtras(intent)

        setContent {
            RecAITheme {
                val context = LocalContext.current
                val folderPreferences = remember { FolderPreferences(context) }
                val fileRepository = remember { FileRepository(context) }
                val coroutineScope = rememberCoroutineScope()

                // State
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
                var selectedAudioFile by remember { mutableStateOf<AudioFile?>(null) }
                var conversationSortOrder by remember { mutableStateOf(SortOrder.NEWEST_FIRST) }
                val storedUriString by folderPreferences.folderUriFlow.collectAsState(initial = null)
                var displayFolderName by remember { mutableStateOf<String?>(null) }
                var conversationsState by remember { mutableStateOf<ConversationsUiState>(ConversationsUiState.Loading) }

                // Call screening role state
                var isCallScreeningRoleHeld by remember {
                    mutableStateOf(checkCallScreeningRoleHeld())
                }

                // Notifications permission state
                var isNotificationsAllowed by remember {
                    mutableStateOf(checkNotificationPermissionGranted())
                }

                // Contacts permission state
                var isContactsPermissionAllowed by remember {
                    mutableStateOf(checkContactsPermissionGranted())
                }

                // Call screening role launcher
                val roleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    isCallScreeningRoleHeld = checkCallScreeningRoleHeld()
                }

                // Notification permission launcher for Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    isNotificationsAllowed = isGranted
                }

                // Contacts permission launcher
                val contactsPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    isContactsPermissionAllowed = isGranted
                }

                // One-time prompt for notifications on launch if not granted (Android 13+)
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationsAllowed) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Check stored URI accessibility whenever it changes
                LaunchedEffect(storedUriString) {
                    val uriString = storedUriString
                    if (uriString != null) {
                        try {
                            val uri = Uri.parse(uriString)
                            val name = folderPreferences.getAccessibleFolderName(uri)
                            if (name != null) {
                                displayFolderName = name
                            } else {
                                displayFolderName = null
                                folderPreferences.clearFolderUri()
                                currentScreen = Screen.HOME
                                selectedConversation = null
                                selectedAudioFile = null
                            }
                        } catch (e: Exception) {
                            displayFolderName = null
                            folderPreferences.clearFolderUri()
                            currentScreen = Screen.HOME
                            selectedConversation = null
                            selectedAudioFile = null
                        }
                    } else {
                        displayFolderName = null
                        currentScreen = Screen.HOME
                        selectedConversation = null
                        selectedAudioFile = null
                    }
                }

                // Function to load and group recordings into conversations
                fun loadConversations(onLoaded: ((List<Conversation>) -> Unit)? = null) {
                    val uriString = storedUriString ?: return
                    val uri = Uri.parse(uriString)
                    conversationsState = ConversationsUiState.Loading
                    coroutineScope.launch {
                        val result = fileRepository.getConversations(uri)
                        result.fold(
                            onSuccess = { convList ->
                                conversationsState = ConversationsUiState.Success(convList)

                                // Handle intent deep-linking to conversation or conversations list
                                val deepTarget = navTargetState.value
                                if (deepTarget != null) {
                                    if (deepTarget.navigateTo == NAV_TARGET_CONVERSATIONS) {
                                        currentScreen = Screen.CONVERSATIONS
                                    } else {
                                        val matched = convList.firstOrNull { conv ->
                                            (deepTarget.conversationId != null && conv.id == deepTarget.conversationId) ||
                                                    (deepTarget.phoneNumber != null && PhoneNumberMatcher.matches(conv.phoneNumber, deepTarget.phoneNumber))
                                        }
                                        if (matched != null) {
                                            selectedConversation = matched
                                            currentScreen = Screen.CONVERSATION_DETAIL
                                        } else {
                                            currentScreen = Screen.CONVERSATIONS
                                        }
                                    }
                                    navTargetState.value = null
                                } else {
                                    val currentConvId = selectedConversation?.id
                                    if (currentConvId != null) {
                                        selectedConversation = convList.firstOrNull { it.id == currentConvId }
                                    }
                                }
                                onLoaded?.invoke(convList)
                            },
                            onFailure = {
                                conversationsState = ConversationsUiState.Inaccessible
                                folderPreferences.clearFolderUri()
                                displayFolderName = null
                            }
                        )
                    }
                }

                // Handle deep link when app is opened from notification
                LaunchedEffect(storedUriString, navTargetState.value) {
                    val deepTarget = navTargetState.value
                    if (storedUriString != null && deepTarget != null) {
                        loadConversations()
                    }
                }

                // Activity Result launcher for ACTION_OPEN_DOCUMENT_TREE
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                            val name = folderPreferences.getAccessibleFolderName(uri)
                            if (name != null) {
                                displayFolderName = name
                                coroutineScope.launch {
                                    folderPreferences.saveFolderUri(uri.toString())
                                }
                            }
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }
                    }
                }

                when (currentScreen) {
                    Screen.HOME -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            HomeScreen(
                                selectedFolderName = displayFolderName,
                                isCallScreeningRoleHeld = isCallScreeningRoleHeld,
                                isNotificationsAllowed = isNotificationsAllowed,
                                isContactsPermissionAllowed = isContactsPermissionAllowed,
                                modifier = Modifier.padding(innerPadding),
                                onSelectFolderClick = {
                                    folderPickerLauncher.launch(null)
                                },
                                onOpenFolderClick = {
                                    loadConversations()
                                    currentScreen = Screen.CONVERSATIONS
                                },
                                onRequestCallScreeningRole = {
                                    requestCallScreeningRole(roleLauncher::launch)
                                },
                                onRequestContactsPermission = {
                                    contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onManageTrackingClick = {
                                    if (conversationsState !is ConversationsUiState.Success && storedUriString != null) {
                                        loadConversations()
                                    }
                                    currentScreen = Screen.TRACKING
                                }
                            )
                        }
                    }

                    Screen.CONVERSATIONS -> {
                        BackHandler {
                            currentScreen = Screen.HOME
                        }

                        ConversationsScreen(
                            folderName = displayFolderName ?: "Recordings",
                            uiState = conversationsState,
                            onBackClick = {
                                currentScreen = Screen.HOME
                            },
                            onRefreshClick = {
                                loadConversations()
                            },
                            onConversationClick = { conversation ->
                                selectedConversation = conversation
                                currentScreen = Screen.CONVERSATION_DETAIL
                            },
                            onResetFolderClick = {
                                currentScreen = Screen.HOME
                                folderPickerLauncher.launch(null)
                            }
                        )
                    }

                    Screen.CONVERSATION_DETAIL -> {
                        val conversation = selectedConversation
                        if (conversation != null) {
                            BackHandler {
                                currentScreen = Screen.CONVERSATIONS
                            }

                            ConversationDetailScreen(
                                conversation = conversation,
                                onBackClick = {
                                    currentScreen = Screen.CONVERSATIONS
                                },
                                onRecordingClick = { recording, sortOrder ->
                                    selectedAudioFile = recording
                                    conversationSortOrder = sortOrder
                                    currentScreen = Screen.RECORDING_DETAIL
                                }
                            )
                        } else {
                            currentScreen = Screen.CONVERSATIONS
                        }
                    }

                    Screen.RECORDING_DETAIL -> {
                        val audio = selectedAudioFile
                        val conversation = selectedConversation
                        if (audio != null && conversation != null) {
                            val orderedRecordings = ConversationFilterHelper.sortRecordings(
                                recordings = conversation.recordings,
                                sortOrder = conversationSortOrder
                            )

                            BackHandler {
                                currentScreen = Screen.CONVERSATION_DETAIL
                            }

                            RecordingDetailScreen(
                                audioFile = audio,
                                recordings = orderedRecordings,
                                onBackClick = {
                                    currentScreen = Screen.CONVERSATION_DETAIL
                                },
                                onManageTrackingClick = {
                                    currentScreen = Screen.TRACKING
                                },
                                onNavigateToRecording = { targetAudio ->
                                    selectedAudioFile = targetAudio
                                },
                                onRecordingDeleted = { deleted ->
                                    val target = RecordingNavigationHelper.findTargetAfterDeletion(
                                        recordings = orderedRecordings,
                                        deletedFile = deleted
                                    )
                                    val remaining = conversation.recordings.filterNot {
                                        (it.uri != null && it.uri == deleted.uri) || it.name == deleted.name
                                    }
                                    val updatedConv = conversation.copy(recordings = remaining)
                                    selectedConversation = updatedConv

                                    // Update in-memory conversationsState
                                    val currentState = conversationsState
                                    if (currentState is ConversationsUiState.Success) {
                                        val updatedConversations = currentState.conversations.map {
                                            if (it.id == updatedConv.id) updatedConv else it
                                        }.filter { it.recordingCount > 0 }
                                        conversationsState = ConversationsUiState.Success(updatedConversations)
                                    }

                                    if (target != null) {
                                        selectedAudioFile = target
                                    } else {
                                        selectedAudioFile = null
                                        currentScreen = Screen.CONVERSATION_DETAIL
                                    }

                                    // Refresh from SAF in background
                                    storedUriString?.let { uriStr ->
                                        coroutineScope.launch {
                                            val res = fileRepository.getConversations(Uri.parse(uriStr))
                                            res.getOrNull()?.let { refreshed ->
                                                conversationsState = ConversationsUiState.Success(refreshed)
                                                selectedConversation = refreshed.firstOrNull { it.id == updatedConv.id }
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            currentScreen = Screen.CONVERSATION_DETAIL
                        }
                    }

                    Screen.TRACKING -> {
                        BackHandler {
                            currentScreen = Screen.HOME
                        }

                        val currentList = when (val state = conversationsState) {
                            is ConversationsUiState.Success -> state.conversations
                            else -> emptyList()
                        }

                        TrackingScreen(
                            conversations = currentList,
                            onBackClick = {
                                currentScreen = Screen.HOME
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission and role checks when user returns to app from system settings
        // (state is handled inside setContent or on next recomposition)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentExtras(intent)
    }

    private fun extractIntentExtras(intent: Intent?) {
        val convId = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        val phone = intent?.getStringExtra(EXTRA_PHONE_NUMBER)
        val navTo = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        if (convId != null || phone != null || navTo != null) {
            navTargetState.value = NotificationNavTarget(
                conversationId = convId,
                phoneNumber = phone,
                navigateTo = navTo
            )
        }
    }

    private fun checkCallScreeningRoleHeld(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun checkNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkContactsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCallScreeningRole(launcher: (Intent) -> Unit) {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            launcher(intent)
        }
    }
}