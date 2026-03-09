package org.darkmean.testmods

// ─────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────
data class ModItem(
    val id:       String,
    val title:    String,
    val desc:     String,
    val author:   String,
    val version:  String,
    val path:     String,
    val hasIcon:  Boolean,
    val isBroken: Boolean = false
)

enum class SignatureStatus { NOT_SIGNED, EXPIRES_SOON, EXPIRED, NOT_ORIGINAL }
data class ModSignatureInfo(val status: SignatureStatus, val daysLeft: Int = 0)

data class LibraryMod(
    val id:           String,
    val title:        String,
    val description:  String,
    val author:       String,
    val version:      String,
    val iconUrl:      String,
    val downloadUrl:  String,
    val sizeMb:       Float,
    val downloadsText: String,
    val categories:   List<String>
)

enum class AppScreen { MAIN, SETTINGS, LIBRARY, MOD_DETAIL }

data class FileEntry(val name: String, val depth: Int, val isDir: Boolean, val sizeMb: Float)