pub mod auth;
pub mod config;
pub mod error;
pub mod models;
pub mod openapi;
pub mod routes;
pub mod state;
pub mod storage;

pub type Result<T> = std::result::Result<T, crate::error::Error>;

/// Embedded at compile time, applied on startup and by `#[sqlx::test]`.
pub static MIGRATOR: sqlx::migrate::Migrator = sqlx::migrate!("./migrations");

/// Demo organizations, users and machines, see `seeds/dev.sql`.
pub const DEV_SEED: &str = include_str!("../seeds/dev.sql");
