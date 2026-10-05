use {
    crate::state::State as AppState,
    axum::{
        extract::{Path, State},
        http::StatusCode,
    },
    std::sync::Arc,
};

/// GET by id
pub async fn fetch_attachment(
    State(state): State<Arc<AppState>>,
    Path(id): Path<uuid::Uuid>,
) -> Result<String, StatusCode> {
    Ok(String::new())
}
