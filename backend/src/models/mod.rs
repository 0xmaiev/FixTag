pub mod attachment;
pub mod device;
pub mod incident;
pub mod incident_update;
pub mod machine;
pub mod organization;
pub mod user;

pub use attachment::Attachment;
pub use device::Device;
pub use incident::Incident;
pub use incident_update::IncidentUpdate;
pub use machine::Machine;
pub use organization::Organization;
pub use user::User;

#[derive(
    Debug,
    Clone,
    Copy,
    PartialEq,
    Eq,
    serde::Serialize,
    serde::Deserialize,
    sqlx::Type,
    utoipa::ToSchema,
)]
#[sqlx(type_name = "org_kind", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum OrganizationKind {
    Cliente,
    Fabricante,
}

#[derive(
    Debug,
    Clone,
    Copy,
    PartialEq,
    Eq,
    serde::Serialize,
    serde::Deserialize,
    sqlx::Type,
    utoipa::ToSchema,
)]
#[sqlx(type_name = "user_role", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum UserRole {
    /// A client's staff, reports incidents
    Operador,
    /// A manufacturer's field technician, works on the incidents assigned to them
    Tecnico,
    /// A manufacturer's support staff, triages incidents and assigns technicians
    Assistencia,
    /// Manages their organization's people, and can do everything the rest of its staff can
    Gestor,
}

impl UserRole {
    /// Roles each kind of organization has, the database enforces the same rule.
    pub fn belongs_to(self, kind: OrganizationKind) -> bool {
        match kind {
            OrganizationKind::Cliente => matches!(self, Self::Operador | Self::Gestor),
            OrganizationKind::Fabricante => {
                matches!(self, Self::Assistencia | Self::Tecnico | Self::Gestor)
            }
        }
    }
}

#[derive(
    Debug,
    Clone,
    Copy,
    PartialEq,
    Eq,
    serde::Serialize,
    serde::Deserialize,
    sqlx::Type,
    utoipa::ToSchema,
)]
#[sqlx(type_name = "incident_severity", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum IncidentSeverity {
    Baixa,
    Media,
    Critica,
}

#[derive(
    Debug,
    Clone,
    Copy,
    PartialEq,
    Eq,
    serde::Serialize,
    serde::Deserialize,
    sqlx::Type,
    utoipa::ToSchema,
)]
#[sqlx(type_name = "incident_status", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum IncidentStatus {
    Recebida,
    EmAnalise,
    Resolvida,
}

#[derive(
    Debug,
    Clone,
    Copy,
    PartialEq,
    Eq,
    serde::Serialize,
    serde::Deserialize,
    sqlx::Type,
    utoipa::ToSchema,
)]
#[sqlx(type_name = "attachment_kind", rename_all = "snake_case")]
#[serde(rename_all = "snake_case")]
pub enum AttachmentKind {
    Foto,
    Audio,
    Video,
}

impl AttachmentKind {
    /// Derives the kind from the upload's Content-Type, e.g. `image/jpeg` is a photo.
    pub fn from_content_type(content_type: &str) -> Option<Self> {
        match content_type.split('/').next()? {
            "image" => Some(Self::Foto),
            "audio" => Some(Self::Audio),
            "video" => Some(Self::Video),
            _ => None,
        }
    }
}
