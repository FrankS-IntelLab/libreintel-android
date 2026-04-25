package com.libreintel.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI-compatible chat completion API service.
 */
interface ChatApiService {
    
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

/**
 * Request body for chat completion.
 */
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>
)

/**
 * Message structure for API.
 */
data class ChatMessageDto(
    val role: String,
    val content: String
)

/**
 * Response from chat completion API.
 */
data class ChatCompletionResponse(
    @SerializedName("choices")
    val choices: List<Choice>?,
    @SerializedName("error")
    val error: ApiError?
)

data class Choice(
    @SerializedName("message")
    val message: ResponseMessage?
)

data class ResponseMessage(
    @SerializedName("role")
    val role: String?,
    @SerializedName("content")
    val content: String?
)

data class ApiError(
    @SerializedName("message")
    val message: String?,
    @SerializedName("type")
    val type: String?
)