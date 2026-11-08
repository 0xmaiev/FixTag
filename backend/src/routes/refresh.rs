use {
    crate::{
        Result,
        auth::hash_refresh_token,
        error::{Error, ErrorBody},
        models::User,
        routes::login::{TokenResponse, issue_tokens},
        state::State as AppState,
        user_columns,
    },
    axum::{Json, extract::State},
    serde::Deserialize,
    std::sync::Arc,
    uuid::Uuid,
};

/// How long a rotated refresh token keeps working. If the response to a refresh is lost on a
/// flaky connection the app retries with the old token, and that shouldn't log the user out.
const ROTATION_GRACE_SECS: f64 = 60.0;

#[derive(Deserialize, utoipa::ToSchema)]
pub struct RefreshRequest {
    pub refresh_token: String,
}

/// Exchanges a refresh token for a new access token and a new refresh token. The old refresh
/// token is revoked (after a short grace period), so always store the new one.
#[utoipa::path(
    post,
    path = "/refresh",
    tag = "auth",
    request_body = RefreshRequest,
    responses(
        (status = 200, body = TokenResponse),
        (status = 401, description = "Unknown, expired or revoked refresh token, log in again", body = ErrorBody),
    ),
)]
pub async fn refresh(
    State(state): State<Arc<AppState>>,
    Json(req): Json<RefreshRequest>,
) -> Result<Json<TokenResponse>> {
    let mut tx = state.db.begin().await?;

    let user_id: Uuid = sqlx::query_scalar(
        "UPDATE refresh_tokens
            SET revoked_at = COALESCE(revoked_at, now())
          WHERE token_hash = $1
            AND expires_at > now()
            AND (revoked_at IS NULL OR revoked_at > now() - make_interval(secs => $2))
         RETURNING user_id",
    )
    .bind(hash_refresh_token(&req.refresh_token))
    .bind(ROTATION_GRACE_SECS)
    .fetch_optional(&mut *tx)
    .await?
    .ok_or(Error::Unauthorized)?;

    // re-read the user so role or organization changes apply from the next access token
    let user = sqlx::query_as::<_, User>(concat!(
        "SELECT ",
        user_columns!(),
        " FROM users u WHERE u.id = $1"
    ))
    .bind(user_id)
    .fetch_one(&mut *tx)
    .await?;

    let tokens = issue_tokens(&state, &mut *tx, user).await?;
    tx.commit().await?;
    Ok(Json(tokens))
}
