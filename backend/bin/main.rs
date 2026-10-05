use axum::{
    Router, middleware,
    routing::{get, post, put},
};
use clap::Parser;
use clap_derive::Parser;
use fixtag_api::{
    auth::auth,
    config::Config,
    routes::{
        attachments::fetch_attachment,
        devices::register_token,
        health::health,
        incidents::{
            add_attachment, create_incident, fetch_incident_by_id, fetch_incidents, update_incident,
        },
        login::login,
        refresh::refresh,
    },
    state::State,
};
use std::path::PathBuf;

const DEFAULT_CONFIG_PATH: &'static str = "~/.fixtag/config.json";

#[derive(Parser, Debug)]
#[command(version, about)]
struct Cli {
    /// Path to the config file
    #[arg(short, long, default_value = DEFAULT_CONFIG_PATH)]
    path: PathBuf,
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let args = Cli::parse();

    let config = Config::load(args.path)?;

    let state = State::init();

    let app = Router::new()
        .route("/", get(health))
        // auth
        .route("/login", post(login))
        .route("/refresh", post(refresh))
        // devices
        .route("/devices", post(register_token))
        // attachments
        .route("/attachments/{id}", get(fetch_attachment))
        // incidents
        .route("/incidents/{id}", put(create_incident))
        .route("/incidents/{id}/update", post(update_incident))
        .route(
            "/incidents/{id}/attachments/{attachment_id}",
            post(add_attachment),
        )
        .route("/incidents/{id}", get(fetch_incident_by_id))
        .route("/incidents", get(fetch_incidents))
        .route_layer(middleware::from_fn_with_state(state.clone(), auth))
        .with_state(state);

    let addr = format!("0.0.0.0:{}", config.port);

    println!("starting server on {}", addr);

    // run our app with hyper, listening globally on port 3000
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();

    Ok(())
}
