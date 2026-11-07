package com.example.fixtag.api

import com.example.fixtag.api.model.AttachmentDto
import com.example.fixtag.api.model.TokenResponseDto
import com.example.fixtag.api.model.IncidentDto
import com.example.fixtag.api.model.MachineDto
import com.example.fixtag.api.model.OrganizationDto
import com.example.fixtag.api.model.UserDto
import com.example.fixtag.data.model.IncidentSeverity
import com.example.fixtag.data.model.IncidentStatus
import com.example.fixtag.data.model.UserRole
import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

data class RefreshRequestDto(
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterDeviceRequestDto(
    @SerializedName("fcm_token")
    val fcmToken: String
)

data class CreateIncidentRequestDto(
    @SerializedName("machine_id")
    val machineId: String,
    val description: String,
    val severity: IncidentSeverity,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("accuracy_m")
    val accuracyM: Float?,
    @SerializedName("reported_at")
    val reportedAt: String,
)

data class IncidentPageDto(
    val items: List<IncidentDto>,
    @SerializedName("next_cursor")
    val nextCursor: String?,
    @SerializedName("has_more")
    val hasMore: Boolean
)

// Gson writes the enum's @SerializedName ("em_analise") and leaves out the fields that are null
data class UpdateIncidentRequestDto(
    val id: String,
    val message: String?,
    @SerializedName("new_status")
    val newStatus: IncidentStatus?,
    @SerializedName("assigned_to")
    val assignedTo: String?,
)

data class RenameOrganizationRequestDto(
    val name: String,
)

data class PasswordChangeRequestDto(
    @SerializedName("current_password")
    val currentPassword: String,
    @SerializedName("new_password")
    val newPassword: String,
)

data class CreateMachineRequestDto(
    @SerializedName("organization_id")
    val organizationId: String,
    @SerializedName("serial_number")
    val serialNumber: String,
    val model: String,
)

data class CreatedMemberDto(
    val user: UserDto,
    @SerializedName("temporary_password")
    val temporaryPassword: String,
)

data class CreateMemberRequestDto(
    val name: String,
    val email: String,
    val role: UserRole
)

data class ManagerInfoDto(
    val name: String,
    val email: String,
)

data class RegisterClientRequestDto(
    val nif: String,
    val name: String,
    val manager: ManagerInfoDto
)

interface ApiService {
    @POST("/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<TokenResponseDto>

    @POST("/me/password")
    suspend fun changePassword(
        @Body request: PasswordChangeRequestDto
    ): Response<TokenResponseDto>

    @POST("/refresh")
    suspend fun refresh(
        @Body request: RefreshRequestDto
    ): Response<TokenResponseDto>

    @POST("/devices")
    suspend fun devices(
        @Body request: RegisterDeviceRequestDto
    ): Response<Unit>

    @POST("/users")
    suspend fun createMember(
        @Body request: CreateMemberRequestDto
    ): Response<CreatedMemberDto>

    @POST("/clients")
    suspend fun registerClient(
        @Body request: RegisterClientRequestDto
    ): Response<UserDto>

    @GET("/users")
    suspend fun getMembers(): Response<List<UserDto>>

    @GET("/organizations")
    suspend fun getOrganizations(): Response<List<OrganizationDto>>

    @PUT("/organizations/{id}")
    suspend fun createOrganization(
        @Path("id") id: String,
        @Body request: RenameOrganizationRequestDto
    ): Response<OrganizationDto>

    @GET("/machines")
    suspend fun getMachines(
        @Query("serial_number") serialNumber: String? = null,
    ): Response<List<MachineDto>>

    @PUT("/machines/{id}")
    suspend fun createMachine(
        @Path("id") id: String,
        @Body request: CreateMachineRequestDto
    ): Response<MachineDto>

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
    ): Response<IncidentPageDto>

    @PUT("/incidents/{id}")
    suspend fun createIncident(
        @Path("id") id: String,
        @Body request: CreateIncidentRequestDto
    ): Response<IncidentDto>

    @PUT("/incidents/{id}/attachments/{attachment_id}")
    suspend fun addAttachment(
        @Path("id") id: String,
        @Path("attachment_id") attachmentId: String,
        @Body raw: RequestBody
    ): Response<AttachmentDto>

    @POST("/incidents/{id}/update")
    suspend fun addIncidentUpdate(
        @Path("id") id: String,
        @Body request: UpdateIncidentRequestDto
    ): Response<IncidentDto>

    @Streaming
    @GET("/attachments/{id}")
    suspend fun downloadAttachment(
        @Path("id") id: String
    ): Response<ResponseBody>

    @GET("/")
    suspend fun health(): Response<Unit>
}