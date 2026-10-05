use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct State {}

impl State {
    pub fn init() -> Arc<Self> {
        Arc::new(Self {})
    }
}
