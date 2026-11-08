use {
    crate::{
        Result,
        auth::{CurrentUser, new_refresh_token, verify_password},
        error::{Error, ErrorBody},
        models::User,
        state::State as AppState,
        user_columns,
    },
    axum::{Json, extract::State},
    chrono::Utc,
    serde::{Deserialize, Serialize},
    std::sync::Arc,
};

#[derive(Deserialize, utoipa::ToSchema)]
pub struct LoginRequest {
    pub email: String,
    pub password: String,
}

#[derive(Serialize, utoipa::ToSchema)]
pub struct TokenResponse {
    /// Send as `Authorization: Bearer <access_token>`
    pub access_token: String,
    /// Exchange at `POST /refresh` when the access token expires, single use
    pub refresh_token: String,
    pub token_type: String,
    /// Seconds until the access token expires
    pub expires_in: i64,
    pub user: User,
}

/// When `user.must_change_password` is set the account is still on its temporary password:
/// every endpoint except `POST /me/password` answers 403 until it's changed.
#[utoipa::path(
    post,
    path = "/login",
    tag = "auth",
    request_body = LoginRequest,
    responses(
        (status = 200, body = TokenResponse),
        (status = 401, description = "Wrong email or password", body = ErrorBody),
    ),
)]
pub async fn login(
    State(state): State<Arc<AppState>>,
    Json(req): Json<LoginRequest>,
) -> Result<Json<TokenResponse>> {
    let user = sqlx::query_as::<_, User>(concat!(
        "SELECT ",
        user_columns!(),
        " FROM users u WHERE lower(u.email) = lower($1)"
    ))
    .bind(req.email.trim())
    .fetch_optional(&state.db)
    .await?;

    let hash = user.as_ref().map(|user| user.password_hash.clone());
    let user = match user {
        Some(user) if verify_password(req.password, hash).await? => user,
        _ => return Err(Error::Unauthorized),
    };

    // drop dead tokens on every login so the table doesn't grow forever
    sqlx::query(
        "DELETE FROM refresh_tokens
          WHERE user_id = $1
            AND (expires_at < now() OR revoked_at < now() - interval '1 day')",
    )
    .bind(user.id)
    .execute(&state.db)
    .await?;

    Ok(Json(issue_tokens(&state, &state.db, user).await?))
}

/// Stores a new refresh token for `user` and signs a matching access token.
pub(crate) async fn issue_tokens(
    state: &AppState,
    db: impl sqlx::PgExecutor<'_>,
    user: User,
) -> Result<TokenResponse> {
    let (refresh_token, token_hash) = new_refresh_token()?;
    sqlx::query(
        "INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
         VALUES ($1, $2, $3)",
    )
    .bind(user.id)
    .bind(token_hash)
    .bind(Utc::now() + state.tokens.refresh_ttl)
    .execute(db)
    .await?;

    let access_token = state.tokens.issue_access_token(&CurrentUser {
        id: user.id,
        organization_id: user.organization_id,
        organization_kind: user.organization_kind,
        role: user.role,
        must_change_password: user.must_change_password,
    })?;

    Ok(TokenResponse {
        access_token,
        refresh_token,
        token_type: "Bearer".into(),
        expires_in: state.tokens.access_ttl.num_seconds(),
        user,
    })
}
