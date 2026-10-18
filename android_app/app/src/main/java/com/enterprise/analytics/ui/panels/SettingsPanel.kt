package com.enterprise.analytics.ui.panels

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPanel() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var bgColor by remember { mutableStateOf(sharedPrefs.getString("floating_bg_color", "#FFFFFF") ?: "#FFFFFF") }
    var textColor by remember { mutableStateOf(sharedPrefs.getString("floating_text_color", "#000000") ?: "#000000") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Floating Window Customization", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bgColor,
            onValueChange = { bgColor = it },
            label = { Text("Background Color (Hex, e.g. #FFFFFF)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = textColor,
            onValueChange = { textColor = it },
            label = { Text("Text Color (Hex, e.g. #000000)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            sharedPrefs.edit()
                .putString("floating_bg_color", bgColor)
                .putString("floating_text_color", textColor)
                .apply()
        }) {
            Text("Save Settings")
        }
    }
}
