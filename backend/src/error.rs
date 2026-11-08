use {
    axum::{
        Json,
        http::StatusCode,
        response::{IntoResponse, Response},
    },
    sqlx::error::ErrorKind,
};

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("I/O failure: {0}")]
    Io(#[from] std::io::Error),
    #[error("JSON serialization/deserialization error: {0}")]
    Json(#[from] serde_json::Error),
    #[error("Invalid configuration: {message}")]
    InvalidConfig { message: String },
    #[error("Database error: {0}")]
    Database(#[from] sqlx::Error),
    #[error("Migration error: {0}")]
    Migrate(#[from] sqlx::migrate::MigrateError),
    #[error("Token error: {0}")]
    Token(#[from] jsonwebtoken::errors::Error),
    #[error("Internal error: {0}")]
    Internal(String),

    // client errors, the message is returned to the caller
    #[error("{0}")]
    BadRequest(String),
    #[error("invalid or expired credentials")]
    Unauthorized,
    #[error("{0}")]
    Forbidden(String),
    #[error("not found")]
    NotFound,
    #[error("{0}")]
    Conflict(String),
    #[error("{0}")]
    UnsupportedMediaType(String),
}

impl Error {
    fn status(&self) -> StatusCode {
        match self {
            Error::BadRequest(_) => StatusCode::BAD_REQUEST,
            Error::Unauthorized => StatusCode::UNAUTHORIZED,
            Error::Forbidden(_) => StatusCode::FORBIDDEN,
            Error::NotFound => StatusCode::NOT_FOUND,
            Error::Conflict(_) => StatusCode::CONFLICT,
            Error::UnsupportedMediaType(_) => StatusCode::UNSUPPORTED_MEDIA_TYPE,
            Error::Database(sqlx::Error::RowNotFound) => StatusCode::NOT_FOUND,
            Error::Database(sqlx::Error::Database(e)) => match e.kind() {
                ErrorKind::UniqueViolation => StatusCode::CONFLICT,
                ErrorKind::ForeignKeyViolation
                | ErrorKind::NotNullViolation
                | ErrorKind::CheckViolation => StatusCode::BAD_REQUEST,
                _ => StatusCode::INTERNAL_SERVER_ERROR,
            },
            _ => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }
}

/// JSON body of every error response.
#[derive(serde::Serialize, utoipa::ToSchema)]
pub struct ErrorBody {
    pub error: String,
}

impl IntoResponse for Error {
    fn into_response(self) -> Response {
        let status = self.status();
        let error = match &self {
            _ if status.is_server_error() => {
                tracing::error!("{self}");
                "internal server error".to_string()
            }
            // constraint violations are the client's fault, name the constraint so the
            // app developer can tell which rule was broken
            Error::Database(sqlx::Error::Database(e)) => match e.constraint() {
                Some(constraint) => format!("constraint violated: {constraint}"),
                None => e.message().to_string(),
            },
            _ => self.to_string(),
        };
        (status, Json(ErrorBody { error })).into_response()
    }
}
