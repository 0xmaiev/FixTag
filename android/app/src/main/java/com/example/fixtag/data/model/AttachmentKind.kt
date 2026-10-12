package com.example.fixtag.data.model

enum class AttachmentKind {
    Photo,
    Audio,
    Video
}

fun attachmentKindFromString(role: String): AttachmentKind {
    return when (role) {
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
