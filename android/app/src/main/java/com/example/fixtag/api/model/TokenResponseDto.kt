package com.example.fixtag.api.model

import com.google.gson.annotations.SerializedName

data class TokenResponseDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("user")
    val userDto: UserDto
)
