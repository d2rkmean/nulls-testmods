package org.darkmean.testmods

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    strings:     AppStrings,
    prefs:       android.content.SharedPreferences,
    context:     Context,
    onBack:      () -> Unit,
    onInstalled: () -> Unit,
    scope:       kotlinx.coroutines.CoroutineScope
) {
    var libraryMods by remember { mutableStateOf<List<LibraryMod>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var loadError   by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<String?>(null) }
    val downloadState = remember { mutableStateMapOf<String, Float>() }
    val downloadJobs  = remember { mutableStateMapOf<String, Job>() }
    var confirmMod    by remember { mutableStateOf<LibraryMod?>(null) }
    var confirmInstallToNulls by remember { mutableStateOf(true) } // true=Nulls, false=folder

    val repoUrl = prefs.getString(AppConfig.PREFS_KEY_REPO, AppConfig.DEFAULT_REPO_URL) ?: AppConfig.DEFAULT_REPO_URL
    val modsDir = prefs.getString(AppConfig.PREFS_KEY_MODS_DIR, AppConfig.MODS_PATH) ?: AppConfig.MODS_PATH

    val installedTitles = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(repoUrl) {
        isLoading   = true
        loadError   = null
        libraryMods = emptyList()
        withContext(Dispatchers.IO) {
            val root = File(modsDir)
            root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                runCatching {
                    val json = JSONObject(File(dir, "content.json").readTextSafe())
                    val t = getLocalizedText(json, "title").lowercase().trim()
                    val v = getLocalizedText(json, "version")
                    if (t.isNotBlank()) withContext(Dispatchers.Main) { installedTitles[t] = v }
                }
            }
            runCatching {
                val client   = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
                val response = client.newCall(Request.Builder().url(repoUrl).build()).execute()
                val body     = response.body?.string() ?: throw IOException("Empty body")
                parseLibraryIndex(body)
            }.onSuccess { mods ->
                withContext(Dispatchers.Main) {
                    libraryMods = mods
                    isLoading   = false
                    mods.forEach { m ->
                        val key = m.title.lowercase().trim()
                        if (installedTitles.containsKey(key)) downloadState[m.id] = -1f
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) { loadError = e.message; isLoading = false }
            }
        }
    }

    fun startInstallToFolder(mod: LibraryMod) {
        val jobHolder = arrayOfNulls<Job>(1)
        val job = scope.launch {
            downloadAndInstallLibraryMod(
                mod        = mod,
                context    = context,
                modsDir    = modsDir,
                isActive   = { jobHolder[0]?.isActive != false },
                onProgress = { p -> downloadState[mod.id] = p },
                onDone     = { success, msg ->
                    downloadJobs.remove(mod.id)
                    if (success) {
                        downloadState[mod.id] = -1f
                        installedTitles[mod.title.lowercase().trim()] = mod.version
                        onInstalled()
                    } else if (msg == "cancelled") {
                        downloadState.remove(mod.id)
                    } else {
                        downloadState.remove(mod.id)
                        Toast.makeText(context, "${strings.libraryError}: $msg", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        jobHolder[0] = job
        downloadJobs[mod.id] = job
    }

    fun startInstallToNulls(mod: LibraryMod) {
        val jobHolder = arrayOfNulls<Job>(1)
        val job = scope.launch {
            // Download first to cache, then fire intent to Null's Brawl installer
            val tempFile = java.io.File(context.cacheDir, "${mod.id}.NullsBrawlAssets")
            var downloaded = false
            downloadAndInstallLibraryMod(
                mod        = mod,
                context    = context,
                modsDir    = context.cacheDir.absolutePath, // temp download location
                skipUnzip  = true,
                isActive   = { jobHolder[0]?.isActive != false },
                onProgress = { p -> downloadState[mod.id] = p },
                onDone     = { success, msg ->
                    downloadJobs.remove(mod.id)
                    if (success) {
                        downloaded = true
                        // Find the extracted dir in cacheDir and send intent
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            java.io.File(context.cacheDir, "${mod.id}.NullsBrawlAssets")
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/zip")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            component = ComponentName(
                                AppConfig.NULLS_BRAWL_INSTALLER_PACKAGE,
                                AppConfig.NULLS_BRAWL_INSTALLER_ACTIVITY
                            )
                        }
                        runCatching { context.startActivity(intent) }.onFailure {
                            Toast.makeText(context, strings.gameNotFound, Toast.LENGTH_SHORT).show()
                        }
                        downloadState[mod.id] = -1f
                        installedTitles[mod.title.lowercase().trim()] = mod.version
                        onInstalled()
                    } else if (msg == "cancelled") {
                        downloadState.remove(mod.id)
                    } else {
                        downloadState.remove(mod.id)
                        Toast.makeText(context, "${strings.libraryError}: $msg", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        jobHolder[0] = job
        downloadJobs[mod.id] = job
    }

    val categories = remember(libraryMods) { libraryMods.flatMap { it.categories }.distinct().sorted() }
    val downloadStateSnapshot = downloadState.toMap()
    val filtered = remember(libraryMods, searchQuery, selectedCat, downloadStateSnapshot) {
        libraryMods.filter { mod ->
            (searchQuery.isBlank() || mod.title.contains(searchQuery, ignoreCase = true) ||
                    mod.description.contains(searchQuery, ignoreCase = true)) &&
                    (selectedCat == null || mod.categories.contains(selectedCat))
        }
    }

    Scaffold(
        containerColor = ColorBgDeep,
        topBar = {
            Surface(color = ColorBgCard, modifier = Modifier.border(0.5.dp, ThemePrimary.copy(alpha = 0.25f), RoundedCornerShape(0.dp))) {
                TopAppBar(
                    title = { Text(strings.libraryTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorTextPrimary) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = ThemePrimary) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search...", color = ColorTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = ThemePrimary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePrimary, unfocusedBorderColor = ColorBgStroke,
                    focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ThemePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            if (categories.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        FilterChip(selected = selectedCat == null, onClick = { selectedCat = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemePrimary, selectedLabelColor = ThemeOnPrimary))
                    }
                    items(categories) { cat ->
                        FilterChip(selected = selectedCat == cat, onClick = { selectedCat = if (selectedCat == cat) null else cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ThemePrimary, selectedLabelColor = ThemeOnPrimary))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = ThemePrimary)
                        Text(strings.libraryLoading, color = ColorTextSecondary, fontSize = 14.sp)
                    }
                }
                loadError != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ColorError, modifier = Modifier.size(48.dp))
                        Text(strings.libraryError, color = ColorError, fontWeight = FontWeight.SemiBold)
                        Text(loadError ?: "", color = ColorTextSecondary, fontSize = 12.sp)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filtered, key = { it.id }) { mod ->
                        val isUpdate = downloadState[mod.id] == -1f &&
                                mod.version.isNotBlank() &&
                                (installedTitles[mod.title.lowercase().trim()] ?: "") != mod.version
                        LibraryModCard(
                            mod      = mod,
                            strings  = strings,
                            progress = downloadState[mod.id],
                            isUpdate = isUpdate,
                            onInstallToNulls = {
                                confirmMod = mod
                                confirmInstallToNulls = true
                            },
                            onInstallToFolder = {
                                confirmMod = mod
                                confirmInstallToNulls = false
                            },
                            onCancel = {
                                downloadJobs[mod.id]?.cancel()
                                downloadJobs.remove(mod.id)
                                downloadState.remove(mod.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirm dialog
    confirmMod?.let { mod ->
        AlertDialog(
            onDismissRequest = { confirmMod = null },
            containerColor   = ColorBgCard,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(strings.libraryConfirmInstall, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = ColorBgDeep),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, ThemeCardTint.copy(0.4f), RoundedCornerShape(12.dp))) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(ThemeAvatarBg)), Alignment.Center) {
                                Text(mod.title.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemePrimary)
                            }
                            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                                val titleA = remember(mod.title) { if (mod.title.contains('<')) htmlToAnnotatedString(mod.title) else AnnotatedString(mod.title) }
                                Text(titleA, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ColorTextPrimary, maxLines = 2)
                                val authA = remember(mod.author) { if (mod.author.contains('<')) htmlToAnnotatedString(mod.author) else AnnotatedString(mod.author) }
                                Text(authA, fontSize = 11.sp, color = ColorTextSecondary, maxLines = 1)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (mod.version.isNotBlank()) Text("v${mod.version}", fontSize = 10.sp, color = ThemePrimary)
                                    Text("%.1f MB".format(mod.sizeMb), fontSize = 10.sp, color = ColorTextMuted)
                                }
                            }
                        }
                    }
                    // Destination selection
                    Text(if (confirmInstallToNulls) strings.installInNullsBrawl else strings.installInFolder,
                        fontSize = 12.sp, color = ThemePrimary, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = confirmMod ?: return@Button
                        val toNulls = confirmInstallToNulls
                        confirmMod = null
                        downloadState[m.id] = 0f
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val root = File(modsDir)
                                root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                                    runCatching {
                                        val json = JSONObject(File(dir, "content.json").readTextSafe())
                                        val t = getLocalizedText(json, "title").lowercase().trim()
                                        if (t == m.title.lowercase().trim()) dir.deleteRecursively()
                                    }
                                }
                            }
                            if (toNulls) startInstallToNulls(m) else startInstallToFolder(m)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.libraryInstall, color = ThemeOnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmMod = null }) {
                    Text(strings.libraryCancel, color = ColorTextSecondary)
                }
            }
        )
    }
}

