package com.floatify.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatify.app.Prefs

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var resetCount by remember { mutableIntStateOf(0) }

    // Re-create all rows after a reset so they reload defaults
    key(resetCount) {
        Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)

            Section("General")
            SwitchRow("Start on boot", "Launch Floatify when the phone starts (placeholder)", "boot", false)
            SwitchRow("Show notification", "Keep a notification while windows are open (placeholder)", "notif", true)
            SwitchRow("Remember window positions", "Reopen windows where you left them (placeholder)", "remember_pos", true)

            Section("Appearance")
            SwitchRow("Use dynamic colors", "Follow your wallpaper colors on Android 12+ (placeholder)", "dynamic", true)
            SliderRow("Window opacity", Prefs.KEY_OPACITY, 0.3f..1f, 0.95f) { "${(it * 100).toInt()}%" }
            SliderRow("Default window width", Prefs.KEY_WIDTH, 200f..400f, 280f) { "${it.toInt()} dp" }

            Section("Behavior")
            SwitchRow("Snap to screen edges", "Windows stick to edges when dragged (placeholder)", "snap", false)
            SwitchRow("Haptic feedback", "Vibrate on window actions (placeholder)", "haptic", true)
            SwitchRow("Close windows on screen off", "Auto-close everything when the screen turns off (placeholder)", "auto_close", false)

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { Prefs.clear(ctx); resetCount++ },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset to defaults") }
            Text(
                "Opacity and width apply to newly opened windows.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, key: String, default: Boolean) {
    val ctx = LocalContext.current
    var checked by remember { mutableStateOf(Prefs.getBool(ctx, key, default)) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = { checked = it; Prefs.setBool(ctx, key, it) })
    }
}

@Composable
private fun SliderRow(
    title: String,
    key: String,
    range: ClosedFloatingPointRange<Float>,
    default: Float,
    format: (Float) -> String,
) {
    val ctx = LocalContext.current
    var value by remember { mutableStateOf(Prefs.getFloat(ctx, key, default)) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(format(value), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { Prefs.setFloat(ctx, key, value) },
            valueRange = range,
        )
    }
}
