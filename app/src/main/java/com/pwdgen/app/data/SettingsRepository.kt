package com.pwdgen.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "pwdgen_settings")

/** User preferences: language, theme/wallpaper, cloud config, sync options. */
class SettingsRepository(private val context: Context) {

    private val store = context.settingsStore

    val language: Flow<String> = store.data.map { it[KEY_LANG] ?: LANG_SYSTEM }
    val themeMode: Flow<String> = store.data.map { it[KEY_THEME] ?: THEME_SYSTEM }

    /** wallpaper style: "solid" | "gradient" | "image" */
    val wallpaperStyle: Flow<String> = store.data.map { it[KEY_WALLPAPER_STYLE] ?: WALL_SOLID }
    val wallpaperColor: Flow<Long> = store.data.map { it[KEY_WALLPAPER_COLOR] ?: 0xFF101623L }
    val wallpaperGradientIndex: Flow<Int> = store.data.map { (it[KEY_WALLPAPER_GRADIENT] ?: 0) }
    val wallpaperImageUri: Flow<String?> = store.data.map { it[KEY_WALLPAPER_IMAGE] }
    val dynamicColor: Flow<Boolean> = store.data.map { it[KEY_DYNAMIC_COLOR] ?: false }

    // ---- cloud ----
    val cloudProvider: Flow<String> = store.data.map { it[KEY_CLOUD_PROVIDER] ?: CLOUD_NONE }
    val githubOwner: Flow<String> = store.data.map { it[KEY_GH_OWNER] ?: "" }
    val githubRepo: Flow<String> = store.data.map { it[KEY_GH_REPO] ?: "" }
    val githubPath: Flow<String> = store.data.map { it[KEY_GH_PATH] ?: DEFAULT_CLOUD_PATH }
    val githubBranch: Flow<String> = store.data.map { it[KEY_GH_BRANCH] ?: "main" }
    val autoSync: Flow<Boolean> = store.data.map { it[KEY_AUTO_SYNC] ?: false }
    val lastSyncAt: Flow<Long> = store.data.map { it[KEY_LAST_SYNC] ?: 0L }
    /** custom endpoint for the generic provider (WebDAV / other) */
    val customEndpoint: Flow<String> = store.data.map { it[KEY_CUSTOM_ENDPOINT] ?: "" }

    // ---- optional master persistence ----
    val saveMaster: Flow<Boolean> = store.data.map { it[KEY_SAVE_MASTER] ?: false }
    // ---- optional site/login persistence ----
    /** Master switch: persist the site name when generating. */
    val saveSite: Flow<Boolean> = store.data.map { it[KEY_SAVE_SITE] ?: true }
    /** Sub-switch (only meaningful when [saveSite] is on): also persist login. */
    val saveLogin: Flow<Boolean> = store.data.map { it[KEY_SAVE_LOGIN] ?: false }

    suspend fun setLanguage(v: String) = put(KEY_LANG, v)
    suspend fun setThemeMode(v: String) = put(KEY_THEME, v)
    suspend fun setWallpaperStyle(v: String) = put(KEY_WALLPAPER_STYLE, v)
    suspend fun setWallpaperColor(v: Long) = put(KEY_WALLPAPER_COLOR, v)
    suspend fun setWallpaperGradientIndex(v: Int) = put(KEY_WALLPAPER_GRADIENT, v)
    suspend fun setWallpaperImageUri(v: String?) = put(KEY_WALLPAPER_IMAGE, v ?: "")
    suspend fun setDynamicColor(v: Boolean) = put(KEY_DYNAMIC_COLOR, v)
    suspend fun setCloudProvider(v: String) = put(KEY_CLOUD_PROVIDER, v)
    suspend fun setGithubOwner(v: String) = put(KEY_GH_OWNER, v)
    suspend fun setGithubRepo(v: String) = put(KEY_GH_REPO, v)
    suspend fun setGithubPath(v: String) = put(KEY_GH_PATH, v)
    suspend fun setGithubBranch(v: String) = put(KEY_GH_BRANCH, v)
    suspend fun setAutoSync(v: Boolean) = put(KEY_AUTO_SYNC, v)
    suspend fun setLastSyncAt(v: Long) = put(KEY_LAST_SYNC, v)
    suspend fun setCustomEndpoint(v: String) = put(KEY_CUSTOM_ENDPOINT, v)
    suspend fun setSaveMaster(v: Boolean) = put(KEY_SAVE_MASTER, v)
    suspend fun setSaveSite(v: Boolean) = put(KEY_SAVE_SITE, v)
    suspend fun setSaveLogin(v: Boolean) = put(KEY_SAVE_LOGIN, v)

    private suspend fun <T> put(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    companion object {
        const val LANG_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_ZH = "zh"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        const val WALL_SOLID = "solid"
        const val WALL_GRADIENT = "gradient"
        const val WALL_IMAGE = "image"

        const val CLOUD_NONE = "none"
        const val CLOUD_GITHUB = "github"

        const val DEFAULT_CLOUD_PATH = "pwdgen/sites.b64"

        private val KEY_LANG = stringPreferencesKey("language")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_WALLPAPER_STYLE = stringPreferencesKey("wallpaper_style")
        private val KEY_WALLPAPER_COLOR = androidx.datastore.preferences.core.longPreferencesKey("wallpaper_color")
        private val KEY_WALLPAPER_GRADIENT = androidx.datastore.preferences.core.intPreferencesKey("wallpaper_gradient")
        private val KEY_WALLPAPER_IMAGE = stringPreferencesKey("wallpaper_image")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_CLOUD_PROVIDER = stringPreferencesKey("cloud_provider")
        private val KEY_GH_OWNER = stringPreferencesKey("github_owner")
        private val KEY_GH_REPO = stringPreferencesKey("github_repo")
        private val KEY_GH_PATH = stringPreferencesKey("github_path")
        private val KEY_GH_BRANCH = stringPreferencesKey("github_branch")
        private val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync")
        private val KEY_LAST_SYNC = androidx.datastore.preferences.core.longPreferencesKey("last_sync")
        private val KEY_CUSTOM_ENDPOINT = stringPreferencesKey("custom_endpoint")
        private val KEY_SAVE_MASTER = booleanPreferencesKey("save_master")
        private val KEY_SAVE_SITE = booleanPreferencesKey("save_site")
        private val KEY_SAVE_LOGIN = booleanPreferencesKey("save_login")
    }
}