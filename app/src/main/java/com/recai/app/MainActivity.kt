package com.recai.app

import android.content.Intent
import android.net.Uri
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
import com.recai.app.data.storage.FileRepository
import com.recai.app.data.storage.FolderPreferences
import com.recai.app.ui.screens.FileBrowserScreen
import com.recai.app.ui.screens.FileBrowserUiState
import com.recai.app.ui.screens.HomeScreen
import com.recai.app.ui.theme.RecAITheme
import kotlinx.coroutines.launch

private enum class Screen {
    HOME,
    FILE_BROWSER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecAITheme {
                val context = LocalContext.current
                val folderPreferences = remember { FolderPreferences(context) }
                val fileRepository = remember { FileRepository(context) }
                val coroutineScope = rememberCoroutineScope()

                // State
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                val storedUriString by folderPreferences.folderUriFlow.collectAsState(initial = null)
                var displayFolderName by remember { mutableStateOf<String?>(null) }
                var fileBrowserState by remember { mutableStateOf<FileBrowserUiState>(FileBrowserUiState.Loading) }

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
                                // Permission revoked or folder no longer accessible
                                displayFolderName = null
                                folderPreferences.clearFolderUri()
                                currentScreen = Screen.HOME
                            }
                        } catch (e: Exception) {
                            displayFolderName = null
                            folderPreferences.clearFolderUri()
                            currentScreen = Screen.HOME
                        }
                    } else {
                        displayFolderName = null
                        currentScreen = Screen.HOME
                    }
                }

                // Function to load audio files
                fun loadAudioFiles() {
                    val uriString = storedUriString ?: return
                    val uri = Uri.parse(uriString)
                    fileBrowserState = FileBrowserUiState.Loading
                    coroutineScope.launch {
                        val result = fileRepository.getAudioFiles(uri)
                        result.fold(
                            onSuccess = { files ->
                                fileBrowserState = FileBrowserUiState.Success(files)
                            },
                            onFailure = {
                                fileBrowserState = FileBrowserUiState.Inaccessible
                                folderPreferences.clearFolderUri()
                                displayFolderName = null
                            }
                        )
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
                                modifier = Modifier.padding(innerPadding),
                                onSelectFolderClick = {
                                    folderPickerLauncher.launch(null)
                                },
                                onOpenFolderClick = {
                                    loadAudioFiles()
                                    currentScreen = Screen.FILE_BROWSER
                                }
                            )
                        }
                    }

                    Screen.FILE_BROWSER -> {
                        BackHandler {
                            currentScreen = Screen.HOME
                        }

                        FileBrowserScreen(
                            folderName = displayFolderName ?: "Recordings",
                            uiState = fileBrowserState,
                            onBackClick = {
                                currentScreen = Screen.HOME
                            },
                            onResetFolderClick = {
                                currentScreen = Screen.HOME
                                folderPickerLauncher.launch(null)
                            }
                        )
                    }
                }
            }
        }
    }
}