use {
    crate::{
        Result,
        auth::CurrentUser,
        error::{Error, ErrorBody},
        state::State as AppState,
    },
    axum::{Extension, Json, extract::State},
    serde::Serialize,
    std::sync::Arc,
    uuid::Uuid,
};

/// Someone support staff can assign incidents to.
#[derive(Serialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct Technician {
    pub id: Uuid,
    pub organization_id: Uuid,
    pub name: String,
    /// Incidents assigned to them that aren't resolved yet, a rough measure of how busy they are
    pub open_incidents: i64,
}

/// The technicians of the caller's manufacturer, who its support staff can assign incidents
/// to, least busy first.
#[utoipa::path(
    get,
    path = "/technicians",
    tag = "technicians",
    responses(
        (status = 200, body = Vec<Technician>),
        (status = 403, description = "Not a manufacturer's support staff", body = ErrorBody),
    ),
    security(("bearer" = [])),
)]
pub async fn fetch_technicians(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
) -> Result<Json<Vec<Technician>>> {
    if !user.is_support() {
        return Err(Error::Forbidden(
            "only support staff can list technicians".into(),
        ));
    }

    let technicians = sqlx::query_as::<_, Technician>(
        "SELECT u.id, u.organization_id, u.name,
                count(i.id) FILTER (WHERE i.status <> 'resolvida') AS open_incidents
           FROM users u
           LEFT JOIN incidents i ON i.assigned_to = u.id
          WHERE u.role = 'tecnico' AND u.organization_id = $1
          GROUP BY u.id
          ORDER BY open_incidents, u.name",
    )
    .bind(user.organization_id)
    .fetch_all(&state.db)
    .await?;

    Ok(Json(technicians))
}
