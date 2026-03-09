package org.darkmean.testmods

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.*
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

// ─────────────────────────────────────────────────────────────────────
// MOD MANAGEMENT
// ─────────────────────────────────────────────────────────────────────

suspend fun loadMods(context: Context, onLoaded: (List<ModItem>) -> Unit) {
    val list = withContext(Dispatchers.IO) {
        val prefs    = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val modsPath = prefs.getString(AppConfig.PREFS_KEY_MODS_DIR, AppConfig.MODS_PATH) ?: AppConfig.MODS_PATH
        val root     = File(modsPath)
        if (!root.exists()) {
            root.mkdirs()
            File(root, ".nomedia").createIfMissing()
        }

        root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val contentFile = File(dir, "content.json")
                if (!contentFile.exists()) {
                    return@mapNotNull ModItem(
                        id = dir.name, title = dir.name, desc = "", author = "", version = "",
                        path = dir.absolutePath, hasIcon = false, isBroken = true
                    )
                }
                val json = runCatching { JSONObject(contentFile.readTextSafe()) }.getOrNull()
                if (json == null) {
                    return@mapNotNull ModItem(
                        id = dir.name, title = dir.name, desc = "", author = "", version = "",
                        path = dir.absolutePath, hasIcon = false, isBroken = true
                    )
                }
                val title = getLocalizedText(json, "title")
                if (title.isBlank()) return@mapNotNull ModItem(
                    id = dir.name, title = dir.name, desc = "", author = "", version = "",
                    path = dir.absolutePath, hasIcon = false, isBroken = true
                )
                runCatching {
                    ModItem(
                        id       = dir.name,
                        title    = title,
                        desc     = getLocalizedText(json, "description"),
                        author   = getLocalizedText(json, "author"),
                        version  = getLocalizedText(json, "version"),
                        path     = dir.absolutePath,
                        hasIcon  = File(dir, "icon.png").exists(),
                        isBroken = false
                    )
                }.getOrNull()
            } ?: emptyList()
    }
    onLoaded(list)
}

fun deleteMod(mod: ModItem): Boolean {
    val deleted = runCatching { File(mod.path).deleteRecursively() }.getOrDefault(false)
    if (deleted) {
        val root      = File(AppConfig.MODS_PATH)
        val remaining = root.listFiles()?.filter { it.isDirectory && it.name != ".nomedia" }
        if (remaining != null && remaining.isEmpty()) {
            runCatching { root.deleteRecursively() }
        }
    }
    return deleted
}

/**
 * Installs a mod from a URI.
 * onComplete(success, message, isBrokenFile)
 */
