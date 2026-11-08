use {
    crate::{Result, error::Error},
    serde_derive::Deserialize,
    std::{fs::File, io::Read, path::PathBuf},
};

#[derive(Debug, Deserialize)]
pub struct Config {
    pub port: String,
    /// e.g. postgres://fixtag:fixtag@localhost:5432/fixtag
    pub database_url: String,
    /// HMAC secret used to sign access tokens, keep it long and random
    pub jwt_secret: String,
    /// Directory where uploaded attachments are stored
    pub storage_dir: PathBuf,
    #[serde(default = "default_access_token_ttl_secs")]
    pub access_token_ttl_secs: i64,
    #[serde(default = "default_refresh_token_ttl_days")]
    pub refresh_token_ttl_days: i64,
    #[serde(default = "default_max_upload_bytes")]
    pub max_upload_bytes: usize,
}

fn default_access_token_ttl_secs() -> i64 {
    15 * 60
}
fn default_refresh_token_ttl_days() -> i64 {
    30
}
fn default_max_upload_bytes() -> usize {
    25 * 1024 * 1024
}

impl Config {
    pub fn load(path: PathBuf) -> Result<Self> {
        let path = expand_home(path);
        let mut file = File::open(&path).map_err(|e| Error::InvalidConfig {
            message: format!("can't open {path:?}: {e}"),
        })?;
        let mut data = String::new();
        let read = file.read_to_string(&mut data)?;
        if read == 0usize {
            return Err(Error::InvalidConfig {
                message: format!("empty file: {:?}", path),
            });
        }
        let config: Self = serde_json::de::from_str(&data).map_err(Error::Json)?;
        if config.jwt_secret.len() < 32 {
            return Err(Error::InvalidConfig {
                message: "jwt_secret must be at least 32 characters".into(),
            });
        }
        Ok(config)
    }
}

/// The shell expands `~`, but the default path in the CLI never goes through a shell.
fn expand_home(path: PathBuf) -> PathBuf {
    match (path.strip_prefix("~"), std::env::var_os("HOME")) {
        (Ok(rest), Some(home)) => PathBuf::from(home).join(rest),
        _ => path,
    }
}
