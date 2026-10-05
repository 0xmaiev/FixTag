use axum::{
    Router, middleware,
    routing::{get, post, put},
};
use clap::Parser;
use clap_derive::Parser;
use fixtag_api::{
    auth::auth,
    config::Config,
    routes::{health::health, login::login, refresh::refresh},
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
        .route("/login", post(login))
        .route("/refresh", post(refresh))
        .route_layer(middleware::from_fn_with_state(state.clone(), auth))
        .with_state(state);

    let addr = format!("0.0.0.0:{}", config.port);

    println!("starting server on {}", addr);

    // run our app with hyper, listening globally on port 3000
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();

    Ok(())
}
