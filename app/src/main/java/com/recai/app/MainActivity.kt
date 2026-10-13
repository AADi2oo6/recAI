package com.recai.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.recai.app.data.storage.FolderPreferences
import com.recai.app.ui.screens.HomeScreen
import com.recai.app.ui.theme.RecAITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecAITheme {
                val context = LocalContext.current
                val folderPreferences = remember { FolderPreferences(context) }
                val coroutineScope = rememberCoroutineScope()

                // Observe stored URI from DataStore
                val storedUriString by folderPreferences.folderUriFlow.collectAsState(initial = null)
                var displayFolderName by remember { mutableStateOf<String?>(null) }

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
                            }
                        } catch (e: Exception) {
                            displayFolderName = null
                            folderPreferences.clearFolderUri()
                        }
                    } else {
                        displayFolderName = null
                    }
                }

                // Activity Result launcher for ACTION_OPEN_DOCUMENT_TREE
                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    // Handle user cancellation: if uri is null, do nothing and keep existing
                    if (uri != null) {
                        try {
                            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                            // Validate and save
                            val name = folderPreferences.getAccessibleFolderName(uri)
                            if (name != null) {
                                displayFolderName = name
                                coroutineScope.launch {
                                    folderPreferences.saveFolderUri(uri.toString())
                                }
                            }
                        } catch (e: SecurityException) {
                            // In case permission couldn't be persisted
                            e.printStackTrace()
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        selectedFolderName = displayFolderName,
                        modifier = Modifier.padding(innerPadding),
                        onSelectFolderClick = {
                            folderPickerLauncher.launch(null)
                        }
                    )
                }
            }
        }
    }
}