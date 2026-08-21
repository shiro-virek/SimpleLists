package com.simplelists.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simplelists.app.ui.lists.ListsScreen
import com.simplelists.app.ui.settings.SettingsScreen
import com.simplelists.app.ui.theme.SimpleListsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Incrementa al importar una BD para recrear los ViewModels con la nueva instancia
            var dbEpoch by remember { mutableIntStateOf(0) }
            SimpleListsTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "lists") {
                    composable("lists") {
                        ListsScreen(
                            dbEpoch = dbEpoch,
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            dbEpoch = dbEpoch,
                            onBack = { navController.popBackStack() },
                            onDbReplaced = { dbEpoch++ }
                        )
                    }
                }
            }
        }
    }
}
