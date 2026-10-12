package com.example.fixtag.api

import com.example.fixtag.api.model.AttachmentDto
import com.example.fixtag.api.model.AuthResponse
import com.example.fixtag.api.model.IncidentDto
import com.example.fixtag.api.model.MachineDto
import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterDeviceRequest(
    @SerializedName("fcm_token")
    val fcmToken: String
)

data class CreateIncidentRequest(
    @SerializedName("machine_id")
    val machineId: String,
    val description: String,
    val severity: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("accuracy_m")
    val accuracyM: Int?,
    @SerializedName("reported_at")
    val reportedAt: String,
)

data class IncidentPage(
    val items: List<IncidentDto>,
    @SerializedName("next_cursor")
    val nextCursor: String?,
    @SerializedName("has_more")
    val hasMore: Boolean
)

data class UpdateIncidentRequest(
    val id: String,
    val message: String,
    @SerializedName("new_status")
    val newStatus: String?
)

interface ApiService {
    @POST("/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): Response<AuthResponse>

    @POST("/devices")
    suspend fun devices(
        @Body request: RegisterDeviceRequest
    ): Response<Unit>

    @GET("/machines")
    suspend fun getMachines(
        @Query("serial_number") serialNumber: String? = null,
    ): Response<List<MachineDto>>

    @GET("/incidents/{id}")
    suspend fun getIncident(
        @Path("id") id: String,
    ): Response<IncidentDto>

    @GET("/incidents")
    suspend fun getIncidents(
        @Query("limit") limit: Int? = 50,
        @Query("cursor") cursor: String?,
        @Query("status") status: String?,
        @Query("severity") severity: String?,
        @Query("machine_id") machineId: String?
    ): Response<IncidentPage>

    @PUT("/incidents/{id}")
    suspend fun createIncident(
        @Path("id") id: String,
        @Body incident: CreateIncidentRequest
    ): Response<IncidentDto>

    @PUT("/incidents/{id}/attachments/{attachment_id}")
    suspend fun addAttachment(
        @Path("id") id: String,
        @Path("attachment_id") attachmentId: String,
        @Body raw: RequestBody
    ): Response<AttachmentDto>

    @PUT("/incidents/{id}/update")
    suspend fun addIncidentUpdate(
        @Path("id") id: String,
        @Body request: UpdateIncidentRequest
    ): Response<IncidentDto>

    @GET("/")
    suspend fun health(): Response<Unit>
}