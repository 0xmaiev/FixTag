use {
    crate::{Result, error::Error},
    serde_derive::Deserialize,
    std::{fs::File, io::Read, path::PathBuf},
};

#[derive(Debug, Deserialize)]
pub struct Config {
    pub port: String,
}

impl Config {
    pub fn load(path: PathBuf) -> Result<Self> {
        let mut file = File::open(&path)?;
        let mut data = String::new();
        let read = file.read_to_string(&mut data)?;
        if read == 0usize {
            return Err(Error::InvalidConfig {
                message: format!("empty file: {:?}", path),
            });
        }
        serde_json::de::from_str(&data).map_err(Error::Json)
    }
}
