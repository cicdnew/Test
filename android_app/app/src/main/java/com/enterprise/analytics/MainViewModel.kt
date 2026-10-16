package com.enterprise.analytics

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import org.json.JSONObject

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

data class ChatMessage(val role: String, val content: String)
data class AgentStatus(val agent: String, val message: String)
data class ToolActivity(val action: String, val arguments: String)

class MainViewModel : ViewModel() {
    private val api = NetworkModule.retrofit.create(ApiService::class.java)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _agentStatuses = MutableStateFlow<List<AgentStatus>>(emptyList())
    val agentStatuses: StateFlow<List<AgentStatus>> = _agentStatuses

    private val _toolActivities = MutableStateFlow<List<ToolActivity>>(emptyList())
    val toolActivities: StateFlow<List<ToolActivity>> = _toolActivities

    fun uploadFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading
            try {
                val file = getFileFromUri(context, uri)
                if (file != null) {
                    val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                    val response = api.uploadFile(body)
                    _uiState.value = UiState.Success(response.message)
                } else {
                    _uiState.value = UiState.Error("Could not process file")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Upload failed: ${e.message}")
            }
        }
    }

    fun sendChatMessage(message: String) {
        val currentMessages = _chatMessages.value.toMutableList()
        currentMessages.add(ChatMessage("user", message))
        _chatMessages.value = currentMessages

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Loading
            try {
                val request = ChatRequest(model = "kilo-pro", messages = listOf(message), stream = true)
                val responseBody = api.streamChat(request)

                val source = responseBody.source()
                var fullResponse = ""

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val jsonStr = line.substring(6)
                        try {
                            val json = JSONObject(jsonStr)
                            val type = json.optString("type")

                            withContext(Dispatchers.Main) {
                                when (type) {
                                    "status" -> {
                                        val agent = json.optString("agent")
                                        val msg = json.optString("message")
                                        _agentStatuses.value = _agentStatuses.value + AgentStatus(agent, msg)
                                    }
                                    "tool" -> {
                                        val agent = json.optString("agent")
                                        val action = json.optString("action")
                                        val args = json.optJSONObject("arguments")?.toString() ?: "{}"
                                        _toolActivities.value = _toolActivities.value + ToolActivity(action, args)
                                        _agentStatuses.value = _agentStatuses.value + AgentStatus(agent, "Executed $action")
                                    }
                                    "content" -> {
                                        fullResponse += json.optString("content")
                                    }
                                    "done" -> {
                                        _chatMessages.value = _chatMessages.value + ChatMessage("assistant", fullResponse)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignored or log error parsing stream segment
                        }
                    }
                }
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Chat failed: ${e.message}")
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        var fileName = "temp_file"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) fileName = it.getString(nameIndex)
            }
        }

        val tempFile = File(context.cacheDir, fileName)
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
