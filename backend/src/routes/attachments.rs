use {
    crate::{
        Result,
        auth::CurrentUser,
        error::{Error, ErrorBody},
        incident_scope,
        openapi::Binary,
        state::State as AppState,
        storage::Storage,
    },
    axum::{
        Extension,
        extract::{Path, State},
        http::header,
        response::{IntoResponse, Response},
    },
    std::sync::Arc,
    uuid::Uuid,
};

/// GET by id, returns the raw file with the Content-Type it was uploaded with
#[utoipa::path(
    get,
    path = "/attachments/{id}",
    tag = "attachments",
    params(("id" = Uuid, Path)),
    responses(
        (status = 200, description = "The file, with the Content-Type it was uploaded with", content(
            (inline(Binary) = "image/*"),
            (inline(Binary) = "audio/*"),
            (inline(Binary) = "video/*"),
        )),
        (status = 404, body = ErrorBody),
    ),
    security(("bearer" = [])),
)]
pub async fn fetch_attachment(
    State(state): State<Arc<AppState>>,
    Extension(user): Extension<CurrentUser>,
    Path(id): Path<Uuid>,
) -> Result<Response> {
    let scope = user.scope();
    let (content_type, storage_key): (String, String) = sqlx::query_as(concat!(
        "SELECT a.content_type, a.storage_key
           FROM attachments a
           JOIN incidents i ON i.id = a.incident_id
          WHERE a.id = $4 AND ",
        incident_scope!()
    ))
    .bind(scope.client())
    .bind(scope.manufacturer())
    .bind(scope.technician())
    .bind(id)
    .fetch_optional(&state.db)
    .await?
    .ok_or(Error::NotFound)?;

    let data = state.storage.get(&storage_key).await?;
    Ok((
        [
            (header::CONTENT_TYPE, content_type),
            // attachments never change once uploaded, let the app's image loader cache them
            (
                header::CACHE_CONTROL,
                "private, max-age=31536000, immutable".to_string(),
            ),
        ],
        data,
    )
        .into_response())
}
