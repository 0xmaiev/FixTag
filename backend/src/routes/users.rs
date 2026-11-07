use {
    crate::{
        Result,
        auth::{CurrentUser, MIN_PASSWORD_LEN, hash_password, temporary_password, verify_password},
        error::{Error, ErrorBody},
        models::{OrganizationKind, User, UserRole},
        routes::login::{TokenResponse, issue_tokens},
        state::State as AppState,
        user_columns,
    },
    axum::{Extension, Json, extract::State, http::StatusCode},
    serde::{Deserialize, Serialize},
    std::sync::Arc,
    uuid::Uuid,
};

#[derive(Deserialize, utoipa::ToSchema)]
pub struct MemberInput {
    pub name: String,
    pub email: String,
    /// A gestor can create any role of their organization, support staff only `tecnico`
    pub role: UserRole,
}

#[derive(Serialize, utoipa::ToSchema)]
pub struct CreatedMember {
    pub user: User,
    /// Shown only in this response, for whoever created the account to hand over. The new
    /// member has to replace it at their first login
    pub temporary_password: String,
}

#[derive(Deserialize, utoipa::ToSchema)]
pub struct PasswordChange {
    pub current_password: String,
    /// At least 8 characters, different from the current one
    pub new_password: String,
}

/// The people in the caller's organization, e.g. for a gestor's team screen.
#[utoipa::path(
    get,
    path = "/users",
    tag = "users",
    responses((status = 200, body = Vec<User>)),
    security(("bearer" = [])),
)]
pub async fn fetch_users(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
) -> Result<Json<Vec<User>>> {
    let users = sqlx::query_as::<_, User>(concat!(
        "SELECT ",
        user_columns!(),
        " FROM users u WHERE u.organization_id = $1 ORDER BY u.name"
    ))
    .bind(user.organization_id)
    .fetch_all(&state.db)
    .await?;

    Ok(Json(users))
}

/// POST, creates an account in the caller's own organization with a temporary password. There
/// are no emails, so the response is the only place the password appears.
#[utoipa::path(
    post,
    path = "/users",
    tag = "users",
    request_body = MemberInput,
    responses(
        (status = 201, body = CreatedMember),
        (status = 400, description = "Empty name, invalid email, or a role the organization doesn't have", body = ErrorBody),
        (status = 403, description = "Not allowed to create that role", body = ErrorBody),
        (status = 409, description = "Email already in use", body = ErrorBody),
    ),
    security(("bearer" = [])),
)]
pub async fn create_user(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
    Json(input): Json<MemberInput>,
) -> Result<(StatusCode, Json<CreatedMember>)> {
    if !input.role.belongs_to(user.organization_kind) {
        return Err(Error::BadRequest(
            "your organization doesn't have that role".into(),
        ));
    }
    if !user.can_create(input.role) {
        return Err(Error::Forbidden(
            "a gestor creates accounts, support staff only technicians".into(),
        ));
    }

    let member = insert_member(
        &state.db,
        user.organization_id,
        user.organization_kind,
        &input.name,
        &input.email,
        input.role,
    )
    .await?;
    Ok((StatusCode::CREATED, Json(member)))
}

/// POST, replaces the caller's password, temporary or not. Other devices are logged out (their
/// refresh tokens stop working), and this one gets a new pair of tokens.
#[utoipa::path(
    post,
    path = "/me/password",
    tag = "users",
    request_body = PasswordChange,
    responses(
        (status = 200, body = TokenResponse),
        (status = 400, description = "Wrong current password, or the new one is too short or the same", body = ErrorBody),
    ),
    security(("bearer" = [])),
)]
pub async fn change_password(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
    Json(input): Json<PasswordChange>,
) -> Result<Json<TokenResponse>> {
    if input.new_password.chars().count() < MIN_PASSWORD_LEN {
        return Err(Error::BadRequest(format!(
            "the new password needs at least {MIN_PASSWORD_LEN} characters"
        )));
    }
    if input.new_password == input.current_password {
        return Err(Error::BadRequest(
            "the new password must be different from the current one".into(),
        ));
    }

    let mut tx = state.db.begin().await?;
    let stored = sqlx::query_as::<_, User>(concat!(
        "SELECT ",
        user_columns!(),
        " FROM users u WHERE u.id = $1 FOR UPDATE"
    ))
    .bind(user.id)
    .fetch_one(&mut *tx)
    .await?;
    if !verify_password(input.current_password, Some(stored.password_hash)).await? {
        return Err(Error::BadRequest("the current password is wrong".into()));
    }

    let password_hash = hash_in_background(input.new_password).await?;
    let updated = sqlx::query_as::<_, User>(concat!(
        "UPDATE users AS u
            SET password_hash = $2, must_change_password = false
          WHERE u.id = $1
         RETURNING ",
        user_columns!()
    ))
    .bind(user.id)
    .bind(password_hash)
    .fetch_one(&mut *tx)
    .await?;
    // deleted rather than revoked: a revoked token still refreshes during the rotation grace
    // period, and each refresh hands out a new one, so the old session would live on
    sqlx::query("DELETE FROM refresh_tokens WHERE user_id = $1")
        .bind(user.id)
        .execute(&mut *tx)
        .await?;

    let tokens = issue_tokens(&state, &mut *tx, updated).await?;
    tx.commit().await?;
    Ok(Json(tokens))
}

/// Inserts an account with a fresh temporary password, also used for a new client's first gestor.
pub(crate) async fn insert_member(
    db: impl sqlx::PgExecutor<'_>,
    organization_id: Uuid,
    organization_kind: OrganizationKind,
    name: &str,
    email: &str,
    role: UserRole,
) -> Result<CreatedMember> {
    let name = name.trim();
    let email = email.trim();
    if name.is_empty() {
        return Err(Error::BadRequest("name can't be empty".into()));
    }
    if !valid_email(email) {
        return Err(Error::BadRequest("invalid email".into()));
    }

    let temporary_password = temporary_password()?;
    let password_hash = hash_in_background(temporary_password.clone()).await?;
    let user = sqlx::query_as::<_, User>(concat!(
        "INSERT INTO users AS u (organization_id, organization_kind, name, email, password_hash,
                                 role, must_change_password)
         VALUES ($1, $2, $3, $4, $5, $6, true)
         RETURNING ",
        user_columns!()
    ))
    .bind(organization_id)
    .bind(organization_kind)
    .bind(name)
    .bind(email)
    .bind(password_hash)
    .bind(role)
    .fetch_one(db)
    .await
    .map_err(|e| match &e {
        sqlx::Error::Database(db) if db.constraint() == Some("users_email_key") => {
            Error::Conflict("that email is already in use".into())
        }
        _ => e.into(),
    })?;

    Ok(CreatedMember {
        user,
        temporary_password,
    })
}

/// argon2 is deliberately slow and would stall the runtime.
async fn hash_in_background(password: String) -> Result<String> {
    tokio::task::spawn_blocking(move || hash_password(&password))
        .await
        .map_err(|e| Error::Internal(e.to_string()))?
}

/// Just enough to catch typos, there's no email verification.
fn valid_email(email: &str) -> bool {
    !email.contains(char::is_whitespace)
        && email.split_once('@').is_some_and(|(local, domain)| {
            !local.is_empty() && !domain.contains('@') && domain.contains('.')
        })
}
