package com.example.fixtag.data.model

enum class AttachmentKind(val extension: String, val contentType: String) {
    Photo("jpg", "image/jpeg"),
    Audio("m4a", "audio/mp4"),
    Video("mp4", "video/mp4");

    companion object {
        fun fromCustomString(kind: String): AttachmentKind {
            return when (kind) {
                "foto" -> {
                    AttachmentKind.Photo
                }
                "audio" -> {
                    AttachmentKind.Audio
                }
                "video" -> {
                    AttachmentKind.Video
                }
                else -> {
                    throw Exception("invalid attachment kind")
                }
            }
        }
    }
}
