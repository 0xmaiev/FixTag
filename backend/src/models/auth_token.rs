use {
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize)]
pub struct AuthToken {
    pub id: Uuid,
    pub user_id: Uuid,
    pub token: String,
    pub expiry: DateTime<Utc>,
}
