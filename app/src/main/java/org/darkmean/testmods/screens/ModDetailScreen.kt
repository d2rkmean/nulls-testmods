package org.darkmean.testmods

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModDetailScreen(
    mod:     ModItem,
    strings: AppStrings,
    context: Context,
    onBack:  () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val iconBitmap = remember(mod.path) {
        runCatching {
            val f = File(mod.path, "icon.png")
            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
        }.getOrNull()
    }

    var jsonContent by remember { mutableStateOf("") }
    var allFiles    by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var dirSizeMb   by remember { mutableStateOf("…") }

    LaunchedEffect(mod.path) {
        withContext(Dispatchers.IO) {
            val dir = File(mod.path)
            val json = runCatching { File(dir, "content.json").readTextSafe() }.getOrDefault("")
            val files = buildFileTree(dir)
            val totalBytes = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val mb = "%.2f MB".format(totalBytes / 1_048_576f)
            withContext(Dispatchers.Main) {
                jsonContent = json
                allFiles    = files
                dirSizeMb   = mb
            }
        }
    }

    Scaffold(
        containerColor = ColorBgDeep,
        topBar = {
            Surface(color = ColorBgCard, modifier = Modifier.border(0.5.dp, ThemePrimary.copy(alpha = 0.25f), RoundedCornerShape(0.dp))) {
                TopAppBar(
                    title = { Text(strings.modDetailTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ColorTextPrimary) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = ThemePrimary) } },
                    actions = { IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, null, tint = ColorError) } },
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
                Card(colors = CardDefaults.cardColors(containerColor = ColorBgCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, ThemeCardTint.copy(0.5f), RoundedCornerShape(16.dp))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(ThemeAvatarBg)), Alignment.Center) {
                            if (iconBitmap != null) Image(iconBitmap, null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                            else Text(mod.title.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ThemePrimary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(mod.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorTextPrimary)
                            if (mod.author.isNotBlank()) Text(mod.author, fontSize = 12.sp, color = ColorTextSecondary)
                            if (mod.version.isNotBlank()) Text("v${mod.version}", fontSize = 11.sp, color = ThemePrimary)
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Info") {
                    DetailRow(label = strings.modDetailUuid, value = mod.id)
                    DetailRow(label = strings.modDetailSize, value = dirSizeMb)
                }
            }

            item {
                SettingsSection(title = strings.modDetailJson) {
                    val parsedJson = remember(jsonContent) { runCatching { org.json.JSONObject(jsonContent) }.getOrNull() }
                    if (parsedJson != null) {
                        Surface(color = ColorBgDeep, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp)) { JsonTreeNode("", parsedJson, 0) }
                        }
                    } else {
                        Surface(color = ColorBgDeep, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(jsonContent.take(1000), modifier = Modifier.padding(10.dp),
                                fontSize = 10.sp, color = ColorTextSecondary, fontFamily = FontFamily.Monospace, lineHeight = 15.sp)
                        }
                    }
                }
            }

            item {
                SettingsSection(title = strings.modDetailFiles) {
                    allFiles.forEach { entry -> FileTreeRow(entry) }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick  = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A0000))
                    ) {
                        Icon(Icons.Default.Delete, null, tint = ColorError, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.modDetailDelete, color = ColorError, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor   = ColorBgCard,
            shape            = RoundedCornerShape(16.dp),
            title  = { Text(strings.modDetailDelete, color = ColorError, fontWeight = FontWeight.Bold) },
            text   = { Text("\"${mod.title}\"?", color = ColorTextPrimary) },
            confirmButton = {
                Button(onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)) {
                    Text(strings.deleteLabel, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel, color = ColorTextSecondary)
                }
            }
        )
    }
}