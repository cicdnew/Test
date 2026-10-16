package com.enterprise.analytics

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

data class UploadResponse(val message: String, val records: Int)
data class ModelsResponse(val data: List<ModelInfo>)
data class ModelInfo(val id: String, val capabilities: List<String>, val isFree: Boolean)
data class ChatRequest(val model: String, val messages: List<String>, val stream: Boolean)
data class ToolExecuteRequest(val action: String, val arguments: Map<String, String>, val requiresApproval: Boolean)
data class ToolExecuteResponse(val result: String?, val error: String?, val approvalContext: ApprovalContext?)
data class ApprovalContext(val action: String, val estimatedCost: String, val affectedRecords: String, val destination: String)

interface ApiService {
    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): UploadResponse

    @GET("api/gateway/models")
    suspend fun getModels(): ModelsResponse

    @POST("api/gateway/chat/completions")
    @Streaming
    suspend fun streamChat(@Body request: ChatRequest): ResponseBody

    @POST("api/tools/execute")
    suspend fun executeTool(@Body request: ToolExecuteRequest): retrofit2.Response<ToolExecuteResponse>
}
