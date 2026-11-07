use {
    crate::models::IncidentStatus,
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct IncidentUpdate {
    pub id: Uuid,
    pub incident_id: Uuid,
    pub author_id: Uuid,
    pub author_name: String,
    pub message: Option<String>,
    pub new_status: Option<IncidentStatus>,
    /// Technician the incident was assigned to by this entry
    pub assigned_to: Option<Uuid>,
    pub assigned_to_name: Option<String>,
    pub created_at: DateTime<Utc>,
}
