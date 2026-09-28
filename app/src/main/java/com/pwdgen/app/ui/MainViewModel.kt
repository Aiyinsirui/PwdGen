package com.pwdgen.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pwdgen.app.cloud.CloudException
import com.pwdgen.app.cloud.GitHubStorage
import com.pwdgen.app.cloud.RemoteStorage
import com.pwdgen.app.cloud.SyncResult
import com.pwdgen.app.core.PasswordGenerator
import com.pwdgen.app.core.SiteEntry
import com.pwdgen.app.crypto.SecureStore
import com.pwdgen.app.data.SettingsRepository
import com.pwdgen.app.data.SiteCodec
import com.pwdgen.app.data.SiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the screens render from. */
data class UiState(
    val site: String = "",
    val login: String = "",
    val master: String = "",
    val length: Int = 10,
    val counter: Int = 1,
    val useSpecial: Boolean = true,
    val generated: String = "",
    val sites: List<SiteEntry> = emptyList(),
    val message: String? = null,
    val syncMessage: String? = null,
    val syncing: Boolean = false,
    val rememberedMaster: Boolean = false,
    val cloudConfigured: Boolean = false,
    // site list access lock
    val sitesLockEnabled: Boolean = false,
    val sitesUnlocked: Boolean = false,
    val sitesLockError: Boolean = false,
    // settings
    val language: String = SettingsRepository.LANG_SYSTEM,
    val themeMode: String = SettingsRepository.THEME_SYSTEM,
    val dynamicColor: Boolean = false,
    val wallpaperStyle: String = SettingsRepository.WALL_SOLID,
    val wallpaperColor: Long = 0xFF101623L,
    val wallpaperGradientIndex: Int = 0,
    val wallpaperImageUri: String? = null,
    val saveMaster: Boolean = false,
    val saveSite: Boolean = true,
    val saveLogin: Boolean = false,
    val autoSync: Boolean = false,
    val lastSyncAt: Long = 0L,
    // github form
    val ghOwner: String = "",
    val ghRepo: String = "",
    val ghPath: String = SettingsRepository.DEFAULT_CLOUD_PATH,
    val ghBranch: String = "main",
    val ghToken: String = "",
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val sites = SiteRepository(app)
    private val settings = SettingsRepository(app)
    private val secure = SecureStore(app)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                sites = sites.load(),
                language = settings.language.first(),
                themeMode = settings.themeMode.first(),
                dynamicColor = settings.dynamicColor.first(),
                wallpaperStyle = settings.wallpaperStyle.first(),
                wallpaperColor = settings.wallpaperColor.first(),
                wallpaperGradientIndex = settings.wallpaperGradientIndex.first(),
                wallpaperImageUri = settings.wallpaperImageUri.first()?.ifBlank { null },
                saveMaster = settings.saveMaster.first(),
                saveSite = settings.saveSite.first(),
                saveLogin = settings.saveLogin.first(),
                autoSync = settings.autoSync.first(),
                lastSyncAt = settings.lastSyncAt.first(),
                ghOwner = settings.githubOwner.first(),
                ghRepo = settings.githubRepo.first(),
                ghPath = settings.githubPath.first(),
                ghBranch = settings.githubBranch.first(),
                ghToken = secure.getString(SecureStore.KEY_CLOUD_TOKEN).orEmpty(),
                rememberedMaster = secure.getString(SecureStore.KEY_SAVED_MASTER) != null,
                sitesLockEnabled = !secure.getString(SecureStore.KEY_SITES_LOCK).isNullOrEmpty(),
            )
            refreshCloudConfigured()
            if (_ui.value.saveMaster && _ui.value.rememberedMaster) {
                val saved = secure.getString(SecureStore.KEY_SAVED_MASTER)
                if (!saved.isNullOrEmpty()) {
                    _ui.value = _ui.value.copy(master = saved)
                }
            }
            if (_ui.value.autoSync) syncNow(silent = true)
        }
    }

    // ------------------------------------------------------------------ form

    fun onSite(v: String) {
        _ui.value = _ui.value.copy(site = v)
    }

    fun onLogin(v: String) = set { copy(login = v) }
    fun onMaster(v: String) = set { copy(master = v) }
    fun onLength(v: Int) = set { copy(length = v.coerceIn(PasswordGenerator.MIN_LENGTH, 64)) }
    fun onCounter(v: Int) = set { copy(counter = v.coerceAtLeast(1)) }
    fun onUseSpecial(v: Boolean) = set { copy(useSpecial = v) }

    fun clearMessage() = set { copy(message = null) }
    fun clearSyncMessage() = set { copy(syncMessage = null) }

    private fun set(block: UiState.() -> UiState) {
        _ui.value = _ui.value.block()
    }

    // -------------------------------------------------------------- generate

    /**
     * Generate a password and (local-first) persist the site immediately when
     * it is new. Cloud push happens only via [syncNow] / auto-sync.
     */
    fun generate() {
        val s = _ui.value
        if (s.site.isBlank()) {
            set { copy(message = "err_site_required") }
            return
        }
        if (s.login.isBlank()) {
            set { copy(message = "err_login_required") }
            return
        }
        if (s.master.isBlank()) {
            set { copy(message = "err_master_required") }
            return
        }
        viewModelScope.launch {
            val pwd = withContext(Dispatchers.Default) {
                try {
                    PasswordGenerator.generate(
                        site = s.site.trim(),
                        login = s.login.trim(),
                        master = s.master,
                        length = s.length,
                        useSpecial = s.useSpecial,
                        counter = s.counter
                    )
                } catch (e: Exception) {
                    null
                }
            }
            if (pwd == null) {
                set { copy(message = "err_generate") }
                return@launch
            }
            val (list, isNew) = if (s.saveSite) {
                sites.add(
                    s.site.trim(),
                    if (s.saveLogin) s.login.trim().ifBlank { null } else null
                )
            } else {
                sites.load() to false
            }
            set { copy(generated = pwd, sites = list) }
            if (isNew && _ui.value.autoSync) {
                // Upload the refreshed list; silent so a failure doesn't nag.
                syncNow(silent = true)
            }
        }
    }

    /** Hides the generated-password card (the ❌ button on the result card). */
    fun clearGenerated() = set { copy(generated = "") }

    // ------------------------------------------------------------ master opt

    fun setSaveMaster(enabled: Boolean) {
        viewModelScope.launch {
            settings.setSaveMaster(enabled)
            if (enabled) {
                val m = _ui.value.master
                if (m.isNotEmpty()) {
                    secure.putString(SecureStore.KEY_SAVED_MASTER, m)
                    set { copy(saveMaster = true, rememberedMaster = true) }
                } else {
                    set { copy(saveMaster = true) }
                }
            } else {
                secure.putString(SecureStore.KEY_SAVED_MASTER, null)
                set { copy(saveMaster = false, rememberedMaster = false) }
            }
        }
    }

    fun setSaveSite(enabled: Boolean) {
        viewModelScope.launch {
            settings.setSaveSite(enabled)
            set { copy(saveSite = enabled) }
        }
    }

    fun setSaveLogin(enabled: Boolean) {
        viewModelScope.launch {
            settings.setSaveLogin(enabled)
            set { copy(saveLogin = enabled) }
        }
    }

    fun forgetMaster() {
        viewModelScope.launch {
            secure.putString(SecureStore.KEY_SAVED_MASTER, null)
            settings.setSaveMaster(false)
            set { copy(saveMaster = false, rememberedMaster = false, master = "") }
        }
    }

    // -------------------------------------------------------- sites access lock

    /**
     * Set / change the optional "sites access password". Stored encrypted in
     * [SecureStore] only — never uploaded. Returns nothing; the UI validates
     * the two inputs before calling this.
     */
    fun setSitesLock(password: String) {
        secure.putString(SecureStore.KEY_SITES_LOCK, password)
        // After setting, treat the current session as already unlocked.
        set { copy(sitesLockEnabled = true, sitesUnlocked = true, sitesLockError = false) }
    }

    /**
     * Change the access password. Requires the OLD password to match before
     * writing the new one. Returns true on success, false if [old] is wrong.
     * Never uploaded — stored encrypted in [SecureStore] only.
     */
    fun changeSitesLock(old: String, new: String): Boolean {
        val stored = secure.getString(SecureStore.KEY_SITES_LOCK)
        if (stored.isNullOrEmpty() || stored != old) {
            set { copy(sitesLockError = true) }
            return false
        }
        secure.putString(SecureStore.KEY_SITES_LOCK, new)
        set { copy(sitesLockEnabled = true, sitesUnlocked = true, sitesLockError = false) }
        return true
    }

    /**
     * Remove the access password. Requires the current password to match.
     * Returns true on success, false if [old] is wrong.
     */
    fun removeSitesLock(old: String): Boolean {
        val stored = secure.getString(SecureStore.KEY_SITES_LOCK)
        if (!stored.isNullOrEmpty() && stored != old) {
            set { copy(sitesLockError = true) }
            return false
        }
        secure.putString(SecureStore.KEY_SITES_LOCK, null)
        set { copy(sitesLockEnabled = false, sitesUnlocked = true, sitesLockError = false) }
        return true
    }

    /** Verify the entered password against the stored one. */
    fun unlockSites(entered: String): Boolean {
        val stored = secure.getString(SecureStore.KEY_SITES_LOCK)
        val ok = !stored.isNullOrEmpty() && stored == entered
        set { copy(sitesUnlocked = ok, sitesLockError = !ok) }
        return ok
    }

    /** Re-lock the site list (e.g. when leaving the screen). */
    fun lockSites() {
        if (_ui.value.sitesLockEnabled) {
            set { copy(sitesUnlocked = false, sitesLockError = false) }
        }
    }

    fun clearSitesLockError() = set { copy(sitesLockError = false) }

    // ----------------------------------------------------------------- sites

    fun removeSite(name: String) {
        viewModelScope.launch {
            sites.remove(name)
            val list = sites.load()
            set { copy(sites = list) }
            if (_ui.value.autoSync) syncNow(silent = true)
        }
    }

    fun clearSites() {
        viewModelScope.launch {
            sites.clear()
            set { copy(sites = emptyList()) }
        }
    }

    fun importRawSites(raw: String) {
        viewModelScope.launch {
            val incoming = SiteCodec.parseRaw(raw)
            val list = sites.merge(incoming)
            set { copy(sites = list, syncMessage = "import_ok") }
            if (_ui.value.autoSync) syncNow(silent = true)
        }
    }

    /** Read-only helper for the export dialog. */
    fun localEncoded(): String = SiteCodec.encode(_ui.value.sites)

    // ----------------------------------------------------------------- cloud

    fun onGhOwner(v: String) { set { copy(ghOwner = v) }; persistCloud() }
    fun onGhRepo(v: String) { set { copy(ghRepo = v) }; persistCloud() }
    fun onGhPath(v: String) { set { copy(ghPath = v) }; persistCloud() }
    fun onGhBranch(v: String) { set { copy(ghBranch = v) }; persistCloud() }
    fun onGhToken(v: String) {
        set { copy(ghToken = v) }
        secure.putString(SecureStore.KEY_CLOUD_TOKEN, v)
        viewModelScope.launch { refreshCloudConfigured() }
    }

    fun setAutoSync(v: Boolean) {
        viewModelScope.launch {
            settings.setAutoSync(v)
            set { copy(autoSync = v) }
            if (v) syncNow(silent = true)
        }
    }

    private fun persistCloud() {
        val s = _ui.value
        viewModelScope.launch {
            settings.setGithubOwner(s.ghOwner)
            settings.setGithubRepo(s.ghRepo)
            settings.setGithubPath(s.ghPath.ifBlank { SettingsRepository.DEFAULT_CLOUD_PATH })
            settings.setGithubBranch(s.ghBranch.ifBlank { "main" })
            settings.setCloudProvider(
                if (s.ghOwner.isNotBlank() && s.ghRepo.isNotBlank())
                    SettingsRepository.CLOUD_GITHUB else SettingsRepository.CLOUD_NONE
            )
            refreshCloudConfigured()
        }
    }

    private fun provider(): RemoteStorage {
        val s = _ui.value
        // Normalize owner/repo: accept a full URL or "owner/repo.git" pasted
        // into either field, and strip "https://github.com/" / ".git".
        val norm = normalizeRepo(s.ghOwner.trim(), s.ghRepo.trim())
        return GitHubStorage(
            owner = norm.first,
            repo = norm.second,
            path = s.ghPath.trim().ifBlank { SettingsRepository.DEFAULT_CLOUD_PATH },
            branch = s.ghBranch.trim().ifBlank { "main" },
            token = s.ghToken.trim()
        )
    }
    /**
     * Split a GitHub repository reference into (owner, repo).
     * Accepted forms (in either field):
     *   https://github.com/owner/repo.git
     *   owner/repo.git
     *   /owner/repo.git
     * A bare "repo" keeps the previously supplied owner.
     */
    private fun normalizeRepo(ownerIn: String, repoIn: String): Pair<String, String> {
        var owner = ownerIn
        var repo = repoIn
        // If the repo field looks like a path/URL, split it.
        val candidate = repo.removePrefix("https://").removePrefix("http://")
            .removePrefix("github.com/").trimStart('/')
        if (candidate.contains('/')) {
            val parts = candidate.split('/')
            owner = parts[0].trim().removeSuffix(".git").removeSuffix(".Git")
            repo = parts.drop(1).joinToString("/").trim()
        } else {
            repo = repo.removeSuffix(".git").removeSuffix(".Git").trim()
        }
        owner = owner.removePrefix("https://").removePrefix("http://")
            .removePrefix("github.com/").trim('/').removeSuffix(".git").trim()
        return owner to repo
    }

    private suspend fun refreshCloudConfigured() {
        val ok = provider().isConfigured()
        set { copy(cloudConfigured = ok) }
    }

    /**
     * Full sync:
     *  1. pull remote blob (if any) and merge unknown sites locally;
     *  2. push the merged local list back so the cloud is the union.
     * This is safe to run repeatedly and never deletes remotely-known sites.
     */
    fun syncNow(silent: Boolean = false) {
        val storage = provider()
        if (!storage.isConfigured()) {
            if (!silent) set { copy(syncMessage = "sync_not_configured") }
            return
        }
        if (_ui.value.syncing) return
        set { copy(syncing = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val remote = storage.pull()
                    val remoteSites = if (remote.isNullOrBlank()) emptyList() else sites.decodePayload(remote)
                    if (remoteSites.isNotEmpty()) sites.merge(remoteSites)
                    val localEncoded = sites.encodePayload(sites.load())
                    if (localEncoded.isNotEmpty()) storage.push(localEncoded)
                    SyncResult.Success
                } catch (e: CloudException) {
                    SyncResult.Failed(e.message ?: "cloud error")
                } catch (e: Exception) {
                    SyncResult.Failed(e.message ?: "unknown error")
                }
            }
            when (result) {
                is SyncResult.Success -> {
                    val now = System.currentTimeMillis()
                    settings.setLastSyncAt(now)
                    val refreshed = this@MainViewModel.sites.load()
                    set {
                        copy(
                            syncing = false,
                            sites = refreshed,
                            lastSyncAt = now,
                            syncMessage = if (silent) null else "sync_ok"
                        )
                    }
                }
                is SyncResult.NotConfigured -> set { copy(syncing = false, syncMessage = "sync_not_configured") }
                is SyncResult.Failed -> {
                    set {
                        copy(
                            syncing = false,
                            syncMessage = if (silent) null else "sync_failed:${result.message}"
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------- settings

    fun setLanguage(v: String) = viewModelScope.launch {
        settings.setLanguage(v); set { copy(language = v) }
    }

    fun setThemeMode(v: String) = viewModelScope.launch {
        settings.setThemeMode(v); set { copy(themeMode = v) }
    }

    fun setDynamicColor(v: Boolean) = viewModelScope.launch {
        settings.setDynamicColor(v); set { copy(dynamicColor = v) }
    }

    fun setWallpaperStyle(v: String) = viewModelScope.launch {
        settings.setWallpaperStyle(v); set { copy(wallpaperStyle = v) }
    }

    fun setWallpaperColor(argb: Long) = viewModelScope.launch {
        settings.setWallpaperColor(argb); set { copy(wallpaperColor = argb) }
    }

    fun setWallpaperGradient(index: Int) = viewModelScope.launch {
        settings.setWallpaperGradientIndex(index); set { copy(wallpaperGradientIndex = index) }
    }

    fun setWallpaperImage(uri: String?) = viewModelScope.launch {
        settings.setWallpaperImageUri(uri); set { copy(wallpaperImageUri = uri) }
    }
}