package com.example.fixtag.data.model

import com.google.gson.annotations.SerializedName

enum class AttachmentKind(val extension: String, val contentType: String) {
    @SerializedName("foto")
    Photo("jpg", "image/jpeg"),
    @SerializedName("audio")
    Audio("m4a", "audio/mp4"),
    @SerializedName("video")
    Video("mp4", "video/mp4");
}
