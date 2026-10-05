use {
    crate::state::State as AppState,
    axum::{extract::State, http::StatusCode},
    std::sync::Arc,
};

pub async fn register_token(State(state): State<Arc<AppState>>) -> Result<String, StatusCode> {
    Ok(String::new())
}
