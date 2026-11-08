use {
    crate::{
        Result,
        auth::CurrentUser,
        error::{Error, ErrorBody},
        state::State as AppState,
    },
    axum::{Extension, Json, extract::State, http::StatusCode},
    serde::Deserialize,
    std::sync::Arc,
};

#[derive(Deserialize, utoipa::ToSchema)]
pub struct RegisterDevice {
    pub fcm_token: String,
}

/// Registers the device's FCM token for push notifications. Call it after every login and
/// whenever Firebase rotates the token, a token moves to whoever registered it last.
#[utoipa::path(
    post,
    path = "/devices",
    tag = "devices",
    request_body = RegisterDevice,
    responses(
        (status = 204, description = "Registered"),
        (status = 400, body = ErrorBody),
    ),
    security(("bearer" = [])),
)]
pub async fn register_token(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
    Json(req): Json<RegisterDevice>,
) -> Result<StatusCode> {
    let fcm_token = req.fcm_token.trim();
    if fcm_token.is_empty() {
        return Err(Error::BadRequest("fcm_token is empty".into()));
    }

    sqlx::query(
        "INSERT INTO devices (user_id, fcm_token)
         VALUES ($1, $2)
         ON CONFLICT (fcm_token)
         DO UPDATE SET user_id = EXCLUDED.user_id, last_seen_at = now()",
    )
    .bind(user.id)
    .bind(fcm_token)
    .execute(&state.db)
    .await?;

    Ok(StatusCode::NO_CONTENT)
}
