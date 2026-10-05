use {
    crate::state::State as AppState,
    axum::{extract::State, http::StatusCode},
    std::sync::Arc,
};

pub async fn login(State(state): State<Arc<AppState>>) -> Result<String, StatusCode> {
    // placeholder example for other routes
    // todo access state and return db connection / health metrics
    Ok(String::new())
}
