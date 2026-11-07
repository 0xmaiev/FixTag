pub mod attachments;
pub mod devices;
pub mod health;
pub mod incidents;
pub mod login;
pub mod machines;
pub mod organizations;
pub mod refresh;
pub mod technicians;
pub mod users;

pub use attachments::fetch_attachment;
pub use devices::register_token;
pub use health::health;
pub use incidents::{
    add_attachment, create_incident, fetch_incident_by_id, fetch_incidents, update_incident,
};
pub use login::login;
pub use machines::{create_machine, fetch_machines};
pub use organizations::{fetch_organizations, register_client, rename_organization};
pub use refresh::refresh;
pub use technicians::fetch_technicians;
pub use users::{change_password, create_user, fetch_users};

use {
    crate::{
        auth::{CHANGE_PASSWORD_PATH, auth},
        openapi::ApiDoc,
        state::State as AppState,
    },
    axum::{
        Json, Router,
        extract::DefaultBodyLimit,
        middleware,
        routing::{get, post, put},
    },
    serde_derive::Deserialize,
    std::sync::Arc,
    tower_http::trace::TraceLayer,
    utoipa::OpenApi,
};

pub const DEFAULT_LIMIT: i64 = 50;
pub const MAX_LIMIT: i64 = 200;

#[derive(Deserialize, utoipa::IntoParams)]
#[into_params(parameter_in = Query)]
pub struct PaginationParams {
    /// Opaque cursor, the `next_cursor` of a previous response
    pub cursor: Option<String>,
    /// Page size, at most 200
    #[serde(default = "default_limit")]
    #[param(default = 50, minimum = 1, maximum = 200)]
    pub limit: i64,
}

fn default_limit() -> i64 {
    DEFAULT_LIMIT
}

pub fn router(state: Arc<AppState>) -> Router {
    let protected = Router::new()
        // devices
        .route("/devices", post(register_token))
        // organizations, people and machines
        .route("/organizations", get(fetch_organizations))
        .route("/organizations/{id}", put(rename_organization))
        .route("/clients", post(register_client))
        .route("/users", get(fetch_users).post(create_user))
        .route(CHANGE_PASSWORD_PATH, post(change_password))
        .route("/technicians", get(fetch_technicians))
        .route("/machines", get(fetch_machines))
        .route("/machines/{id}", put(create_machine))
        // attachments
        .route("/attachments/{id}", get(fetch_attachment))
        // incidents
        .route("/incidents", get(fetch_incidents))
        .route(
            "/incidents/{id}",
            get(fetch_incident_by_id).put(create_incident),
        )
        .route("/incidents/{id}/update", post(update_incident))
        .route(
            "/incidents/{id}/attachments/{attachment_id}",
            put(add_attachment).layer(DefaultBodyLimit::max(state.max_upload_bytes)),
        )
        .route_layer(middleware::from_fn_with_state(state.clone(), auth));

    Router::new()
        .route("/", get(health))
        .route("/openapi.json", get(|| async { Json(ApiDoc::openapi()) }))
        // auth
        .route("/login", post(login))
        .route("/refresh", post(refresh))
        .merge(protected)
        .layer(TraceLayer::new_for_http())
        .with_state(state)
}
