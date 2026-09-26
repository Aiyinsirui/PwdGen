package com.pwdgen.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pwdgen.app.R
import com.pwdgen.app.data.SettingsRepository
import com.pwdgen.app.ui.MainViewModel
import com.pwdgen.app.ui.UiState
import com.pwdgen.app.ui.components.Section
import com.pwdgen.app.ui.theme.Gradients

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    vm: MainViewModel,
    onBack: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val syncOk = stringResource(R.string.sync_ok)
    val syncNotConfigured = stringResource(R.string.sync_not_configured)
    val syncFailed = stringResource(R.string.sync_failed)

    LaunchedEffect(state.syncMessage) {
        val msg = state.syncMessage ?: return@LaunchedEffect
        val text = when {
            msg == "sync_ok" -> syncOk
            msg == "sync_not_configured" -> syncNotConfigured
            msg.startsWith("sync_failed") -> "$syncFailed: ${msg.removePrefix("sync_failed:")}"
            else -> msg
        }
        snackbar.showSnackbar(text)
        vm.clearSyncMessage()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Persist read permission so the URI survives restarts.
            vm.setWallpaperImage(uri.toString())
            vm.setWallpaperStyle(SettingsRepository.WALL_IMAGE)
        }
    }

    // sites access-password setup dialog state
    var showSetLock by remember { mutableStateOf(false) }
    var lockOld by remember { mutableStateOf("") }
    var lockNew by remember { mutableStateOf("") }
    var lockConfirm by remember { mutableStateOf("") }
    var lockErr by remember { mutableStateOf<Int?>(null) }

    // remove access-password dialog state
    var showRemoveLock by remember { mutableStateOf(false) }
    var removePw by remember { mutableStateOf("") }
    var removeErr by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ============================================================ LOOK
            Section(title = stringResource(R.string.section_look)) {
                // language
                Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChoiceChip(stringResource(R.string.lang_system), state.language == SettingsRepository.LANG_SYSTEM) {
                        vm.setLanguage(SettingsRepository.LANG_SYSTEM)
                    }
                    ChoiceChip(stringResource(R.string.lang_en), state.language == SettingsRepository.LANG_EN) {
                        vm.setLanguage(SettingsRepository.LANG_EN)
                    }
                    ChoiceChip(stringResource(R.string.lang_zh), state.language == SettingsRepository.LANG_ZH) {
                        vm.setLanguage(SettingsRepository.LANG_ZH)
                    }
                }

                // theme
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChoiceChip(stringResource(R.string.theme_system), state.themeMode == SettingsRepository.THEME_SYSTEM) {
                        vm.setThemeMode(SettingsRepository.THEME_SYSTEM)
                    }
                    ChoiceChip(stringResource(R.string.theme_light), state.themeMode == SettingsRepository.THEME_LIGHT) {
                        vm.setThemeMode(SettingsRepository.THEME_LIGHT)
                    }
                    ChoiceChip(stringResource(R.string.theme_dark), state.themeMode == SettingsRepository.THEME_DARK) {
                        vm.setThemeMode(SettingsRepository.THEME_DARK)
                    }
                }

                ToggleRow(
                    label = stringResource(R.string.dynamic_color),
                    checked = state.dynamicColor,
                    onCheckedChange = vm::setDynamicColor
                )
            }

            // ======================================================= WALLPAPER
            Section(title = stringResource(R.string.section_wallpaper)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChoiceChip(stringResource(R.string.wall_solid), state.wallpaperStyle == SettingsRepository.WALL_SOLID) {
                        vm.setWallpaperStyle(SettingsRepository.WALL_SOLID)
                    }
                    ChoiceChip(stringResource(R.string.wall_gradient), state.wallpaperStyle == SettingsRepository.WALL_GRADIENT) {
                        vm.setWallpaperStyle(SettingsRepository.WALL_GRADIENT)
                    }
                    ChoiceChip(stringResource(R.string.wall_image), state.wallpaperStyle == SettingsRepository.WALL_IMAGE) {
                        vm.setWallpaperStyle(SettingsRepository.WALL_IMAGE)
                    }
                }

                // solid swatches
                if (state.wallpaperStyle == SettingsRepository.WALL_SOLID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(Gradients.solidPresets) { _, argb ->
                            val color = Color(argb)
                            val selected = state.wallpaperColor == argb
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { vm.setWallpaperColor(argb) }
                            )
                        }
                    }
                }

                // gradients
                if (state.wallpaperStyle == SettingsRepository.WALL_GRADIENT) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(Gradients.presets) { idx, preset ->
                            val selected = state.wallpaperGradientIndex == idx
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .background(preset.brush(), RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { vm.setWallpaperGradient(idx) }
                            )
                        }
                    }
                }

                // image
                if (state.wallpaperStyle == SettingsRepository.WALL_IMAGE) {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.pick_image))
                    }
                    if (!state.wallpaperImageUri.isNullOrBlank()) {
                        TextButtonRow(
                            text = stringResource(R.string.clear_image),
                            onClick = { vm.setWallpaperImage(null) }
                        )
                    }
                }
            }

            // ========================================================== SECURITY
            Section(title = stringResource(R.string.section_security)) {
                ToggleRow(
                    label = stringResource(R.string.save_master),
                    sub = stringResource(R.string.save_master_desc),
                    checked = state.saveMaster,
                    onCheckedChange = vm::setSaveMaster
                )
                if (state.rememberedMaster) {
                    TextButtonRow(
                        text = stringResource(R.string.forget_master),
                        onClick = { vm.forgetMaster() }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.sites_lock),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.sites_lock_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                TextButtonRow(
                    text = stringResource(
                        if (state.sitesLockEnabled) R.string.sites_lock_change
                        else R.string.sites_lock_set
                    ),
                    onClick = {
                        lockOld = ""
                        lockNew = ""
                        lockConfirm = ""
                        lockErr = null
                        showSetLock = true
                    }
                )
                if (state.sitesLockEnabled) {
                    TextButtonRow(
                        text = stringResource(R.string.sites_lock_remove),
                        onClick = {
                            removePw = ""
                            removeErr = false
                            showRemoveLock = true
                        }
                    )
                }
            }

            // ============================================================= CLOUD
            Section(title = stringResource(R.string.section_cloud)) {
                Text(
                    stringResource(R.string.cloud_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.ghOwner,
                    onValueChange = vm::onGhOwner,
                    label = { Text(stringResource(R.string.gh_owner)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.ghRepo,
                    onValueChange = vm::onGhRepo,
                    label = { Text(stringResource(R.string.gh_repo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.ghPath,
                    onValueChange = vm::onGhPath,
                    label = { Text(stringResource(R.string.gh_path)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.ghBranch,
                    onValueChange = vm::onGhBranch,
                    label = { Text(stringResource(R.string.gh_branch)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))

                var tokenVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = state.ghToken,
                    onValueChange = vm::onGhToken,
                    label = { Text(stringResource(R.string.gh_token)) },
                    singleLine = true,
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                ToggleRow(
                    label = stringResource(R.string.auto_sync),
                    sub = stringResource(R.string.auto_sync_desc),
                    checked = state.autoSync,
                    onCheckedChange = vm::setAutoSync
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.syncNow(silent = false) },
                        enabled = !state.syncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.syncing) stringResource(R.string.syncing) else stringResource(R.string.sync_now))
                    }
                }

                if (state.lastSyncAt > 0) {
                    val d = remember(state.lastSyncAt) {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(state.lastSyncAt))
                    }
                    Text(
                        stringResource(R.string.last_sync, d),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ---- set / change sites access-password dialog ----
    if (showSetLock) {
        val errEmpty = stringResource(R.string.sites_lock_err_empty)
        val errMismatch = stringResource(R.string.sites_lock_err_mismatch)
        val errOld = stringResource(R.string.sites_lock_err_old)

        AlertDialog(
            onDismissRequest = { showSetLock = false },
            title = {
                Text(
                    stringResource(
                        if (state.sitesLockEnabled) R.string.sites_lock_change
                        else R.string.sites_lock_set
                    )
                )
            },
            text = {
                Column {
                    // Current password field — only when changing an existing one.
                    if (state.sitesLockEnabled) {
                        OutlinedTextField(
                            value = lockOld,
                            onValueChange = { lockOld = it; lockErr = null },
                            label = { Text(stringResource(R.string.sites_lock_old)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = lockErr == 3,
                            supportingText = {
                                if (lockErr == 3) Text(errOld)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    OutlinedTextField(
                        value = lockNew,
                        onValueChange = { lockNew = it; lockErr = null },
                        label = { Text(stringResource(R.string.sites_lock_new)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = lockErr == 1 || lockErr == 2,
                        supportingText = {
                            when (lockErr) {
                                1 -> Text(errEmpty)
                                2 -> Text(errMismatch)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = lockConfirm,
                        onValueChange = { lockConfirm = it; lockErr = null },
                        label = { Text(stringResource(R.string.sites_lock_confirm)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = lockErr == 1 || lockErr == 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        state.sitesLockEnabled && lockOld.isBlank() -> lockErr = 3
                        lockNew.isBlank() -> lockErr = 1
                        lockNew != lockConfirm -> lockErr = 2
                        else -> {
                            val ok = if (state.sitesLockEnabled) {
                                vm.changeSitesLock(lockOld, lockNew)
                            } else {
                                vm.setSitesLock(lockNew); true
                            }
                            if (ok) {
                                showSetLock = false
                            } else {
                                lockErr = 3
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.sites_lock_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetLock = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // ---- remove sites access-password dialog (requires current password) ----
    if (showRemoveLock) {
        val errOld = stringResource(R.string.sites_lock_err_old)

        AlertDialog(
            onDismissRequest = { showRemoveLock = false },
            title = { Text(stringResource(R.string.sites_lock_remove_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.sites_lock_remove_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = removePw,
                        onValueChange = { removePw = it; removeErr = false },
                        label = { Text(stringResource(R.string.sites_lock_old)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = removeErr,
                        supportingText = {
                            if (removeErr) Text(errOld)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        removePw.isBlank() -> removeErr = true
                        vm.removeSitesLock(removePw) -> {
                            showRemoveLock = false
                            removePw = ""
                            removeErr = false
                        }
                        else -> removeErr = true
                    }
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveLock = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    sub: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (sub != null) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TextButtonRow(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}