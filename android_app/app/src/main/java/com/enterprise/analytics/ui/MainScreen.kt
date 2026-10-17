package com.enterprise.analytics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.enterprise.analytics.MainViewModel
import com.enterprise.analytics.ui.panels.AgentStatusPanel
import com.enterprise.analytics.ui.panels.ChatPanel
import com.enterprise.analytics.ui.panels.ToolActivityPanel
import com.enterprise.analytics.ui.panels.PlannerPanel

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chat", "Planner", "Agent Status", "Tool Activity")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> ChatPanel(viewModel)
            1 -> PlannerPanel(viewModel)
            2 -> AgentStatusPanel(viewModel)
            3 -> ToolActivityPanel(viewModel)
        }
    }
}