suspend fun installMod(
    context:    Context,
    uri:        Uri,
    onComplete: (Boolean, String?, Boolean) -> Unit
) {
    withContext(Dispatchers.IO) {
        runCatching {
            val prefs    = context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
            val modsPath = prefs.getString(AppConfig.PREFS_KEY_MODS_DIR, AppConfig.MODS_PATH) ?: AppConfig.MODS_PATH

            val resolver = context.contentResolver
            val fileName = getFileName(resolver, uri) ?: "mod_${System.currentTimeMillis()}"
            val tempFile = File(context.cacheDir, fileName)

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot open input stream")

            if (tempFile.length() == 0L) {
                tempFile.delete()
                withContext(Dispatchers.Main) { onComplete(false, "File is empty", true) }
                return@runCatching
            }

            val targetDir = File(modsPath, UUID.randomUUID().toString()).apply { mkdirs() }
            var title   = ""
            var desc    = ""
            var author  = ""
            var version = "1.0"

            fun readMetadata(json: JSONObject) {
                title   = getLocalizedText(json, "title")
                desc    = getLocalizedText(json, "description")
                author  = getLocalizedText(json, "author")
                version = getLocalizedText(json, "version").ifBlank { "1.0" }
            }

            when {
                isValidZip(tempFile) -> {
                    unzip(tempFile, targetDir)
                    val contentFile = File(targetDir, "content.json")
                    if (!contentFile.exists()) {
                        tempFile.delete()
                        targetDir.deleteRecursively()
                        withContext(Dispatchers.Main) {
                            onComplete(false, "content.json not found in archive", true)
                        }
                        return@runCatching
                    }
                    runCatching { readMetadata(JSONObject(contentFile.readTextSafe())) }.onFailure {
                        tempFile.delete()
                        targetDir.deleteRecursively()
                        withContext(Dispatchers.Main) {
                            onComplete(false, "content.json is malformed: ${it.message}", true)
                        }
                        return@runCatching
                    }
                }
                else -> {
                    val jsonText = runCatching { tempFile.readText() }.getOrNull()
                    if (jsonText == null) {
                        tempFile.delete()
                        targetDir.deleteRecursively()
                        withContext(Dispatchers.Main) {
                            onComplete(false, "Cannot read file", true)
                        }
                        return@runCatching
                    }
                    runCatching {
                        val json = JSONObject(jsonText)
                        tempFile.copyTo(File(targetDir, "content.json"), overwrite = true)
                        readMetadata(json)
                    }.onFailure {
                        tempFile.delete()
                        targetDir.deleteRecursively()
                        withContext(Dispatchers.Main) {
                            onComplete(false, "Invalid JSON: ${it.message}", true)
                        }
                        return@runCatching
                    }
                }
            }

            tempFile.delete()
            withContext(Dispatchers.Main) { onComplete(true, null, false) }
        }.onFailure { e ->
            withContext(Dispatchers.Main) { onComplete(false, e.localizedMessage, false) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// LIBRARY DOWNLOAD + INSTALL
// ─────────────────────────────────────────────────────────────────────
suspend fun downloadAndInstallLibraryMod(
    mod:        LibraryMod,
    context:    Context,
    modsDir:    String,
    skipUnzip:  Boolean = false,
    isActive:   () -> Boolean = { true },
    onProgress: (Float) -> Unit,
    onDone:     (Boolean, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        runCatching {
            val client   = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(Request.Builder().url(mod.downloadUrl).build()).execute()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            val contentLength = response.body?.contentLength() ?: -1L
            val tempFile = File(context.cacheDir, "${mod.id}.NullsBrawlAssets")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (!isActive()) {
                            tempFile.delete()
                            withContext(Dispatchers.Main) { onDone(false, "cancelled") }
                            return@runCatching
                        }
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (contentLength > 0) {
                            val progress = (downloaded.toFloat() / contentLength).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            } ?: throw IOException("Empty response body")
            if (skipUnzip) {
                withContext(Dispatchers.Main) { onDone(true, null)}
                return@runCatching
            }
            if (!isActive()) {
                tempFile.delete()
                withContext(Dispatchers.Main) { onDone(false, "cancelled") }
                return@runCatching
            }

            if (!isValidZip(tempFile)) {
                tempFile.delete()
                withContext(Dispatchers.Main) { onDone(false, "Not a valid archive") }
                return@runCatching
            }

            val targetDir = File(modsDir, UUID.randomUUID().toString()).apply { mkdirs() }
            unzip(tempFile, targetDir)
            tempFile.delete()

            val contentFile = File(targetDir, "content.json")
            if (!contentFile.exists()) {
                targetDir.deleteRecursively()
                withContext(Dispatchers.Main) { onDone(false, "content.json not found") }
                return@runCatching
            }

            withContext(Dispatchers.Main) { onDone(true, null) }
        }.onFailure { e ->
            withContext(Dispatchers.Main) { onDone(false, e.message) }
        }
    }
}

fun parseLibraryIndex(json: String): List<LibraryMod> {
    val root = JSONObject(json)
    val arr  = root.getJSONArray("library")
    val lang = Locale.getDefault().language
    val result = mutableListOf<LibraryMod>()
    for (i in 0 until arr.length()) {
        val obj  = arr.getJSONObject(i)
        val meta = obj.optJSONObject("meta") ?: continue
        val id   = obj.optString("id")
        fun localized(key: String): String {
            val v = meta.opt("@$key") ?: return ""
            return when (v) {
                is JSONObject -> {
                    v.optString(lang.uppercase()).takeIf { it.isNotBlank() }
                        ?: v.optString("EN").takeIf { it.isNotBlank() }
                        ?: ""
                }
                is String -> v
                else -> v.toString()
            }
        }
        val title = localized("title")
        if (title.isBlank()) continue
        val cats = runCatching {
            val a = meta.getJSONArray("@categories")
            (0 until a.length()).map { a.getString(it) }
        }.getOrElse { emptyList() }
        val sizeMb = obj.optLong("size", 0L) / 1_048_576f
        val dlObj  = obj.optJSONObject("downloads")
        val dlText = dlObj?.optString(if (lang == "ru") "RU" else "EN", "") ?: ""
        result.add(LibraryMod(
            id           = id,
            title        = title,
            description  = localized("description"),
            author       = localized("author"),
            version      = localized("version"),
            iconUrl      = obj.optString("icon_url", ""),
            downloadUrl  = obj.optString("download_url", ""),
            sizeMb       = sizeMb,
            downloadsText = dlText,
            categories   = cats
        ))
    }
    return result
}

// ─────────────────────────────────────────────────────────────────────
// UTILITIES
// ─────────────────────────────────────────────────────────────────────
fun isValidZip(file: File): Boolean {
    return runCatching {
        FileInputStream(file).use { stream ->
            val magic = ByteArray(4)
            stream.read(magic) == 4 &&
                    magic[0] == 0x50.toByte() &&
                    magic[1] == 0x4B.toByte() &&
                    magic[2] == 0x03.toByte() &&
                    magic[3] == 0x04.toByte()
        }
    }.getOrDefault(false)
}

fun unzip(zipFile: File, targetDir: File) {
    val canonicalTarget = targetDir.canonicalPath
    ZipInputStream(FileInputStream(zipFile)).use { stream ->
        var entry = stream.nextEntry
        while (entry != null) {
            val entryName = entry.name
            if (entryName.startsWith("build/") || entryName == "build") {
                stream.closeEntry()
                entry = stream.nextEntry
                continue
            }
            val outFile = File(targetDir, entryName)
            if (!outFile.canonicalPath.startsWith("$canonicalTarget${File.separator}")) {
                throw SecurityException("Path traversal attack blocked: $entryName")
            }
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { stream.copyTo(it) }
            }
            stream.closeEntry()
            entry = stream.nextEntry
        }
    }
}

fun getFileName(resolver: android.content.ContentResolver, uri: Uri): String? {
    if (uri.scheme == "content") {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) return cursor.getString(index)
            }
        }
    }
    return uri.path?.substringAfterLast('/')
}

fun requestStoragePermission(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>? = null
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
        runCatching {
            val intent = android.content.Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (launcher != null) launcher.launch(intent)
            else context.startActivity(intent)
        }
    }
}

fun File.createIfMissing() {
    if (!exists()) runCatching { createNewFile() }
}

fun File.readTextSafe(): String {
    val bytes = readBytes()
    val text = if (bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() &&
        bytes[1] == 0xBB.toByte() &&
        bytes[2] == 0xBF.toByte()
    ) {
        String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
    } else {
        String(bytes, Charsets.UTF_8)
    }
    return text.trim()
}

fun buildFileTree(dir: File, depth: Int = 0): List<FileEntry> {
    val skipDirs = setOf("META-INF", "build", ".gradle")
    val result = mutableListOf<FileEntry>()
    dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))?.forEach { f ->
        if (depth == 0 && f.isDirectory && f.name in skipDirs) return@forEach
        val size = if (f.isFile) f.length() / 1_048_576f else 0f
        result.add(FileEntry(f.name, depth, f.isDirectory, size))
        if (f.isDirectory && depth < 3) result.addAll(buildFileTree(f, depth + 1))
    }
    return result
}