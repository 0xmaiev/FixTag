use {
    crate::{
        Result,
        auth::CurrentUser,
        error::{Error, ErrorBody},
        models::{Organization, OrganizationKind, UserRole, organization::valid_nif},
        routes::users::{CreatedMember, insert_member},
        state::State as AppState,
    },
    axum::{
        Extension, Json,
        extract::{Path, State},
        http::StatusCode,
    },
    serde::{Deserialize, Serialize},
    sqlx::PgPool,
    std::sync::Arc,
    uuid::Uuid,
};

/// Filter on `organizations o` for what the organization bound to `$1` can see: itself and
/// whoever it's linked to, on either side. A literal, for `concat!`.
macro_rules! visible_organizations {
    () => {
        "(o.id = $1
          OR o.id IN (SELECT manufacturer_id FROM client_manufacturers WHERE client_id = $1)
          OR o.id IN (SELECT client_id FROM client_manufacturers WHERE manufacturer_id = $1))"
    };
}

#[derive(Deserialize, utoipa::ToSchema)]
pub struct ClientInput {
    /// The client's NIF, what tells two manufacturers they serve the same client
    pub nif: String,
    /// Ignored when the NIF is already registered
    pub name: String,
    /// The client's first gestor, who creates the rest of its accounts. Ignored when the NIF is
    /// already registered, the client has one then
    pub manager: ManagerInput,
}

#[derive(Deserialize, utoipa::ToSchema)]
pub struct ManagerInput {
    pub name: String,
    pub email: String,
}

#[derive(Serialize, utoipa::ToSchema)]
pub struct ClientRegistration {
    pub organization: Organization,
    /// The new client's gestor and their temporary password, `null` when the client was
    /// already registered and only the link to the caller's organization was added
    pub manager: Option<CreatedMember>,
}

#[derive(Deserialize, utoipa::ToSchema)]
pub struct OrganizationInput {
    pub name: String,
}

/// The visible organizations: the caller's own, plus the manufacturers it buys from (for a
/// client) or the clients it serves (for a manufacturer). Small enough to cache whole, e.g. to
/// name a machine's owner and manufacturer, or to pick the client of a new machine.
#[utoipa::path(
    get,
    path = "/organizations",
    tag = "organizations",
    responses((status = 200, body = Vec<Organization>)),
    security(("bearer" = [])),
)]
pub async fn fetch_organizations(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
) -> Result<Json<Vec<Organization>>> {
    let organizations = sqlx::query_as::<_, Organization>(concat!(
        "SELECT o.id, o.name, o.kind, o.nif, o.created_at FROM organizations o WHERE ",
        visible_organizations!(),
        " ORDER BY o.name"
    ))
    .bind(user.organization_id)
    .fetch_all(&state.db)
    .await?;

    Ok(Json(organizations))
}

