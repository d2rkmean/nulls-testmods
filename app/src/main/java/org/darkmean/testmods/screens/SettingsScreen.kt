package org.darkmean.testmods

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    strings:    AppStrings,
    prefs:      android.content.SharedPreferences,
    context:    Context,
    onBack:     () -> Unit,
    onDeleteAll: () -> Unit
) {
    val ctx = LocalContext.current
    var selectedThemeIdx by remember { mutableStateOf(prefs.getInt(AppConfig.PREFS_KEY_THEME, -1)) }
    var repoUrl by remember { mutableStateOf(prefs.getString(AppConfig.PREFS_KEY_REPO, AppConfig.DEFAULT_REPO_URL) ?: AppConfig.DEFAULT_REPO_URL) }
    var modsDir by remember { mutableStateOf(prefs.getString(AppConfig.PREFS_KEY_MODS_DIR, AppConfig.MODS_PATH) ?: AppConfig.MODS_PATH) }
    var showDeleteAllConfirm by remember { mutableStateOf(false) }

    val themeNames = listOf("Blue", "Green", "Yellow", "Red", "Purple", "Orange")

    Scaffold(
        containerColor = ColorBgDeep,
        topBar = {
            Surface(color = ColorBgCard, modifier = Modifier.border(0.5.dp, ThemePrimary.copy(alpha = 0.25f), RoundedCornerShape(0.dp))) {
                TopAppBar(
                    title = { Text(strings.settingsTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = {
                            prefs.edit()
                                .putInt(AppConfig.PREFS_KEY_THEME, selectedThemeIdx)
                                .putString(AppConfig.PREFS_KEY_REPO, repoUrl)
                                .putString(AppConfig.PREFS_KEY_MODS_DIR, modsDir)
                                .apply()
                            Toast.makeText(ctx, strings.settingsSaved, Toast.LENGTH_SHORT).show()
                            onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, null, tint = ThemePrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                SettingsSection(title = strings.settingsTheme) {
                    val allOptions = listOf(strings.settingsThemeAuto) + themeNames
                    allOptions.forEachIndexed { i, name ->
                        val idx   = i - 1
                        val theme = if (idx in allThemes.indices) allThemes[idx] else sessionTheme
                        val isSelected = selectedThemeIdx == idx
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) theme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedThemeIdx = idx }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (idx in allThemes.indices) {
                                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50))
                                    .background(Brush.linearGradient(theme.gradientColors)))
                            } else {
                                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50))
                                    .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))))
                            }
                            Text(name, color = if (isSelected) theme.primary else ColorTextPrimary, fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            Spacer(Modifier.weight(1f))
                            if (isSelected) Icon(Icons.Default.Check, null, tint = theme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            item {
                SettingsSection(title = strings.settingsRepo) {
                    OutlinedTextField(value = repoUrl, onValueChange = { repoUrl = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimary, unfocusedBorderColor = ColorBgStroke,
                            focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ThemePrimary
                        ), shape = RoundedCornerShape(10.dp))
                }
            }

            item {
                SettingsSection(title = strings.settingsModsDir) {
                    OutlinedTextField(value = modsDir, onValueChange = { modsDir = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePrimary, unfocusedBorderColor = ColorBgStroke,
                            focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ThemePrimary
                        ), shape = RoundedCornerShape(10.dp))
                }
            }

            item {
                SettingsSection(title = "Links") {
                    SettingsLinkRow(icon = Icons.Default.Group,  label = strings.settingsGroupLink,  url = "https://t.me/nb_mods",   context = ctx)
                    SettingsLinkRow(icon = Icons.Default.Person, label = strings.settingsAuthorLink, url = "https://t.me/d2rkmean", context = ctx)
                }
            }

            item {
                SettingsSection(title = "⚠ Danger Zone") {
                    Button(
                        onClick  = { showDeleteAllConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A0000)),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, null, tint = ColorError, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.settingsDeleteAll, color = ColorError, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDeleteAllConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirm = false },
            containerColor   = ColorBgCard,
            shape            = RoundedCornerShape(16.dp),
            title  = { Text(strings.settingsDeleteAll, color = ColorError, fontWeight = FontWeight.Bold) },
            text   = { Text(strings.settingsDeleteAllConfirm, color = ColorTextPrimary) },
            confirmButton = {
                Button(onClick = { showDeleteAllConfirm = false; onDeleteAll() },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)) {
                    Text(strings.deleteLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirm = false }) {
                    Text(strings.cancel, color = ColorTextSecondary)
                }
            }
        )
    }
}