@Composable
fun LibraryModCard(
    mod: LibraryMod,
    strings: AppStrings,
    progress: Float?,
    isUpdate: Boolean = false,
    onInstallToNulls: () -> Unit,
    onInstallToFolder: () -> Unit,
    onCancel: () -> Unit = {}
) {
    val titleAnnotated  = remember(mod.title)       { if (mod.title.contains('<'))       htmlToAnnotatedString(mod.title)       else AnnotatedString(mod.title)       }
    val authorAnnotated = remember(mod.author)      { if (mod.author.contains('<'))      htmlToAnnotatedString(mod.author)      else AnnotatedString(mod.author)      }
    val descAnnotated   = remember(mod.description) { if (mod.description.contains('<')) htmlToAnnotatedString(mod.description) else AnnotatedString(mod.description) }

    Card(
        colors   = CardDefaults.cardColors(containerColor = ColorBgCard),
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().border(0.5.dp, ThemeCardTint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(ThemeAvatarBg)),
                    contentAlignment = Alignment.Center) {
                    Text(mod.title.take(1).uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ThemePrimary)
                }
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    Text(titleAnnotated, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ColorTextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(authorAnnotated, fontSize = 11.sp, color = ColorTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("%.1f MB".format(mod.sizeMb), fontSize = 10.sp, color = ThemePrimary)
                        if (mod.downloadsText.isNotBlank())
                            Text(mod.downloadsText, fontSize = 10.sp, color = ColorTextMuted, maxLines = 1)
                    }
                }
            }
            if (mod.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                ClickableHtmlText(annotated = descAnnotated, fontSize = 12.sp, color = ColorTextSecondary,
                    maxLines = 3, lineHeight = 17.sp, overflow = TextOverflow.Ellipsis)
            }
            if (mod.categories.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    mod.categories.forEach { cat ->
                        Surface(color = ThemePrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(cat, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = ThemePrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            when {
                progress == -1f && !isUpdate -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = ThemePrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.libraryInstalled, color = ThemePrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                progress != null -> {
                    // Downloading with cancel
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(strings.libraryInstalling, fontSize = 12.sp, color = ThemePrimary, modifier = Modifier.weight(1f))
                            Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = ColorTextSecondary)
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                            color = ThemePrimary, trackColor = ColorBgStroke)
                        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Icon(Icons.Default.Close, null, tint = ColorError, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.libraryCancel, color = ColorError, fontSize = 12.sp)
                        }
                    }
                }
                else -> {
                    // Two install buttons
                    val label = if (isUpdate) strings.libraryUpdateAvailable else strings.libraryInstall
                    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Button 1: Install in Null's Brawl
                        Box(
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(listOf(ThemeDark, ThemePrimary)))
                                .clickable { onInstallToNulls() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.InstallMobile, null, tint = ThemeOnPrimary, modifier = Modifier.size(16.dp))
                                Text(strings.installInNullsBrawl, color = ThemeOnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                        // Button 2: Install in folder
                        OutlinedButton(
                            onClick  = { onInstallToFolder() },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = ThemePrimary),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, ThemeDark),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.installInFolder, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}