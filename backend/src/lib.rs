pub mod auth;
pub mod config;
pub mod error;
pub mod models;
pub mod routes;
pub mod state;
pub mod storage;

type Result<T> = std::result::Result<T, crate::error::Error>;
