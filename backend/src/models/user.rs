use {
    crate::models::{OrganizationKind, UserRole},
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct User {
    pub id: Uuid,
    pub organization_id: Uuid,
    /// Which side the user works for, the client or the manufacturer
    pub organization_kind: OrganizationKind,
    pub name: String,
    pub email: String,
    #[serde(skip)]
    pub password_hash: String,
    pub role: UserRole,
    /// Still on the temporary password they were created with. Until they change it at
    /// `POST /me/password`, every other endpoint answers 403
    pub must_change_password: bool,
    pub created_at: DateTime<Utc>,
}

/// Columns to select for a [`User`] from `users u`, as a literal for `concat!`.
#[macro_export]
macro_rules! user_columns {
    () => {
        "u.id, u.organization_id, u.organization_kind, u.name, u.email, u.password_hash, u.role, \
         u.must_change_password, u.created_at"
    };
}
