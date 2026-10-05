pub mod attachments;
pub mod devices;
pub mod health;
pub mod incidents;
pub mod login;
pub mod machines;
pub mod refresh;

pub use attachments::fetch_attachment;
pub use devices::register_token;
pub use health::health;
pub use incidents::{
    add_attachment, create_incident, fetch_incident_by_id, fetch_incidents, update_incident,
};
pub use login::login;
pub use refresh::refresh;

use serde_derive::Deserialize;

pub const DEFAULT_PAGE: usize = 1;
pub const DEFAULT_PER_PAGE: usize = 50;

#[derive(Deserialize)]
pub struct PaginationParams {
    #[serde(default = "default_page")]
    page: usize,
    #[serde(default = "default_per_page")]
    per_page: usize,
}

fn default_page() -> usize {
    DEFAULT_PAGE
}
fn default_per_page() -> usize {
    DEFAULT_PER_PAGE
}
