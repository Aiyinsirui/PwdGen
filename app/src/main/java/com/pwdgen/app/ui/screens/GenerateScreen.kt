package com.pwdgen.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwdgen.app.R
import com.pwdgen.app.core.PasswordGenerator
import com.pwdgen.app.data.SettingsRepository
import com.pwdgen.app.ui.MainViewModel
import com.pwdgen.app.ui.UiState
import com.pwdgen.app.ui.components.Section
import com.pwdgen.app.ui.components.StrengthBar

/**
 * Main page -- closely mirrors LessPass's generator form:
 * site / login / master on top, options in the middle, result at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    state: UiState,
    vm: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenSites: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current

    // Localised message resolver (state stores message keys, not text).
    val errSite = stringResource(R.string.err_site_required)
    val errLogin = stringResource(R.string.err_login_required)
    val errMaster = stringResource(R.string.err_master_required)
    val errGen = stringResource(R.string.err_generate)

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        val text = when {
            msg == "err_site_required" -> errSite
            msg == "err_login_required" -> errLogin
            msg == "err_master_required" -> errMaster
            msg == "err_generate" -> errGen
            else -> msg
        }
        snackbar.showSnackbar(text)
        vm.clearMessage()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = onOpenSites) {
                        Icon(Icons.Filled.List, contentDescription = stringResource(R.string.sites_title))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
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
            // ------------------------------------------------------------- site
            // Field order follows the pwdgen CLI: site -> login -> master password.
            OutlinedTextField(
                value = state.site,
                onValueChange = vm::onSite,
                label = { Text(stringResource(R.string.site)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // ------------------------------------------------------------ login
            OutlinedTextField(
                value = state.login,
                onValueChange = vm::onLogin,
                label = { Text(stringResource(R.string.login)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // ------------------------------------------------ master password
            var masterVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = state.master,
                onValueChange = vm::onMaster,
                label = { Text(stringResource(R.string.master_password)) },
                singleLine = true,
                visualTransformation = if (masterVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { masterVisible = !masterVisible }) {
                        Icon(
                            if (masterVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ---------------------------------------------------------- options
            Section(title = stringResource(R.string.options)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.length).format(state.length),
                        modifier = Modifier.width(110.dp)
                    )
                    Slider(
                        value = state.length.toFloat(),
                        onValueChange = { vm.onLength(it.toInt()) },
                        valueRange = PasswordGenerator.MIN_LENGTH.toFloat()..32f,
                        steps = 32 - PasswordGenerator.MIN_LENGTH - 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.counter).format(state.counter),
                        modifier = Modifier.width(110.dp)
                    )
                    Slider(
                        value = state.counter.toFloat(),
                        onValueChange = { vm.onCounter(it.toInt()) },
                        valueRange = 1f..20f,
                        steps = 18,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.use_special), modifier = Modifier.weight(1f))
                    Switch(checked = state.useSpecial, onCheckedChange = vm::onUseSpecial)
                }

                // strength preview
                val bits = remember(state.length, state.useSpecial) {
                    PasswordGenerator.entropyBits(state.length, state.useSpecial)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.strength).format(bits.toInt()),
                        modifier = Modifier.width(130.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    StrengthBar(
                        percent = ((bits / 128.0) * 100).toInt(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --------------------------------------------------------- generate
            Button(
                onClick = { vm.generate() },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.generate))
            }

            // ----------------------------------------------------------- result
            if (state.generated.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.generated_password),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = state.generated,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(state.generated))
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy))
                            }
                            IconButton(onClick = { vm.clearGenerated() }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.close))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
