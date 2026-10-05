use {
    crate::models::IncidentStatus,
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize)]
pub struct Incident {
    pub id: Uuid,
    pub machine_id: Uuid,
    pub author_id: Uuid,
    pub description: String,
    pub severity: String, // todo: replace for enum
    pub status: IncidentStatus,
    pub latitude: f64,
    pub longitude: f64,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
}