/// POST, registers a client together with its first gestor. When the NIF is already registered
/// (the client buys from another manufacturer too) it only links the caller's organization to
/// it, and nothing else about the client changes. Only a manufacturer's support staff.
/// Online only: the NIF decides which organization the client is, so the server has to answer.
#[utoipa::path(
    post,
    path = "/clients",
    tag = "organizations",
    request_body = ClientInput,
    responses(
        (status = 201, description = "New client, with its gestor's temporary password", body = ClientRegistration),
        (status = 200, description = "Already registered, now linked (`manager` is null)", body = ClientRegistration),
        (status = 400, description = "Invalid NIF, empty name or invalid manager", body = ErrorBody),
        (status = 403, description = "Not a manufacturer's support staff", body = ErrorBody),
        (status = 409, description = "The NIF belongs to a manufacturer, or the manager's email is in use", body = ErrorBody),
    ),
    security(("bearer" = [])),
)]
pub async fn register_client(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
    Json(input): Json<ClientInput>,
) -> Result<(StatusCode, Json<ClientRegistration>)> {
    if !user.is_support() {
        return Err(Error::Forbidden(
            "only a manufacturer's support staff can register clients".into(),
        ));
    }
    let nif = input.nif.trim();
    if !valid_nif(nif) {
        return Err(Error::BadRequest(
            "invalid NIF, it needs 9 digits with a valid check digit".into(),
        ));
    }
    let name = input.name.trim();
    if name.is_empty() {
        return Err(Error::BadRequest("name can't be empty".into()));
    }

    let mut tx = state.db.begin().await?;
    let inserted = sqlx::query_as::<_, Organization>(
        "INSERT INTO organizations (name, kind, nif)
         VALUES ($1, 'cliente', $2)
         ON CONFLICT (nif) DO NOTHING
         RETURNING id, name, kind, nif, created_at",
    )
    .bind(name)
    .bind(nif)
    .fetch_optional(&mut *tx)
    .await?;

    let (organization, manager) = match inserted {
        Some(organization) => {
            let manager = insert_member(
                &mut *tx,
                organization.id,
                OrganizationKind::Cliente,
                &input.manager.name,
                &input.manager.email,
                UserRole::Gestor,
            )
            .await?;
            (organization, Some(manager))
        }
        None => {
            let organization = sqlx::query_as::<_, Organization>(
                "SELECT id, name, kind, nif, created_at FROM organizations WHERE nif = $1",
            )
            .bind(nif)
            .fetch_one(&mut *tx)
            .await?;
            if organization.kind != OrganizationKind::Cliente {
                return Err(Error::Conflict("that NIF belongs to a manufacturer".into()));
            }
            (organization, None)
        }
    };

    sqlx::query(
        "INSERT INTO client_manufacturers (client_id, manufacturer_id)
         VALUES ($1, $2)
         ON CONFLICT DO NOTHING",
    )
    .bind(organization.id)
    .bind(user.organization_id)
    .execute(&mut *tx)
    .await?;
    tx.commit().await?;

    let status = if manager.is_some() {
        StatusCode::CREATED
    } else {
        StatusCode::OK
    };
    Ok((
        status,
        Json(ClientRegistration {
            organization,
            manager,
        }),
    ))
}

/// PUT, renames an organization: its own gestor, or the support staff of a manufacturer that
/// serves it (a client).
#[utoipa::path(
    put,
    path = "/organizations/{id}",
    tag = "organizations",
    params(("id" = Uuid, Path)),
    request_body = OrganizationInput,
    responses(
        (status = 200, body = Organization),
        (status = 400, description = "Empty name", body = ErrorBody),
        (status = 403, description = "Neither its gestor nor a manufacturer that serves it", body = ErrorBody),
        (status = 404, body = ErrorBody),
    ),
    security(("bearer" = [])),
)]
pub async fn rename_organization(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
    Path(id): Path<Uuid>,
    Json(input): Json<OrganizationInput>,
) -> Result<Json<Organization>> {
    let name = input.name.trim();
    if name.is_empty() {
        return Err(Error::BadRequest("name can't be empty".into()));
    }

    let organization = visible_organization(&state.db, &user, id)
        .await?
        .ok_or(Error::NotFound)?;
    let own_gestor = organization.id == user.organization_id && user.role == UserRole::Gestor;
    // visible and not the caller's own means it's linked to the caller's organization
    let serves_it = organization.kind == OrganizationKind::Cliente && user.is_support();
    if !(own_gestor || serves_it) {
        return Err(Error::Forbidden(
            "only its gestor or a manufacturer that serves it can rename it".into(),
        ));
    }

    let organization = sqlx::query_as::<_, Organization>(
        "UPDATE organizations SET name = $2 WHERE id = $1
         RETURNING id, name, kind, nif, created_at",
    )
    .bind(id)
    .bind(name)
    .fetch_one(&state.db)
    .await?;
    Ok(Json(organization))
}

async fn visible_organization(
    db: &PgPool,
    user: &CurrentUser,
    id: Uuid,
) -> Result<Option<Organization>> {
    Ok(sqlx::query_as::<_, Organization>(concat!(
        "SELECT o.id, o.name, o.kind, o.nif, o.created_at FROM organizations o WHERE ",
        visible_organizations!(),
        " AND o.id = $2"
    ))
    .bind(user.organization_id)
    .bind(id)
    .fetch_optional(db)
    .await?)
}
