use {
    crate::models::OrganizationKind,
    chrono::{DateTime, Utc},
    serde::{Deserialize, Serialize},
    uuid::Uuid,
};

#[derive(Debug, Serialize, Deserialize, sqlx::FromRow, utoipa::ToSchema)]
pub struct Organization {
    pub id: Uuid,
    pub name: String,
    pub kind: OrganizationKind,
    /// Portuguese tax number (NIF/NIPC), 9 digits, unique across the platform
    pub nif: String,
    pub created_at: DateTime<Utc>,
}

/// Whether `nif` is a valid Portuguese tax number: 9 digits, the last one a mod 11 check digit.
pub fn valid_nif(nif: &str) -> bool {
    let digits: Vec<u32> = nif.chars().filter_map(|c| c.to_digit(10)).collect();
    if nif.len() != 9 || digits.len() != 9 {
        return false;
    }
    let sum: u32 = digits[..8]
        .iter()
        .zip((2..=9).rev())
        .map(|(digit, weight)| digit * weight)
        .sum();
    let check = match sum % 11 {
        0 | 1 => 0,
        rest => 11 - rest,
    };
    digits[8] == check
}

#[cfg(test)]
mod tests {
    use super::valid_nif;

    #[test]
    fn nif_check_digit() {
        assert!(valid_nif("500000018"));
        assert!(valid_nif("500000050")); // remainder 0, check digit 0
        assert!(!valid_nif("500000019"));
        assert!(!valid_nif("50000001"));
        assert!(!valid_nif("5000000180"));
        assert!(!valid_nif("50000001a"));
        assert!(!valid_nif("+50000018"));
    }
}
