use {
    crate::models::OrganizationKind,
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize)]
pub struct Organization {
    pub id: Uuid,
    pub name: String,
    pub kind: OrganizationKind,
    pub created_at: DateTime<Utc>,
}
