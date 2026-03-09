package org.darkmean.testmods

import android.content.Context
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────
// LOCALIZATION — strings are now backed by strings.xml resources
// This interface keeps type-safety; values are loaded via Context.
// ─────────────────────────────────────────────────────────────────────
interface AppStrings {
    val appTitle: String
    val installButton: String
    val installInNullsBrawl: String
    val installInFolder: String
    val launchGameButton: String
    val warningTitle: String
    val warningBody: String
    val warningDismiss: String
    val banTitle: String
    val banMessage: String
    val banDetails: String
    val banReasonLabel: String
    val banButton: String
    val modInstalled: String
    val gameNotFound: String
    val errorPrefix: String
    val deleteLabel: String
    val emptyListTitle: String
    val emptyListSubtitle: String
    val serverUnavailable: String
    val modsCountSuffix: String
    val authorLabel: String
    val brokenFileError: String
    val brokenFileTitle: String
    val settingsTitle: String
    val settingsTheme: String
    val settingsThemeAuto: String
    val settingsRepo: String
    val settingsModsDir: String
    val settingsDeleteAll: String
    val settingsDeleteAllConfirm: String
    val settingsGroupLink: String
    val settingsAuthorLink: String
    val settingsSaved: String
    val libraryTitle: String
    val libraryInstall: String
    val libraryInstalling: String
    val libraryInstalled: String
    val libraryError: String
    val libraryLoading: String
    val librarySize: String
    val libraryDownloads: String
    val modDetailTitle: String
    val modDetailUuid: String
    val modDetailSize: String
    val modDetailFiles: String
    val modDetailJson: String
    val modDetailOpenInExplorer: String
    val modDetailDelete: String
    val sigNotSigned: String
    val sigExpiresSoon: String
    val sigExpired: String
    val sigNotOriginal: String
    val libraryCancel: String
    val libraryConfirmInstall: String
    val libraryUpdateAvailable: String
    val modBroken: String
    val warningBodyNullsVersion: String
    val cancel: String
}

