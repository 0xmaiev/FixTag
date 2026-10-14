package com.example.fixtag.data.model

enum class AttachmentKind {
    Photo,
    Audio,
    Video;

    companion object {
        fun fromCustomString(kind: String): AttachmentKind {
            return when (kind) {
                "Foto" -> {
                    AttachmentKind.Photo
                }
                "Audio" -> {
                    AttachmentKind.Audio
                }
                else -> {
                    AttachmentKind.Video
                }
            }
        }
    }
}
