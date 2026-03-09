package org.darkmean.testmods

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deepLinkUri = if (intent?.action == Intent.ACTION_VIEW) intent?.data else null
        setContent { ModManagerApp(deepLinkUri) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

// ─────────────────────────────────────────────────────────────────────
// ROOT COMPOSABLE
// ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModManagerApp(deepLinkUri: Uri?) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val strings = remember { getStrings(context) }

    var modsList      by remember { mutableStateOf<List<ModItem>>(emptyList()) }
    var isLoading     by remember { mutableStateOf(false) }
    var brokenFileDlg by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }
    var detailMod     by remember { mutableStateOf<ModItem?>(null) }
    var pendingInstallUri     by remember { mutableStateOf<Uri?>(null) }
    var pendingInstallToNulls by remember { mutableStateOf(false) }

    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.MAIN) {
            loadMods(context) { list -> modsList = list }
        }
    }

    val prefs = remember { context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE) }
    var warningVisible by remember {
        mutableStateOf(!prefs.getBoolean(AppConfig.PREFS_KEY_WARN_DISMISSED, false))
    }

    LaunchedEffect(Unit) {
        val savedTheme = prefs.getInt(AppConfig.PREFS_KEY_THEME, -1)
        currentTheme = if (savedTheme in allThemes.indices) allThemes[savedTheme] else sessionTheme
    }

    val manageFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        scope.launch { loadMods(context) { list -> modsList = list } }
    }

    LaunchedEffect(Unit) {
        requestStoragePermission(context, manageFilesLauncher)
        loadMods(context) { list -> modsList = list }
    }

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            isLoading = true
            installMod(context, deepLinkUri) { success, msg, broken ->
                isLoading = false
                if (broken) {
                    brokenFileDlg = msg
                } else if (!success) {
                    Toast.makeText(context, "${strings.errorPrefix}$msg", Toast.LENGTH_SHORT).show()
                }
                if (success) scope.launch { loadMods(context) { list -> modsList = list } }
            }
        }
    }

    // File pickers — for Null's Brawl and for folder
    val filePickerForNulls = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingInstallUri = it; pendingInstallToNulls = true }
    }
    val filePickerForFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingInstallUri = it; pendingInstallToNulls = false }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary        = ThemePrimary,
            onPrimary      = ThemeOnPrimary,
            secondary      = ThemeAccent,
            background     = ColorBgDeep,
            surface        = ColorBgCard,
            surfaceVariant = Color(0xFF1E1E1E),
            error          = ColorError,
            onBackground   = ColorTextPrimary,
            onSurface      = ColorTextPrimary
        )
    ) {
        when (currentScreen) {
            AppScreen.SETTINGS -> SettingsScreen(
                strings = strings,
                prefs   = prefs,
                context = context,
                onBack  = {
                    val idx = prefs.getInt(AppConfig.PREFS_KEY_THEME, -1)
                    currentTheme = if (idx in allThemes.indices) allThemes[idx] else sessionTheme
                    currentScreen = AppScreen.MAIN
                },
                onDeleteAll = {
                    val root = java.io.File(prefs.getString(AppConfig.PREFS_KEY_MODS_DIR, AppConfig.MODS_PATH) ?: AppConfig.MODS_PATH)
                    scope.launch {
                        withContext(Dispatchers.IO) { root.deleteRecursively() }
                        loadMods(context) { list -> modsList = list }
                    }
                }
            )
            AppScreen.LIBRARY -> LibraryScreen(
                strings     = strings,
                prefs       = prefs,
                context     = context,
                onBack      = { currentScreen = AppScreen.MAIN },
                onInstalled = { scope.launch { loadMods(context) { list -> modsList = list } } },
                scope       = scope
            )
            AppScreen.MOD_DETAIL -> detailMod?.let { mod ->
                ModDetailScreen(
                    mod     = mod,
                    strings = strings,
                    context = context,
                    onBack  = { currentScreen = AppScreen.MAIN },
                    onDelete = {
                        scope.launch {
                            if (withContext(Dispatchers.IO) { deleteMod(mod) }) {
                                loadMods(context) { list -> modsList = list }
                                currentScreen = AppScreen.MAIN
                            }
                        }
                    }
                )
            }
            AppScreen.MAIN -> {
                Scaffold(
                    containerColor = ColorBgDeep,
                    topBar = {
                        AppTopBar(
                            strings   = strings,
                            modCount  = modsList.size,
                            onRefresh  = { scope.launch { loadMods(context) { list -> modsList = list } } },
                            onSettings = { currentScreen = AppScreen.SETTINGS },
                            onLibrary  = { currentScreen = AppScreen.LIBRARY }
                        )
                    },
                    bottomBar = {
                        AppBottomBar(
                            strings            = strings,
                            scope              = scope,
                            context            = context,
                            onPickFileForNulls = { filePickerForNulls.launch(arrayOf("*/*", "application/zip")) },
                            onPickFileToFolder = { filePickerForFolder.launch(arrayOf("*/*", "application/zip")) }
                        )
                    }
                ) { paddingValues ->
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxSize().padding(paddingValues).background(ColorBgDeep)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxWidth().height(220.dp)
                                .background(Brush.verticalGradient(colors = listOf(
                                    ThemeGradient.first().copy(alpha = 0.10f),
                                    ThemeGradient.getOrElse(1) { ThemeGradient.first() }.copy(alpha = 0.05f),
                                    Color.Transparent
                                )))
                        )
                        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
                            AnimatedVisibility(visible = isLoading) {
                                LinearProgressIndicator(
                                    modifier   = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(50)),
                                    color      = ThemePrimary,
                                    trackColor = ColorBgStroke
                                )
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
                            AnimatedVisibility(visible = warningVisible, exit = shrinkVertically() + fadeOut()) {
                                WarningCard(strings, context) {
                                    warningVisible = false
                                    prefs.edit().putBoolean(AppConfig.PREFS_KEY_WARN_DISMISSED, true).apply()
                                }
                            }
                            androidx.compose.foundation.layout.Spacer(Modifier.height(if (warningVisible) 16.dp else 8.dp))
                            if (modsList.isEmpty() && !isLoading) {
                                EmptyState(strings)
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding      = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(modsList, key = { it.id }) { mod ->
                                        ModCard(mod = mod, strings = strings,
                                            onClick = { detailMod = mod; currentScreen = AppScreen.MOD_DETAIL }
                                        ) {
                                            scope.launch {
                                                if (withContext(Dispatchers.IO) { deleteMod(mod) }) {
                                                    loadMods(context) { list -> modsList = list }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        brokenFileDlg?.let { msg ->
                            BrokenFileDialog(strings = strings, message = msg) { brokenFileDlg = null }
                        }

                        // Confirm install dialog (from bottom bar file picker)
                        pendingInstallUri?.let { uri ->
                            val fileName = remember(uri) { getFileName(context.contentResolver, uri) ?: "mod" }
                            val toNulls  = pendingInstallToNulls
                            AlertDialog(
                                onDismissRequest = { pendingInstallUri = null },
                                containerColor   = ColorBgCard,
                                shape            = RoundedCornerShape(20.dp),
                                title = {
                                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                                .background(ThemePrimary.copy(alpha = 0.15f)),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Icon(androidx.compose.material.icons.Icons.Default.Extension, null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
                                        }
                                        androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                                        Text(strings.libraryConfirmInstall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = ColorTextPrimary)
                                    }
                                },
                                text = {
                                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Card(colors = CardDefaults.cardColors(containerColor = ColorBgDeep),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().border(0.5.dp, ThemeCardTint.copy(0.4f), RoundedCornerShape(12.dp))) {
                                            androidx.compose.foundation.layout.Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                                Icon(Icons.Default.InsertDriveFile, null, tint = ThemePrimary, modifier = Modifier.size(32.dp))
                                                Text(fileName, fontSize = 12.sp, color = ColorTextPrimary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                        Text(
                                            if (toNulls) strings.installInNullsBrawl else strings.installInFolder,
                                            fontSize = 12.sp,
                                            color = ThemePrimary
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val u = pendingInstallUri ?: return@Button
                                            val installToNulls = pendingInstallToNulls
                                            pendingInstallUri = null
                                            if (installToNulls) {
                                                // Send intent directly to Null's Brawl installer
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(u, "application/zip")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    component = ComponentName(
                                                        AppConfig.NULLS_BRAWL_INSTALLER_PACKAGE,
                                                        AppConfig.NULLS_BRAWL_INSTALLER_ACTIVITY
                                                    )
                                                }
                                                runCatching { context.startActivity(intent) }.onFailure {
                                                    Toast.makeText(context, strings.gameNotFound, Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                isLoading = true
                                                scope.launch {
                                                    installMod(context, u) { success, msg, broken ->
                                                        isLoading = false
                                                        if (broken) {
                                                            brokenFileDlg = msg
                                                        } else if (!success) {
                                                            Toast.makeText(context, "${strings.errorPrefix}$msg", Toast.LENGTH_SHORT).show()
                                                        }
                                                        if (success) scope.launch { loadMods(context) { list -> modsList = list } }
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                                        shape  = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(strings.libraryInstall, color = ThemeOnPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingInstallUri = null }) {
                                        Text(strings.libraryCancel, color = ColorTextSecondary)
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