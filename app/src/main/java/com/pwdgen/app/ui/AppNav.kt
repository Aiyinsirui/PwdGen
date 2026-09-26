package com.pwdgen.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pwdgen.app.ui.screens.GenerateScreen
import com.pwdgen.app.ui.screens.SettingsScreen
import com.pwdgen.app.ui.screens.SitesScreen

object Routes {
    const val GENERATE = "generate"
    const val SETTINGS = "settings"
    const val SITES = "sites"
}

/**
 * Single-activity navigation graph: the generator is the start destination and
 * pushes to the settings / sites screens.
 */
@Composable
fun AppNav(state: UiState, vm: MainViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.GENERATE) {
        composable(Routes.GENERATE) {
            GenerateScreen(
                state = state,
                vm = vm,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenSites = { nav.navigate(Routes.SITES) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = state,
                vm = vm,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SITES) {
            SitesScreen(
                state = state,
                vm = vm,
                onBack = { nav.popBackStack() },
            )
        }
    }
}