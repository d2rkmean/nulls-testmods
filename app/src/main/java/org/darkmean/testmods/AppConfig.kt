package org.darkmean.testmods

// ─────────────────────────────────────────────────────────────────────
// CONFIG
// ─────────────────────────────────────────────────────────────────────
object AppConfig {
    const val MODS_PATH          = "/sdcard/NullsBrawlMods/"
    const val PREFS_NAME         = "cfg_x"
    const val PREFS_KEY_WARN_DISMISSED = "warn_dismissed"
    const val PREFS_KEY_THEME    = "selected_theme"
    const val PREFS_KEY_REPO     = "repo_url"
    const val PREFS_KEY_MODS_DIR = "mods_dir"
    const val DEFAULT_REPO_URL   = "https://ext.nulls.gg/mods/index.json"
    const val GAME_SCHEME        = "nullsbrawl://open"

    // Null's Brawl installer package
    const val NULLS_BRAWL_INSTALLER_PACKAGE = "daniillnull.nulls.brawlstars"
    const val NULLS_BRAWL_INSTALLER_ACTIVITY = "daniillnull.nulls.mods.activity.InstallModificationActivity"
}