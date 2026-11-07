use {
    crate::{
        Result,
        error::Error,
        models::{OrganizationKind, UserRole},
        state::State as AppState,
    },
    argon2::{
        Argon2,
        password_hash::{PasswordHasher, PasswordVerifier, phc::PasswordHash},
    },
    axum::{
        extract::{Request, State},
        middleware::Next,
        response::Response,
    },
    base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD},
    chrono::{Duration, Utc},
    jsonwebtoken::{DecodingKey, EncodingKey, Header, Validation},
    serde::{Deserialize, Serialize},
    sha2::{Digest, Sha256},
    std::sync::{Arc, LazyLock},
    uuid::Uuid,
};

/// The authenticated caller, inserted as a request extension by the [`auth`] middleware.
#[derive(Debug, Clone)]
pub struct CurrentUser {
    pub id: Uuid,
    pub organization_id: Uuid,
    pub organization_kind: OrganizationKind,
    pub role: UserRole,
    pub must_change_password: bool,
}

impl CurrentUser {
    pub fn is_manufacturer(&self) -> bool {
        matches!(self.organization_kind, OrganizationKind::Fabricante)
    }

    /// A manufacturer's support staff and its gestor triage incidents: they change the status,
    /// assign technicians, and register clients and machines.
    pub fn is_support(&self) -> bool {
        self.is_manufacturer() && matches!(self.role, UserRole::Assistencia | UserRole::Gestor)
    }

    pub fn scope(&self) -> Scope {
        match (self.organization_kind, self.role) {
            (OrganizationKind::Cliente, _) => Scope::Client(self.organization_id),
            (OrganizationKind::Fabricante, UserRole::Tecnico) => Scope::Technician {
                user_id: self.id,
                manufacturer_id: self.organization_id,
            },
            (OrganizationKind::Fabricante, _) => Scope::Manufacturer(self.organization_id),
        }
    }

    /// Accounts are always created in the creator's own organization: a gestor creates any of
    /// its roles, support staff only technicians.
    pub fn can_create(&self, role: UserRole) -> bool {
        match self.role {
            UserRole::Gestor => role.belongs_to(self.organization_kind),
            UserRole::Assistencia => role == UserRole::Tecnico,
            UserRole::Operador | UserRole::Tecnico => false,
        }
    }
}

/// What a user can see. Queries filter machines with [`machine_scope!`](crate::machine_scope)
/// and incidents with [`incident_scope!`](crate::incident_scope), binding the values of
/// [`Scope::client`], [`Scope::manufacturer`] and [`Scope::technician`] as `$1` to `$3`.
#[derive(Debug, Clone, Copy)]
pub enum Scope {
    /// A client's staff: the client's machines, whoever made them, and their incidents.
    Client(Uuid),
    /// A manufacturer's support staff and gestor: the machines it made, at every client.
    Manufacturer(Uuid),
    /// A manufacturer's technician: its machines, but only the incidents they reported or have
    /// been assigned, including past assignments. Incremental sync can't tell a phone to forget
    /// an incident, so a reassignment mustn't hide it.
    Technician {
        user_id: Uuid,
        manufacturer_id: Uuid,
    },
}

impl Scope {
    /// Only what this client owns.
    pub fn client(self) -> Option<Uuid> {
        match self {
            Self::Client(id) => Some(id),
            Self::Manufacturer(_) | Self::Technician { .. } => None,
        }
    }

    /// Only what this manufacturer made.
    pub fn manufacturer(self) -> Option<Uuid> {
        match self {
            Self::Manufacturer(id)
            | Self::Technician {
                manufacturer_id: id,
                ..
            } => Some(id),
            Self::Client(_) => None,
        }
    }

    /// Only the incidents this technician reported or has been assigned.
    pub fn technician(self) -> Option<Uuid> {
        match self {
            Self::Technician { user_id, .. } => Some(user_id),
            Self::Client(_) | Self::Manufacturer(_) => None,
        }
    }
}

/// [`Scope`] filter on `machines m`, with `$1` and `$2` bound to [`Scope::client`] and
/// [`Scope::manufacturer`]. A literal, for `concat!`.
#[macro_export]
macro_rules! machine_scope {
    () => {
        "($1::uuid IS NULL OR m.organization_id = $1)
            AND ($2::uuid IS NULL OR m.manufacturer_id = $2)"
    };
}

/// [`Scope`] filter on `incidents i`, with `$1` to `$3` bound to [`Scope::client`],
/// [`Scope::manufacturer`] and [`Scope::technician`]. A literal, for `concat!`. Every assignment
/// is an `incident_updates` row, so the `EXISTS` also covers the current one.
#[macro_export]
macro_rules! incident_scope {
    () => {
        "($1::uuid IS NULL OR i.organization_id = $1)
            AND ($2::uuid IS NULL OR i.handled_by = $2)
            AND ($3::uuid IS NULL OR i.author_id = $3 OR EXISTS (
                    SELECT 1 FROM incident_updates iu
                     WHERE iu.incident_id = i.id AND iu.assigned_to = $3))"
    };
}

#[derive(Debug, Serialize, Deserialize)]
struct Claims {
    sub: Uuid,
    org: Uuid,
    org_kind: OrganizationKind,
    role: UserRole,
    #[serde(default)]
    must_change_password: bool,
    iat: i64,
    exp: i64,
}

