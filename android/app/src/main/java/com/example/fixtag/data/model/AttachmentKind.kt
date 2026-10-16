package com.example.fixtag.data.model

enum class AttachmentKind {
    Photo,
    Audio,
    Video;

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
