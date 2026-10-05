use {
    crate::{routes::PaginationParams, state::State as AppState},
    axum::{
        extract::{Path, Query, State},
        http::StatusCode,
    },
    serde::Deserialize,
    std::sync::Arc,
};

/// GET by id
pub async fn fetch_incident_by_id(
    State(state): State<Arc<AppState>>,
    Path(id): Path<uuid::Uuid>,
) -> Result<String, StatusCode> {
    Ok(String::new())
}

#[derive(Deserialize)]
pub struct IndicentQueryParams {
    #[serde(flatten)]
    pub pagination: PaginationParams,
    pub status: String,
    pub severity: String,
    pub updated_since: u64,
}

/// GET with params for filtering and pagination, one endpoint can probably serve all
pub async fn fetch_incidents(
    State(state): State<Arc<AppState>>,
    Query(params): Query<IndicentQueryParams>,
) -> Result<String, StatusCode> {
    let page = params.pagination.page;
    let per_page = params.pagination.per_page;

    Ok(String::new())
}

/// PUT
pub async fn create_incident(State(state): State<Arc<AppState>>) -> Result<String, StatusCode> {
    Ok(String::new())
}

/// PUT
pub async fn add_attachment(State(state): State<Arc<AppState>>) -> Result<String, StatusCode> {
    Ok(String::new())
}

/// POST
pub async fn update_incident(
    State(state): State<Arc<AppState>>,
    Path((id, attachment_id)): Path<(uuid::Uuid, uuid::Uuid)>,
) -> Result<String, StatusCode> {
    Ok(String::new())
}