/// Signing keys and lifetimes for access and refresh tokens.
pub struct Tokens {
    encoding: EncodingKey,
    decoding: DecodingKey,
    pub access_ttl: Duration,
    pub refresh_ttl: Duration,
}

impl Tokens {
    pub fn new(secret: &str, access_ttl: Duration, refresh_ttl: Duration) -> Self {
        Self {
            encoding: EncodingKey::from_secret(secret.as_bytes()),
            decoding: DecodingKey::from_secret(secret.as_bytes()),
            access_ttl,
            refresh_ttl,
        }
    }

    pub fn issue_access_token(&self, user: &CurrentUser) -> Result<String> {
        let now = Utc::now();
        let claims = Claims {
            sub: user.id,
            org: user.organization_id,
            org_kind: user.organization_kind,
            role: user.role,
            must_change_password: user.must_change_password,
            iat: now.timestamp(),
            exp: (now + self.access_ttl).timestamp(),
        };
        Ok(jsonwebtoken::encode(
            &Header::default(),
            &claims,
            &self.encoding,
        )?)
    }

    fn verify_access_token(&self, token: &str) -> Option<CurrentUser> {
        let validation = Validation {
            leeway: 0,
            ..Default::default()
        };
        let claims = jsonwebtoken::decode::<Claims>(token, &self.decoding, &validation)
            .ok()?
            .claims;
        Some(CurrentUser {
            id: claims.sub,
            organization_id: claims.org,
            organization_kind: claims.org_kind,
            role: claims.role,
            must_change_password: claims.must_change_password,
        })
    }
}

/// The one endpoint a user still on a temporary password can call.
pub const CHANGE_PASSWORD_PATH: &str = "/me/password";

/// A new opaque refresh token and the hash that gets stored in the database.
pub fn new_refresh_token() -> Result<(String, Vec<u8>)> {
    let mut bytes = [0u8; 32];
    getrandom::fill(&mut bytes).map_err(|e| Error::Internal(e.to_string()))?;
    let token = URL_SAFE_NO_PAD.encode(bytes);
    let hash = hash_refresh_token(&token);
    Ok((token, hash))
}

pub fn hash_refresh_token(token: &str) -> Vec<u8> {
    Sha256::digest(token.as_bytes()).to_vec()
}

pub const MIN_PASSWORD_LEN: usize = 8;

/// A random password for a new account, shown once to whoever created it and changed at the
/// first login. Easy to read off a screen and type on a phone: no 0/O or 1/l/I.
pub fn temporary_password() -> Result<String> {
    const ALPHABET: &[u8] = b"abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    const LEN: usize = 12;
    // bytes from the top of the range would make the first characters more likely
    let limit = 256 - 256 % ALPHABET.len();

    let mut password = String::with_capacity(LEN);
    while password.len() < LEN {
        let mut bytes = [0u8; 16];
        getrandom::fill(&mut bytes).map_err(|e| Error::Internal(e.to_string()))?;
        password.extend(
            bytes
                .iter()
                .map(|&byte| byte as usize)
                .filter(|&byte| byte < limit)
                .map(|byte| ALPHABET[byte % ALPHABET.len()] as char)
                .take(LEN - password.len()),
        );
    }
    Ok(password)
}

pub fn hash_password(password: &str) -> Result<String> {
    Argon2::default()
        .hash_password(password.as_bytes())
        .map(|hash| hash.to_string())
        .map_err(|e| Error::Internal(e.to_string()))
}

/// Verifies on a blocking thread, argon2 is deliberately slow and would stall the runtime.
/// Pass `None` for unknown users so the response time doesn't reveal which emails exist.
pub async fn verify_password(password: String, hash: Option<String>) -> Result<bool> {
    static DUMMY_HASH: LazyLock<String> =
        LazyLock::new(|| hash_password("dummy password").expect("argon2 hashing failed"));

    tokio::task::spawn_blocking(move || {
        let hash = hash.unwrap_or_else(|| DUMMY_HASH.clone());
        let Ok(parsed) = PasswordHash::new(&hash) else {
            return false;
        };
        Argon2::default()
            .verify_password(password.as_bytes(), &parsed)
            .is_ok()
    })
    .await
    .map_err(|e| Error::Internal(e.to_string()))
}

pub async fn auth(
    State(state): State<Arc<AppState>>,
    mut req: Request,
    next: Next,
) -> Result<Response> {
    let current_user = req
        .headers()
        .get(http::header::AUTHORIZATION)
        .and_then(|header| header.to_str().ok())
        .and_then(|header| header.strip_prefix("Bearer "))
        .and_then(|token| state.tokens.verify_access_token(token))
        .ok_or(Error::Unauthorized)?;

    if current_user.must_change_password && req.uri().path() != CHANGE_PASSWORD_PATH {
        return Err(Error::Forbidden(format!(
            "change the temporary password first, at POST {CHANGE_PASSWORD_PATH}"
        )));
    }

    // insert the current user into a request extension so the handler can
    // extract it
    req.extensions_mut().insert(current_user);
    Ok(next.run(req).await)
}
