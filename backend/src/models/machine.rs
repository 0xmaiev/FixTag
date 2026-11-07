use {
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct Machine {
    pub id: Uuid,
    /// The client that owns it
    pub organization_id: Uuid,
    /// Who made it and services it, their staff see it and its incidents
    pub manufacturer_id: Uuid,
    /// Also what the QR code on the machine encodes
    pub serial_number: String,
    pub model: String,
    pub created_at: DateTime<Utc>,
}
