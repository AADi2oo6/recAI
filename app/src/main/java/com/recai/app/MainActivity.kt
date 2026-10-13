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
import com.recai.app.data.storage.FileRepository
import com.recai.app.data.storage.FolderPreferences
import com.recai.app.ui.screens.ConversationDetailScreen
import com.recai.app.ui.screens.ConversationsScreen
import com.recai.app.ui.screens.ConversationsUiState
import com.recai.app.ui.screens.HomeScreen
import com.recai.app.ui.screens.RecordingDetailScreen
import com.recai.app.ui.theme.RecAITheme
import kotlinx.coroutines.launch

private enum class Screen {
    HOME,
    CONVERSATIONS,
    CONVERSATION_DETAIL,
    RECORDING_DETAIL
}

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }

    private var targetConversationId: String? = null
    private var targetPhoneNumber: String? = null

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
                val storedUriString by folderPreferences.folderUriFlow.collectAsState(initial = null)
                var displayFolderName by remember { mutableStateOf<String?>(null) }
                var conversationsState by remember { mutableStateOf<ConversationsUiState>(ConversationsUiState.Loading) }

                // Call screening role state
                var isCallScreeningRoleHeld by remember {
                    mutableStateOf(checkCallScreeningRoleHeld())
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
                ) { /* Granted / Denied handled gracefully */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
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

                                // Handle intent deep-linking to conversation
                                val deepConvId = targetConversationId
                                val deepPhone = targetPhoneNumber
                                if (deepConvId != null || deepPhone != null) {
                                    val matched = convList.firstOrNull { conv ->
                                        conv.id == deepConvId ||
                                                (deepPhone != null && PhoneNumberMatcher.matches(conv.phoneNumber, deepPhone))
                                    }
                                    if (matched != null) {
                                        selectedConversation = matched
                                        currentScreen = Screen.CONVERSATION_DETAIL
                                    } else {
                                        currentScreen = Screen.CONVERSATIONS
                                    }
                                    targetConversationId = null
                                    targetPhoneNumber = null
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
                LaunchedEffect(storedUriString, targetConversationId, targetPhoneNumber) {
                    if (storedUriString != null && (targetConversationId != null || targetPhoneNumber != null)) {
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
                                onRecordingClick = { recording ->
                                    selectedAudioFile = recording
                                    currentScreen = Screen.RECORDING_DETAIL
                                }
                            )
                        } else {
                            currentScreen = Screen.CONVERSATIONS
                        }
                    }

                    Screen.RECORDING_DETAIL -> {
                        val audio = selectedAudioFile
                        if (audio != null) {
                            BackHandler {
                                currentScreen = Screen.CONVERSATION_DETAIL
                            }

                            RecordingDetailScreen(
                                audioFile = audio,
                                onBackClick = {
                                    currentScreen = Screen.CONVERSATION_DETAIL
                                }
                            )
                        } else {
                            currentScreen = Screen.CONVERSATION_DETAIL
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentExtras(intent)
    }

    private fun extractIntentExtras(intent: Intent?) {
        targetConversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        targetPhoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER)
    }

    private fun checkCallScreeningRoleHeld(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestCallScreeningRole(launcher: (Intent) -> Unit) {
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            launcher(intent)
        }
    }
}