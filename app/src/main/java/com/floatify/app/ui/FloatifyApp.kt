package com.floatify.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class Tab(val label: String, val emoji: String) {
    Home("Home", "🏠"),
    Settings("Settings", "⚙️"),
    About("About", "ℹ️"),
}

@Composable
fun FloatifyApp() {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.emoji) },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { pad ->
        val m = Modifier.padding(pad)
        when (tab) {
            Tab.Home -> HomeScreen(m)
            Tab.Settings -> SettingsScreen(m)
            Tab.About -> AboutScreen(m)
        }
    }
}
