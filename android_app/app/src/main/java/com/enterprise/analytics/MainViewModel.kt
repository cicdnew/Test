package com.enterprise.analytics

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enterprise.analytics.data.AppDatabase
import com.enterprise.analytics.data.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

data class ChatMessage(val role: String, val content: String)
data class AgentStatus(val agent: String, val message: String)
data class ToolActivity(val action: String, val arguments: String)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _agentStatuses = MutableStateFlow<List<AgentStatus>>(emptyList())
    val agentStatuses: StateFlow<List<AgentStatus>> = _agentStatuses

    private val _toolActivities = MutableStateFlow<List<ToolActivity>>(emptyList())
    val toolActivities: StateFlow<List<ToolActivity>> = _toolActivities

    private val database = AppDatabase.getDatabase(application)
    val reminders: StateFlow<List<Reminder>> = database.reminderDao().getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(context: Context, title: String, dateInMillis: Long, isRecurring: Boolean, recurrenceType: String, videoUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val reminder = Reminder(
                title = title,
                dateInMillis = dateInMillis,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                videoUri = videoUri
            )
            val id = database.reminderDao().insert(reminder)
            AlarmHelper.scheduleReminder(
                context = context,
                id = id.toInt(),
                title = title,
                dateInMillis = dateInMillis,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                videoUriStr = videoUri
            )
        }
    }

    fun uploadFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading
            delay(1000) // Simulate upload
            _uiState.value = UiState.Success("File processed locally (mock)")
        }
    }

    fun sendChatMessage(message: String) {
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(ChatMessage("user", message))
        _chatMessages.value = currentMessages

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading
            delay(500)
            _agentStatuses.value = _agentStatuses.value + AgentStatus("LocalAgent", "Thinking...")
            delay(500)
            _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "This is a local dummy response to: \"$message\"")
            _uiState.value = UiState.Idle
        }
    }
}
