pub mod attachment;
pub mod auth_token;
pub mod device;
pub mod incident;
pub mod incident_update;
pub mod machine;
pub mod organization;
pub mod user;

pub use attachment::Attachment;
pub use auth_token::AuthToken;
pub use device::Device;
pub use incident::Incident;
pub use incident_update::IncidentUpdate;
pub use machine::Machine;
pub use organization::Organization;

#[derive(
    Debug, Clone, Copy, serde::Serialize, serde::Deserialize, sqlx::Type, utoipa::ToSchema,
)]
#[sqlx(type_name = "incident_status", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum OrganizationKind {
    Cliente,
    Fabricante,
}

#[derive(
    Debug, Clone, Copy, serde::Serialize, serde::Deserialize, sqlx::Type, utoipa::ToSchema,
)]
#[sqlx(type_name = "incident_status", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum UserRole {
    Operador,
    Tecnico,
    Assistencia,
}

#[derive(
    Debug, Clone, Copy, serde::Serialize, serde::Deserialize, sqlx::Type, utoipa::ToSchema,
)]
#[sqlx(type_name = "incident_status", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum IncidentSeverity {
    Baixa,
    Media,
    Critica,
}

#[derive(
    Debug, Clone, Copy, serde::Serialize, serde::Deserialize, sqlx::Type, utoipa::ToSchema,
)]
#[sqlx(type_name = "incident_status", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum IncidentStatus {
    Recebida,
    EmAnalise,
    Resolvida,
}

#[derive(
    Debug, Clone, Copy, serde::Serialize, serde::Deserialize, sqlx::Type, utoipa::ToSchema,
)]
#[sqlx(type_name = "incident_status", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum AttachmentKind {
    Foto,
    Audio,
    Video,
}
