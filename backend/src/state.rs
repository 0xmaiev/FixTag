use {
    crate::{auth::Tokens, config::Config, storage::FsStorage},
    chrono::Duration,
    sqlx::PgPool,
    std::sync::Arc,
};

pub struct State {
    pub db: PgPool,
    pub tokens: Tokens,
    pub storage: FsStorage,
    pub max_upload_bytes: usize,
}

impl State {
    pub fn init(db: PgPool, config: &Config) -> Arc<Self> {
        Arc::new(Self {
            db,
            tokens: Tokens::new(
                &config.jwt_secret,
                Duration::seconds(config.access_token_ttl_secs),
                Duration::days(config.refresh_token_ttl_days),
            ),
            storage: FsStorage::new(config.storage_dir.clone()),
            max_upload_bytes: config.max_upload_bytes,
        })
    }
}
