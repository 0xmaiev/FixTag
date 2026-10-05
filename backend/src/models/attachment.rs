use {
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize)]
pub struct Attachment {
    pub id: Uuid,
    pub attachment_id: Uuid,
    pub kind: String,
    pub storage_key: String,
    pub size_bytes: u64,
}
