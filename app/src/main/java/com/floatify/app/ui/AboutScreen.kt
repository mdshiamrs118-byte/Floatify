package com.floatify.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.floatify.app.FloatApps

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("About", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Floatify 0.1.0 (dummy build)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Requires Android 10 or newer. All floating apps are placeholders for now.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Text("Planned floating apps", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
        FloatApps.all.forEach {
            Text("${it.emoji}  ${it.name}", modifier = Modifier.padding(top = 6.dp))
        }
    }
}
