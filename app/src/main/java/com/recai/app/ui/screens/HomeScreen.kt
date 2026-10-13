package com.recai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.recai.app.ui.theme.RecAITheme

@Composable
fun HomeScreen(
    selectedFolderName: String?,
    isCallScreeningRoleHeld: Boolean,
    modifier: Modifier = Modifier,
    onSelectFolderClick: () -> Unit = {},
    onOpenFolderClick: () -> Unit = {},
    onRequestCallScreeningRole: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title
            Text(
                text = "recAI",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Your AI conversation memory",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Select Folder Button
            Button(
                onClick = onSelectFolderClick,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(50.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder),
                    contentDescription = "Folder Icon",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Folder",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folder Selection Status & Open Folder action
            if (selectedFolderName != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Selected folder",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📁 ",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = selectedFolderName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Open Folder Button
                    FilledTonalButton(
                        onClick = onOpenFolderClick,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Open Folder",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                Text(
                    text = "No folder selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Call Screening Role Setup
            if (!isCallScreeningRoleHeld) {
                OutlinedButton(
                    onClick = onRequestCallScreeningRole,
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(46.dp)
                ) {
                    Text(
                        text = "Enable Call Detection",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Allows recAI to show caller recordings during incoming calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Preview(name = "No Folder Selected", showBackground = true)
@Composable
fun HomeScreenPreviewNoFolder() {
    RecAITheme(darkTheme = false) {
        HomeScreen(selectedFolderName = null, isCallScreeningRoleHeld = false)
    }
}

@Preview(name = "Folder Selected with Role Enabled", showBackground = true)
@Composable
fun HomeScreenPreviewWithFolder() {
    RecAITheme(darkTheme = false) {
        HomeScreen(selectedFolderName = "Recordings", isCallScreeningRoleHeld = true)
    }
}
