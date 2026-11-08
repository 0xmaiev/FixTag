use {
    crate::models::AttachmentKind,
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

/// Metadata of an uploaded file, the content is served by `GET /attachments/{id}`.
#[derive(Debug, Serialize, Deserialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct Attachment {
    pub id: Uuid,
    pub incident_id: Uuid,
    pub kind: AttachmentKind,
    pub content_type: String,
    #[serde(skip)]
    pub storage_key: String,
    pub size_bytes: i64,
    pub created_at: DateTime<Utc>,
}
