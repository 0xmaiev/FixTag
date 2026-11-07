use {
    crate::state::State as AppState,
    axum::{extract::State, http::StatusCode},
    std::sync::Arc,
};

#[utoipa::path(
    get,
    path = "/",
    tag = "health",
    responses(
        (status = 200, description = "API and database are up", body = String),
        (status = 503, description = "Database unreachable"),
    ),
)]
pub async fn health(State(state): State<Arc<AppState>>) -> Result<&'static str, StatusCode> {
    match sqlx::query("SELECT 1").execute(&state.db).await {
        Ok(_) => Ok("ok"),
        Err(e) => {
            tracing::error!("health check failed: {e}");
            Err(StatusCode::SERVICE_UNAVAILABLE)
        }
    }
}
