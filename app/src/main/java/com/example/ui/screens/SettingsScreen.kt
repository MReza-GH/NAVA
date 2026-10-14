package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.NavaPrimary
import com.example.ui.theme.NavaSecondary
import com.example.ui.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onSaveJamendoClientId: (String) -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var clientIdInput by remember(uiState.jamendoClientId) {
        mutableStateOf(uiState.jamendoClientId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
            .testTag("settings_screen")
    ) {
        // Title
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // 1. Language Section
        SettingsCard(title = stringResource(R.string.language_section)) {
            LanguageOptionRow(
                title = stringResource(R.string.language_farsi),
                isSelected = uiState.language == "fa",
                onClick = { onLanguageChange("fa") }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LanguageOptionRow(
                title = stringResource(R.string.language_english),
                isSelected = uiState.language == "en",
                onClick = { onLanguageChange("en") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Appearance / Theme Section
        SettingsCard(title = stringResource(R.string.appearance_section)) {
            ThemeOptionRow(
                title = stringResource(R.string.theme_dark),
                isSelected = uiState.theme == "dark",
                onClick = { onThemeChange("dark") }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ThemeOptionRow(
                title = stringResource(R.string.theme_light),
                isSelected = uiState.theme == "light",
                onClick = { onThemeChange("light") }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            ThemeOptionRow(
                title = stringResource(R.string.theme_system),
                isSelected = uiState.theme == "system",
                onClick = { onThemeChange("system") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Local Library Rescan Section
        SettingsCard(title = stringResource(R.string.library_section)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.rescan_library),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.rescan_library_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = NavaPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Button(
                        onClick = onRescanLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                    ) {
                        Text(stringResource(R.string.rescan_library))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Jamendo Online Service Config
        SettingsCard(title = stringResource(R.string.online_service_section)) {
            Text(
                text = stringResource(R.string.jamendo_guide_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = clientIdInput,
                onValueChange = { clientIdInput = it },
                label = { Text(stringResource(R.string.jamendo_client_id_label)) },
                placeholder = { Text(uiState.effectiveJamendoClientId.ifBlank { "c45a68d0" }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onSaveJamendoClientId(clientIdInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = NavaPrimary)
                ) {
                    Text(stringResource(R.string.jamendo_save_btn))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. About Section
        SettingsCard(title = stringResource(R.string.about_section)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NavaPrimary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.about_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavaPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun LanguageOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = NavaPrimary)
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = NavaPrimary)
        )
    }
}
