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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    isNotificationsAllowed: Boolean,
    isContactsPermissionAllowed: Boolean,
    modifier: Modifier = Modifier,
    onSelectFolderClick: () -> Unit = {},
    onOpenFolderClick: () -> Unit = {},
    onRequestCallScreeningRole: () -> Unit = {},
    onRequestContactsPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onManageTrackingClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
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

            Spacer(modifier = Modifier.height(32.dp))

            // Select Folder Button
            Button(
                onClick = onSelectFolderClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Open Folder Button
                    FilledTonalButton(
                        onClick = onOpenFolderClick,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Open Folder",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Manage Tracking Button
                    OutlinedButton(
                        onClick = onManageTrackingClick,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Manage Tracking",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Manage Tracking Button even when no folder is selected
                OutlinedButton(
                    onClick = onManageTrackingClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Manage Tracking",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Call Detection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Call Detection Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Call Screening Role
                    StatusItemRow(
                        label = "Call screening role",
                        statusText = if (isCallScreeningRoleHeld) "Enabled" else "Not enabled",
                        isOk = isCallScreeningRoleHeld
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Notifications
                    StatusItemRow(
                        label = "Notifications",
                        statusText = if (isNotificationsAllowed) "Allowed" else "Not allowed",
                        isOk = isNotificationsAllowed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Contacts Permission
                    StatusItemRow(
                        label = "Contacts permission",
                        statusText = if (isContactsPermissionAllowed) "Allowed" else "Not allowed",
                        isOk = isContactsPermissionAllowed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Recording Folder
                    StatusItemRow(
                        label = "Recording folder",
                        statusText = if (selectedFolderName != null) "Selected" else "Not selected",
                        isOk = selectedFolderName != null
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons for missing items
                    if (!isCallScreeningRoleHeld) {
                        OutlinedButton(
                            onClick = onRequestCallScreeningRole,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Call Detection")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!isContactsPermissionAllowed) {
                        OutlinedButton(
                            onClick = onRequestContactsPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Contacts Access")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!isNotificationsAllowed) {
                        OutlinedButton(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow Notifications")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusItemRow(
    label: String,
    statusText: String,
    isOk: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (isOk) "✓ $statusText" else "✗ $statusText",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Preview(name = "HomeScreen Preview All Allowed", showBackground = true)
@Composable
fun HomeScreenPreviewAllAllowed() {
    RecAITheme(darkTheme = false) {
        HomeScreen(
            selectedFolderName = "call_rec",
            isCallScreeningRoleHeld = true,
            isNotificationsAllowed = true,
            isContactsPermissionAllowed = true
        )
    }
}

@Preview(name = "HomeScreen Preview Missing Permissions", showBackground = true)
@Composable
fun HomeScreenPreviewMissingPermissions() {
    RecAITheme(darkTheme = false) {
        HomeScreen(
            selectedFolderName = null,
            isCallScreeningRoleHeld = false,
            isNotificationsAllowed = false,
            isContactsPermissionAllowed = false
        )
    }
}
