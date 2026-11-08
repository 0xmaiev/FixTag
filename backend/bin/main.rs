use {
    clap::{Parser, Subcommand},
    fixtag_api::{
        DEV_SEED, MIGRATOR, auth::hash_password, config::Config, openapi::ApiDoc, routes::router,
        state::State,
    },
    sqlx::postgres::PgPoolOptions,
    std::path::PathBuf,
    tracing_subscriber::EnvFilter,
    utoipa::OpenApi,
};

const DEFAULT_CONFIG_PATH: &str = "~/.fixtag/config.json";

#[derive(Parser, Debug)]
#[command(version, about)]
struct Cli {
    /// Path to the config file
    #[arg(short, long, default_value = DEFAULT_CONFIG_PATH)]
    path: PathBuf,
    #[command(subcommand)]
    command: Option<Command>,
}

#[derive(Subcommand, Debug)]
enum Command {
    /// Run the API server (the default)
    Serve,
    /// Apply migrations and insert the demo organizations, users and machines
    Seed,
    /// Print the OpenAPI spec as JSON
    Openapi,
    /// Print the argon2 hash of a password, to insert users by hand
    HashPassword { password: String },
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let args = Cli::parse();

    // commands that don't need the config or the database
    match &args.command {
        Some(Command::Openapi) => {
            println!("{}", ApiDoc::openapi().to_pretty_json()?);
            return Ok(());
        }
        Some(Command::HashPassword { password }) => {
            println!("{}", hash_password(password)?);
            return Ok(());
        }
        _ => {}
    }

    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "fixtag_api=info,tower_http=debug".into()),
        )
        .init();

    let config = Config::load(args.path)?;

    let db = PgPoolOptions::new()
        .max_connections(10)
        .connect(&config.database_url)
        .await?;
    MIGRATOR.run(&db).await?;

    if let Some(Command::Seed) = args.command {
        sqlx::raw_sql(DEV_SEED).execute(&db).await?;
        println!("seeded demo data, every demo user's password is \"fixtag123\"");
        return Ok(());
    }

    let state = State::init(db, &config);
    let app = router(state);

    let addr = format!("0.0.0.0:{}", config.port);

    tracing::info!("starting server on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app)
        .with_graceful_shutdown(async {
            let _ = tokio::signal::ctrl_c().await;
        })
        .await?;

    Ok(())
}
