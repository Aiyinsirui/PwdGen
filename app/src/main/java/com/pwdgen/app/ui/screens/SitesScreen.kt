package com.pwdgen.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pwdgen.app.R
import com.pwdgen.app.ui.MainViewModel
import com.pwdgen.app.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    state: UiState,
    vm: MainViewModel,
    onBack: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    var showImport by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var lockInput by remember { mutableStateOf("") }

    val importOk = stringResource(R.string.import_ok)
    val locked = state.sitesLockEnabled && !state.sitesUnlocked

    LaunchedEffect(state.syncMessage) {
        if (state.syncMessage == "import_ok") {
            snackbar.showSnackbar(importOk)
            vm.clearSyncMessage()
        }
    }

    // Re-lock the site list whenever this screen is disposed (back / navigating
    // away), so returning always requires the access password again. Previously
    // `sitesUnlocked` stayed true for the whole session, which let the history
    // page open without ever prompting.
    DisposableEffect(Unit) {
        onDispose { vm.lockSites() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (locked) stringResource(R.string.sites_title)
                        else "${stringResource(R.string.sites_title)}  (${state.sites.size})",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(horizontal = 16.dp)
        ) {
            if (locked) {
                // ------------------------------------------------ lock gate
                Spacer(Modifier.height(48.dp))
                Text(
                    stringResource(R.string.sites_locked_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.sites_locked_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = lockInput,
                    onValueChange = {
                        lockInput = it
                        if (state.sitesLockError) vm.clearSitesLockError()
                    },
                    label = { Text(stringResource(R.string.sites_lock)) },
                    singleLine = true,
                    isError = state.sitesLockError,
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = if (state.sitesLockError) {
                        { Text(stringResource(R.string.sites_locked_err)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.unlockSites(lockInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.sites_locked_unlock))
                }
            } else {
                Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { importText = ""; showImport = true },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.import_sites)) }
                OutlinedButton(
                    onClick = { showExport = true },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.export_sites)) }
            }

            if (state.sites.isEmpty()) {
                Text(
                    stringResource(R.string.sites_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.sites, key = { it.site }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (entry.login.isNullOrBlank()) entry.site else "[${entry.site}:${entry.login}]",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { vm.removeSite(entry.site) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { showClear = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        stringResource(R.string.clear_all),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            }
        }
    }

    // ------------------------------------------------------------- import dlg
    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text(stringResource(R.string.import_sites)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.import_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("example.com\ngithub.com") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importRawSites(importText)
                    showImport = false
                }) { Text(stringResource(R.string.do_import)) }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ------------------------------------------------------------- export dlg
    if (showExport) {
        val encoded = remember(showExport) { vm.localEncoded() }
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text(stringResource(R.string.export_sites)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(encoded))
                        }) { Text(stringResource(R.string.copy)) }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        encoded.ifBlank { stringResource(R.string.sites_empty) },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExport = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    // -------------------------------------------------------------- clear dlg
    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text(stringResource(R.string.clear_all)) },
            text = { Text(stringResource(R.string.clear_all_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearSites()
                    showClear = false
                }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}