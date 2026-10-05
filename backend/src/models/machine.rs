use {
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize)]
pub struct Machine {
    pub id: Uuid,
    pub organization_id: Uuid,
    pub serial_number: String,
    pub model: String,
    pub qr_code: String,
    pub created_at: DateTime<Utc>,
}
