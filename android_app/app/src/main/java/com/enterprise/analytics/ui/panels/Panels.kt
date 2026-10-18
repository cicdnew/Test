package com.enterprise.analytics.ui.panels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.enterprise.analytics.MainViewModel

@Composable
fun ChatPanel(viewModel: MainViewModel) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadFile(context, it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { launcher.launch("*/*") }) {
                Text("Upload Dataset (CSV/Excel)")
            }
            if (uiState is com.enterprise.analytics.UiState.Loading) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(chatMessages) { msg ->
                Text("${msg.role}: ${msg.content}", modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                label = { Text("Ask about your data...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (inputText.isNotBlank()) {
                    viewModel.sendChatMessage(inputText)
                    inputText = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}

@Composable
fun AgentStatusPanel(viewModel: MainViewModel) {
    val agentStatuses by viewModel.agentStatuses.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(agentStatuses) { status ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Agent: ${status.agent}", style = MaterialTheme.typography.titleMedium)
                    Text("Status: ${status.message}")
                }
            }
        }
    }
}

@Composable
fun ToolActivityPanel(viewModel: MainViewModel) {
    val toolActivities by viewModel.toolActivities.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(toolActivities) { tool ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Action: ${tool.action}", style = MaterialTheme.typography.titleMedium)
                    Text("Arguments: ${tool.arguments}")
                }
            }
        }
    }
}