// ─────────────────────────────────────────────────────────────────────
// Resource-backed implementation — reads from strings.xml
// ─────────────────────────────────────────────────────────────────────
class ResourceStrings(private val ctx: Context) : AppStrings {
    override val appTitle               get() = ctx.getString(R.string.app_name)
    override val installButton          get() = ctx.getString(R.string.install_button)
    override val installInNullsBrawl    get() = ctx.getString(R.string.install_in_nulls_brawl)
    override val installInFolder        get() = ctx.getString(R.string.install_in_folder)
    override val launchGameButton       get() = ctx.getString(R.string.launch_game_button)
    override val warningTitle           get() = ctx.getString(R.string.warning_title)
    override val warningBody            get() = ctx.getString(R.string.warning_body)
    override val warningDismiss         get() = ctx.getString(R.string.warning_dismiss)
    override val banTitle               get() = ctx.getString(R.string.ban_title)
    override val banMessage             get() = ctx.getString(R.string.ban_message)
    override val banDetails             get() = ctx.getString(R.string.ban_details)
    override val banReasonLabel         get() = ctx.getString(R.string.ban_reason_label)
    override val banButton              get() = ctx.getString(R.string.ban_button)
    override val modInstalled           get() = ctx.getString(R.string.mod_installed)
    override val gameNotFound           get() = ctx.getString(R.string.game_not_found)
    override val errorPrefix            get() = ctx.getString(R.string.error_prefix)
    override val deleteLabel            get() = ctx.getString(R.string.delete_label)
    override val emptyListTitle         get() = ctx.getString(R.string.empty_list_title)
    override val emptyListSubtitle      get() = ctx.getString(R.string.empty_list_subtitle)
    override val serverUnavailable      get() = ctx.getString(R.string.server_unavailable)
    override val modsCountSuffix        get() = ctx.getString(R.string.mods_count_suffix)
    override val authorLabel            get() = ctx.getString(R.string.author_label)
    override val brokenFileError        get() = ctx.getString(R.string.broken_file_error)
    override val brokenFileTitle        get() = ctx.getString(R.string.broken_file_title)
    override val settingsTitle          get() = ctx.getString(R.string.settings_title)
    override val settingsTheme          get() = ctx.getString(R.string.settings_theme)
    override val settingsThemeAuto      get() = ctx.getString(R.string.settings_theme_auto)
    override val settingsRepo           get() = ctx.getString(R.string.settings_repo)
    override val settingsModsDir        get() = ctx.getString(R.string.settings_mods_dir)
    override val settingsDeleteAll      get() = ctx.getString(R.string.settings_delete_all)
    override val settingsDeleteAllConfirm get() = ctx.getString(R.string.settings_delete_all_confirm)
    override val settingsGroupLink      get() = ctx.getString(R.string.settings_group_link)
    override val settingsAuthorLink     get() = ctx.getString(R.string.settings_author_link)
    override val settingsSaved          get() = ctx.getString(R.string.settings_saved)
    override val libraryTitle           get() = ctx.getString(R.string.library_title)
    override val libraryInstall         get() = ctx.getString(R.string.library_install)
    override val libraryInstalling      get() = ctx.getString(R.string.library_installing)
    override val libraryInstalled       get() = ctx.getString(R.string.library_installed)
    override val libraryError           get() = ctx.getString(R.string.library_error)
    override val libraryLoading         get() = ctx.getString(R.string.library_loading)
    override val librarySize            get() = ctx.getString(R.string.library_size)
    override val libraryDownloads       get() = ctx.getString(R.string.library_downloads)
    override val modDetailTitle         get() = ctx.getString(R.string.mod_detail_title)
    override val modDetailUuid          get() = ctx.getString(R.string.mod_detail_uuid)
    override val modDetailSize          get() = ctx.getString(R.string.mod_detail_size)
    override val modDetailFiles         get() = ctx.getString(R.string.mod_detail_files)
    override val modDetailJson          get() = ctx.getString(R.string.mod_detail_json)
    override val modDetailOpenInExplorer get() = ctx.getString(R.string.mod_detail_open_in_explorer)
    override val modDetailDelete        get() = ctx.getString(R.string.mod_detail_delete)
    override val sigNotSigned           get() = ctx.getString(R.string.sig_not_signed)
    override val sigExpiresSoon         get() = ctx.getString(R.string.sig_expires_soon)
    override val sigExpired             get() = ctx.getString(R.string.sig_expired)
    override val sigNotOriginal         get() = ctx.getString(R.string.sig_not_original)
    override val libraryCancel          get() = ctx.getString(R.string.library_cancel)
    override val libraryConfirmInstall  get() = ctx.getString(R.string.library_confirm_install)
    override val libraryUpdateAvailable get() = ctx.getString(R.string.library_update_available)
    override val modBroken              get() = ctx.getString(R.string.mod_broken)
    override val warningBodyNullsVersion get() = ctx.getString(R.string.warning_body_nulls_version)
    override val cancel                 get() = ctx.getString(R.string.cancel)
}

fun getStrings(context: Context): AppStrings = ResourceStrings(context)

// ─────────────────────────────────────────────────────────────────────
// getLocalizedText — reads localized field from mod's content.json
// Fallback order: device language → ru → en → first available
// ─────────────────────────────────────────────────────────────────────
fun getLocalizedText(json: org.json.JSONObject, key: String): String {
    val lang      = Locale.getDefault().language
    val localeKey = "@$key"
    if (json.has(localeKey)) {
        val value = json.opt(localeKey)
        if (value is org.json.JSONObject) {
            val keys = value.keys().asSequence().toList()
            fun findLang(target: String): String? =
                keys.firstOrNull { it.equals(target, ignoreCase = true) }
                    ?.let { value.optString(it).takeIf { s -> s.isNotEmpty() } }
            return findLang(lang)
                ?: findLang("en")
                ?: keys.firstOrNull()?.let { value.optString(it) }
                ?: ""
        }
        if (value is String) return value
        if (value != null && value !is org.json.JSONObject) return value.toString()
    }
    return json.optString(key, "")
}