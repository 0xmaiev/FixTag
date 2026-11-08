use {
    crate::{Result, error::Error},
    axum::body::Bytes,
    std::{future::Future, io::ErrorKind, path::PathBuf},
};

/// Blob storage for attachments, keys are relative paths like `incidents/{id}/{attachment_id}`.
pub trait Storage: Send + Sync {
    fn put(&self, key: &str, data: Bytes) -> impl Future<Output = Result<()>> + Send;
    fn get(&self, key: &str) -> impl Future<Output = Result<Bytes>> + Send;
}

/// Stores attachments on the local filesystem under `root`.
#[derive(Debug, Clone)]
pub struct FsStorage {
    root: PathBuf,
}

impl FsStorage {
    pub fn new(root: PathBuf) -> Self {
        Self { root }
    }
}

impl Storage for FsStorage {
    async fn put(&self, key: &str, data: Bytes) -> Result<()> {
        let path = self.root.join(key);
        if let Some(parent) = path.parent() {
            tokio::fs::create_dir_all(parent).await?;
        }
        // write then rename, so a crash mid-upload never leaves a truncated file behind
        let tmp = path.with_extension(format!("tmp-{}", uuid::Uuid::new_v4()));
        tokio::fs::write(&tmp, &data).await?;
        tokio::fs::rename(&tmp, &path).await?;
        Ok(())
    }

    async fn get(&self, key: &str) -> Result<Bytes> {
        match tokio::fs::read(self.root.join(key)).await {
            Ok(data) => Ok(data.into()),
            Err(e) if e.kind() == ErrorKind::NotFound => {
                Err(Error::Internal(format!("attachment file missing: {key}")))
            }
            Err(e) => Err(e.into()),
        }
    }
}
