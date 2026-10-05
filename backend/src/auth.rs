use axum::{
    extract::{Extension, Request},
    http::StatusCode,
    middleware::{self, Next},
    response::{IntoResponse, Response},
};

#[derive(Clone)]
struct CurrentUser {/* ... */}

// Placeholder fn for now
async fn authorize_current_user(auth_token: &str) -> Option<CurrentUser> {
    // ...
    None
}

pub async fn auth(mut req: Request, next: Next) -> Result<Response, StatusCode> {
    let auth_header = req
        .headers()
        .get(http::header::AUTHORIZATION)
        .and_then(|header| header.to_str().ok());

    let auth_header = if let Some(auth_header) = auth_header {
        auth_header
    } else {
        return Err(StatusCode::UNAUTHORIZED);
    };

    if let Some(current_user) = authorize_current_user(auth_header).await {
        // insert the current user into a request extension so the handler can
        // extract it
        req.extensions_mut().insert(current_user);
        Ok(next.run(req).await)
    } else {
        Err(StatusCode::UNAUTHORIZED)
    }
}
