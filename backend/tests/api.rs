//! End-to-end tests through the router. Each test gets its own database, so they need
//! `DATABASE_URL` pointing at a Postgres server (see `.env.example` and `docker-compose.yml`).

use {
    axum::{
        Router,
        body::Body,
        http::{Method, Request, StatusCode, header},
    },
    fixtag_api::{DEV_SEED, config::Config, routes::router, state::State},
    http_body_util::BodyExt,
    serde_json::{Value, json},
    sqlx::PgPool,
    tower::ServiceExt,
    uuid::Uuid,
};

const PASSWORD: &str = "fixtag123";
// Cliente Demo, served by Fabricante Demo
const OPERADOR: &str = "operador@fixtag.test";
const OPERADOR2: &str = "operador2@fixtag.test";
const GESTOR_CLIENTE: &str = "gestor2@fixtag.test";
// Metalúrgica Norte, served by both manufacturers
const OPERADOR_NORTE: &str = "operador3@fixtag.test";
// Fabricante Demo
const TECNICO: &str = "tecnico@fixtag.test";
const ASSISTENCIA: &str = "assistencia@fixtag.test";
const GESTOR: &str = "gestor@fixtag.test";
// Compressores Atlântico, the second manufacturer
const ASSISTENCIA_ATLANTICO: &str = "assistencia2@fixtag.test";
const TECNICO_ATLANTICO: &str = "tecnico4@fixtag.test";
// Outro Cliente, added by app(), also served by Fabricante Demo
const OUTRO: &str = "outro@fixtag.test";

const CLIENTE_DEMO: &str = "a0000000-0000-4000-8000-000000000001";
const FABRICANTE: &str = "a0000000-0000-4000-8000-000000000002";
const METALURGICA: &str = "a0000000-0000-4000-8000-000000000003";
const ATLANTICO: &str = "a0000000-0000-4000-8000-000000000004";
const OTHER_ORG: &str = "a0000000-0000-4000-8000-0000000000ff";

const ANA: &str = "b0000000-0000-4000-8000-000000000001";
const BRUNO: &str = "b0000000-0000-4000-8000-000000000002";
const CARLA: &str = "b0000000-0000-4000-8000-000000000003";
const EVA: &str = "b0000000-0000-4000-8000-000000000005";
const MARCO: &str = "b0000000-0000-4000-8000-000000000012";

const PRENSA: &str = "c0000000-0000-4000-8000-000000000001";
const TORNO: &str = "c0000000-0000-4000-8000-000000000002";
// no seeded incidents, so counts in the sync tests are exact
const COMPRESSOR: &str = "c0000000-0000-4000-8000-000000000003";
// Compressores Atlântico's, at Metalúrgica Norte
const COMPRESSOR_ATLANTICO: &str = "c0000000-0000-4000-8000-000000000012";
const OTHER_ORG_MACHINE: &str = "c0000000-0000-4000-8000-0000000000ff";

// FT-0001's oil leak, handled by Fabricante Demo
const FUGA_OLEO: &str = "d0000000-0000-4000-8000-000000000001";
// CA-2001's, assigned to Marco, and CA-2002's, still waiting for a technician
const PRESSAO_BAIXA: &str = "d0000000-0000-4000-8000-000000000013";
const FUGA_AR: &str = "d0000000-0000-4000-8000-000000000014";

const MAX_UPLOAD_BYTES: usize = 1024;

async fn app(pool: &PgPool) -> Router {
    sqlx::raw_sql(DEV_SEED).execute(pool).await.unwrap();
    // another client of Fabricante Demo, to check that clients can't see each other's data
    sqlx::raw_sql(
        "INSERT INTO organizations (id, name, kind, nif)
         VALUES ('a0000000-0000-4000-8000-0000000000ff', 'Outro Cliente', 'cliente', '500000050');
         INSERT INTO client_manufacturers (client_id, manufacturer_id)
         VALUES ('a0000000-0000-4000-8000-0000000000ff', 'a0000000-0000-4000-8000-000000000002');
         INSERT INTO users (organization_id, organization_kind, name, email, password_hash, role)
         SELECT 'a0000000-0000-4000-8000-0000000000ff', 'cliente', 'Outro', 'outro@fixtag.test',
                password_hash, 'operador'
           FROM users WHERE email = 'operador@fixtag.test';
         INSERT INTO machines (id, organization_id, manufacturer_id, serial_number, model)
         VALUES ('c0000000-0000-4000-8000-0000000000ff', 'a0000000-0000-4000-8000-0000000000ff',
                 'a0000000-0000-4000-8000-000000000002', 'OUT-0001', 'Outra máquina');",
    )
    .execute(pool)
    .await
    .unwrap();

    let config = Config {
        port: "0".into(),
        database_url: String::new(),
        jwt_secret: "test-secret-test-secret-test-secret".into(),
        storage_dir: std::env::temp_dir().join(format!("fixtag-test-{}", Uuid::new_v4())),
        access_token_ttl_secs: 900,
        refresh_token_ttl_days: 30,
        max_upload_bytes: MAX_UPLOAD_BYTES,
    };
    router(State::init(pool.clone(), &config))
}

async fn send(app: &Router, request: Request<Body>) -> (StatusCode, Vec<u8>) {
    let response = app.clone().oneshot(request).await.unwrap();
    let status = response.status();
    let body = response.into_body().collect().await.unwrap().to_bytes();
    (status, body.to_vec())
}

async fn call(
    app: &Router,
    method: Method,
    uri: &str,
    token: Option<&str>,
    body: Option<Value>,
) -> (StatusCode, Value) {
    let mut request = Request::builder().method(method).uri(uri);
    if let Some(token) = token {
        request = request.header(header::AUTHORIZATION, format!("Bearer {token}"));
    }
    let request = match body {
        Some(body) => request
            .header(header::CONTENT_TYPE, "application/json")
            .body(Body::from(body.to_string())),
        None => request.body(Body::empty()),
    }
    .unwrap();
    let (status, body) = send(app, request).await;
    let json = serde_json::from_slice(&body).unwrap_or(Value::Null);
    (status, json)
}

/// The access token and the user's profile.
async fn login_as(app: &Router, email: &str) -> (String, Value) {
    let (status, body) = call(
        app,
        Method::POST,
        "/login",
        None,
        Some(json!({ "email": email, "password": PASSWORD })),
    )
    .await;
    assert_eq!(status, StatusCode::OK, "{body}");
    (
        body["access_token"].as_str().unwrap().to_string(),
        body["user"].clone(),
    )
}

async fn login(app: &Router, email: &str) -> String {
    login_as(app, email).await.0
}

/// Number of entries in a list response, paginated or not.
async fn count(app: &Router, token: &str, uri: &str) -> usize {
    let (status, body) = call(app, Method::GET, uri, Some(token), None).await;
    assert_eq!(status, StatusCode::OK, "{body}");
    body.get("items").unwrap_or(&body).as_array().unwrap().len()
}

/// Names in a list response, in order.
fn names(body: &Value) -> Vec<String> {
    body.as_array()
        .unwrap()
        .iter()
        .map(|item| item["name"].as_str().unwrap().to_string())
        .collect()
}

fn incident_body(machine_id: &str) -> Value {
    json!({
        "machine_id": machine_id,
        "description": "Motor não arranca",
        "severity": "critica",
        "latitude": 41.15,
        "longitude": -8.61,
        "accuracy_m": 8.5,
        "reported_at": "2026-10-01T10:00:00Z",
    })
}

async fn create_incident(app: &Router, token: &str, machine_id: &str) -> Value {
    let (status, body) = call(
        app,
        Method::PUT,
        &format!("/incidents/{}", Uuid::new_v4()),
        Some(token),
        Some(incident_body(machine_id)),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{body}");
    body
}

async fn upload(
    app: &Router,
    token: &str,
    incident_id: &str,
    attachment_id: &str,
    content_type: &str,
    data: &'static [u8],
) -> (StatusCode, Value) {
    let request = Request::builder()
        .method(Method::PUT)
        .uri(format!(
            "/incidents/{incident_id}/attachments/{attachment_id}"
        ))
        .header(header::AUTHORIZATION, format!("Bearer {token}"))
        .header(header::CONTENT_TYPE, content_type)
        .body(Body::from(data))
        .unwrap();
    let (status, body) = send(app, request).await;
    (status, serde_json::from_slice(&body).unwrap_or(Value::Null))
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn health_and_login(pool: PgPool) {
    let app = app(&pool).await;

    let (status, body) = send(&app, Request::get("/").body(Body::empty()).unwrap()).await;
    assert_eq!(
        (status, body.as_slice()),
        (StatusCode::OK, b"ok".as_slice())
    );

    for (email, password) in [(OPERADOR, "wrong"), ("nobody@fixtag.test", PASSWORD)] {
        let (status, body) = call(
            &app,
            Method::POST,
            "/login",
            None,
            Some(json!({ "email": email, "password": password })),
        )
        .await;
        assert_eq!(status, StatusCode::UNAUTHORIZED);
        assert!(body["error"].is_string());
    }

    // emails are case insensitive
    let (status, body) = call(
        &app,
        Method::POST,
        "/login",
        None,
        Some(json!({ "email": " Operador@FixTag.test ", "password": PASSWORD })),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body["token_type"], "Bearer");
    assert_eq!(body["expires_in"], 900);
    assert_eq!(body["user"]["role"], "operador");
    assert_eq!(body["user"]["organization_kind"], "cliente");
    assert_eq!(body["user"]["name"], "Ana Operadora");
    assert_eq!(body["user"]["must_change_password"], false);
    assert!(body["user"].get("password_hash").is_none());
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn protected_routes_need_a_valid_token(pool: PgPool) {
    let app = app(&pool).await;

    for token in [None, Some("garbage")] {
        let (status, body) = call(&app, Method::GET, "/incidents", token, None).await;
        assert_eq!(status, StatusCode::UNAUTHORIZED);
        assert!(body["error"].is_string());
    }

    // signed with another secret
    let forged = jsonwebtoken::encode(
        &jsonwebtoken::Header::default(),
        &json!({ "sub": Uuid::new_v4(), "org": Uuid::new_v4(), "role": "assistencia",
                 "iat": 0, "exp": 4_000_000_000i64 }),
        &jsonwebtoken::EncodingKey::from_secret(b"not-the-server-secret-not-the-server"),
    )
    .unwrap();
    let (status, _) = call(&app, Method::GET, "/incidents", Some(&forged), None).await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn refresh_rotates_tokens(pool: PgPool) {
    let app = app(&pool).await;
    let (_, first) = call(
        &app,
        Method::POST,
        "/login",
        None,
        Some(json!({ "email": OPERADOR, "password": PASSWORD })),
    )
    .await;
    let old_refresh = first["refresh_token"].as_str().unwrap();

    let refresh = |token: &str| {
        call(
            &app,
            Method::POST,
            "/refresh",
            None,
            Some(json!({ "refresh_token": token })),
        )
    };

    let (status, second) = refresh(old_refresh).await;
    assert_eq!(status, StatusCode::OK);
    assert_ne!(second["refresh_token"], first["refresh_token"]);
    assert_eq!(second["user"]["email"], OPERADOR);
    let new_access = second["access_token"].as_str().unwrap();
    let (status, _) = call(&app, Method::GET, "/machines", Some(new_access), None).await;
    assert_eq!(status, StatusCode::OK);

    // the response could have been lost, so the old token still works for a moment
    let (status, _) = refresh(old_refresh).await;
    assert_eq!(status, StatusCode::OK);

    // but not once the grace period is over
    sqlx::query("UPDATE refresh_tokens SET revoked_at = now() - interval '2 minutes' WHERE revoked_at IS NOT NULL")
        .execute(&pool)
        .await
        .unwrap();
    let (status, _) = refresh(old_refresh).await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);

    // expired tokens are rejected too
    let current = second["refresh_token"].as_str().unwrap();
    sqlx::query("UPDATE refresh_tokens SET expires_at = now() - interval '1 second'")
        .execute(&pool)
        .await
        .unwrap();
    let (status, _) = refresh(current).await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);

    let (status, _) = refresh("not-a-token").await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn machines_are_scoped_to_the_organization(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let assistencia = login(&app, ASSISTENCIA).await;

    let (_, body) = call(&app, Method::GET, "/machines", Some(&operador), None).await;
    let serials: Vec<_> = body
        .as_array()
        .unwrap()
        .iter()
        .map(|m| m["serial_number"].as_str().unwrap())
        .collect();
    assert_eq!(
        serials,
        [
            "FT-0001", "FT-0002", "FT-0003", "FT-0004", "FT-0005", "FT-0006", "FT-0007", "FT-0008"
        ]
    );

    // the 11 Fabricante Demo made plus the other client's, not Compressores Atlântico's
    assert_eq!(count(&app, &assistencia, "/machines").await, 12);
    let atlantico = login(&app, ASSISTENCIA_ATLANTICO).await;
    assert_eq!(count(&app, &atlantico, "/machines").await, 2);
    // the client sees every machine it owns, whoever made it
    let norte = login(&app, OPERADOR_NORTE).await;
    let (_, body) = call(&app, Method::GET, "/machines", Some(&norte), None).await;
    let makers: Vec<_> = body
        .as_array()
        .unwrap()
        .iter()
        .map(|m| m["manufacturer_id"].as_str().unwrap())
        .collect();
    assert_eq!(
        makers,
        [ATLANTICO, ATLANTICO, FABRICANTE, FABRICANTE, FABRICANTE]
    );

    let (_, body) = call(
        &app,
        Method::GET,
        "/machines?serial_number=FT-0002",
        Some(&operador),
        None,
    )
    .await;
    assert_eq!(body.as_array().unwrap().len(), 1);
    assert_eq!(body[0]["id"], TORNO);

    let (_, body) = call(
        &app,
        Method::GET,
        "/machines?serial_number=OUT-0001",
        Some(&operador),
        None,
    )
    .await;
    assert_eq!(body, json!([]));
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn create_incident_is_idempotent(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let id = Uuid::new_v4();
    let uri = format!("/incidents/{id}");

    let (status, created) = call(
        &app,
        Method::PUT,
        &uri,
        Some(&operador),
        Some(incident_body(PRENSA)),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{created}");
    assert_eq!(created["id"], id.to_string());
    assert_eq!(created["status"], "recebida");
    assert_eq!(created["severity"], "critica");
    assert_eq!(created["author_name"], "Ana Operadora");
    assert_eq!(created["organization_id"], CLIENTE_DEMO);
    assert_eq!(created["handled_by"], FABRICANTE);
    assert_eq!(created["accuracy_m"], 8.5);
    assert_eq!(created["reported_at"], "2026-10-01T10:00:00Z");
    assert_eq!(created["attachments"], json!([]));
    assert_eq!(created["updates"], json!([]));

    // a retry changes nothing, not even updated_at
    let (status, retried) = call(
        &app,
        Method::PUT,
        &uri,
        Some(&operador),
        Some(incident_body(PRENSA)),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(retried, created);
    let count: i64 = sqlx::query_scalar("SELECT count(*) FROM incidents WHERE id = $1")
        .bind(id)
        .fetch_one(&pool)
        .await
        .unwrap();
    assert_eq!(count, 1);

    // the author can edit the descriptive fields
    let mut edited = incident_body(PRENSA);
    edited["description"] = json!("Motor não arranca, cheiro a queimado");
    edited["latitude"] = Value::Null;
    edited["longitude"] = Value::Null;
    let (status, body) = call(
        &app,
        Method::PUT,
        &uri,
        Some(&operador),
        Some(edited.clone()),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body["description"], "Motor não arranca, cheiro a queimado");
    assert_eq!(body["latitude"], Value::Null);
    assert_ne!(body["updated_at"], created["updated_at"]);

    // nobody else can, not even from the same organization
    let colleague = login(&app, OPERADOR2).await;
    let (status, _) = call(
        &app,
        Method::PUT,
        &uri,
        Some(&colleague),
        Some(edited.clone()),
    )
    .await;
    assert_eq!(status, StatusCode::FORBIDDEN);

    // and the machine is fixed once reported
    let (status, _) = call(
        &app,
        Method::PUT,
        &uri,
        Some(&operador),
        Some(incident_body(TORNO)),
    )
    .await;
    assert_eq!(status, StatusCode::CONFLICT);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn create_incident_validates_input(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let put = |body: Value| {
        let uri = format!("/incidents/{}", Uuid::new_v4());
        let operador = operador.clone();
        let app = app.clone();
        async move { call(&app, Method::PUT, &uri, Some(&operador), Some(body)).await }
    };

    let mut only_latitude = incident_body(PRENSA);
    only_latitude["longitude"] = Value::Null;
    let (status, _) = put(only_latitude).await;
    assert_eq!(status, StatusCode::BAD_REQUEST);

    let mut out_of_range = incident_body(PRENSA);
    out_of_range["latitude"] = json!(123.0);
    let (status, body) = put(out_of_range).await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    assert!(
        body["error"].as_str().unwrap().contains("constraint"),
        "{body}"
    );

    let (status, _) = put(incident_body(&Uuid::new_v4().to_string())).await;
    assert_eq!(status, StatusCode::BAD_REQUEST);

    // another client's machine looks exactly like a machine that doesn't exist
    let (status, _) = put(incident_body(OTHER_ORG_MACHINE)).await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    // and so does another manufacturer's, even at a client both serve
    let atlantico = login(&app, ASSISTENCIA_ATLANTICO).await;
    let (status, _) = call(
        &app,
        Method::PUT,
        &format!("/incidents/{}", Uuid::new_v4()),
        Some(&atlantico),
        Some(incident_body(PRENSA)),
    )
    .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);

    let mut bad_severity = incident_body(PRENSA);
    bad_severity["severity"] = json!("enorme");
    let (status, _) = put(bad_severity).await;
    assert_eq!(status, StatusCode::UNPROCESSABLE_ENTITY);

    // a voice report may come without a description
    let mut no_description = incident_body(PRENSA);
    no_description
        .as_object_mut()
        .unwrap()
        .remove("description");
    let (status, body) = put(no_description).await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(body["description"], "");
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn clients_only_see_their_own_incidents(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let outro = login(&app, OUTRO).await;
    let assistencia = login(&app, ASSISTENCIA).await;

    let incident = create_incident(&app, &operador, PRENSA).await;
    let uri = format!("/incidents/{}", incident["id"].as_str().unwrap());

    let (status, _) = call(&app, Method::GET, &uri, Some(&outro), None).await;
    assert_eq!(status, StatusCode::NOT_FOUND);
    let (_, body) = call(&app, Method::GET, "/incidents", Some(&outro), None).await;
    assert_eq!(body["items"], json!([]));

    let (status, body) = call(&app, Method::GET, &uri, Some(&assistencia), None).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(body, incident);

    // Cliente Demo's 8 seeded incidents plus the new one
    assert_eq!(count(&app, &operador, "/incidents").await, 9);

    let (status, _) = call(
        &app,
        Method::GET,
        &format!("/incidents/{}", Uuid::new_v4()),
        Some(&operador),
        None,
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn support_changes_the_status(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let tecnico = login(&app, TECNICO).await;
    let assistencia = login(&app, ASSISTENCIA).await;
    let outro = login(&app, OUTRO).await;

    let incident = create_incident(&app, &operador, PRENSA).await;
    let uri = format!("/incidents/{}/update", incident["id"].as_str().unwrap());
    let resolve = || Some(json!({ "new_status": "resolvida" }));

    let (status, _) = call(&app, Method::POST, &uri, Some(&operador), resolve()).await;
    assert_eq!(status, StatusCode::FORBIDDEN);
    // a technician doesn't even see an incident nobody assigned them
    let (status, _) = call(&app, Method::POST, &uri, Some(&tecnico), resolve()).await;
    assert_eq!(status, StatusCode::NOT_FOUND);

    let (status, body) = call(
        &app,
        Method::POST,
        &uri,
        Some(&operador),
        Some(json!({ "message": "  Continua a perder óleo  " })),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(body["status"], "recebida");
    assert_eq!(body["updates"][0]["message"], "Continua a perder óleo");
    assert_eq!(body["updates"][0]["new_status"], Value::Null);

    let update_id = Uuid::new_v4();
    let status_change = json!({
        "id": update_id,
        "message": "Peça encomendada",
        "new_status": "em_analise",
    });
    let (status, body) = call(
        &app,
        Method::POST,
        &uri,
        Some(&assistencia),
        Some(status_change.clone()),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(body["status"], "em_analise");
    assert_eq!(body["updates"].as_array().unwrap().len(), 2);
    assert_eq!(body["updates"][1]["author_name"], "Carla Assistência");
    assert_eq!(body["updates"][1]["id"], update_id.to_string());

    // a retry with the same id doesn't add a second entry
    let (status, retried) = call(
        &app,
        Method::POST,
        &uri,
        Some(&assistencia),
        Some(status_change.clone()),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(retried, body);

    // the same id on another incident is a client bug
    let other = create_incident(&app, &operador, PRENSA).await;
    let (status, _) = call(
        &app,
        Method::POST,
        &format!("/incidents/{}/update", other["id"].as_str().unwrap()),
        Some(&assistencia),
        Some(status_change),
    )
    .await;
    assert_eq!(status, StatusCode::CONFLICT);

    for empty in [json!({}), json!({ "message": "   " })] {
        let (status, _) = call(&app, Method::POST, &uri, Some(&operador), Some(empty)).await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
    }

    let (status, _) = call(
        &app,
        Method::POST,
        &uri,
        Some(&outro),
        Some(json!({ "message": "olá" })),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn incremental_sync_with_cursor(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let assistencia = login(&app, ASSISTENCIA).await;
    let list = |query: String| {
        let app = app.clone();
        let operador = operador.clone();
        async move {
            let uri = format!("/incidents?machine_id={COMPRESSOR}&{query}");
            let (status, body) = call(&app, Method::GET, &uri, Some(&operador), None).await;
            assert_eq!(status, StatusCode::OK, "{body}");
            body
        }
    };
    let ids = |page: &Value| -> Vec<String> {
        page["items"]
            .as_array()
            .unwrap()
            .iter()
            .map(|i| i["id"].as_str().unwrap().to_string())
            .collect()
    };

    let mut created = Vec::new();
    for _ in 0..3 {
        created.push(
            create_incident(&app, &operador, COMPRESSOR).await["id"]
                .as_str()
                .unwrap()
                .to_string(),
        );
    }

    let first = list("limit=2".into()).await;
    assert_eq!(ids(&first), created[..2]);
    assert_eq!(first["has_more"], true);

    let cursor = first["next_cursor"].as_str().unwrap();
    let second = list(format!("limit=2&cursor={cursor}")).await;
    assert_eq!(ids(&second), created[2..]);
    assert_eq!(second["has_more"], false);

    // nothing new: same cursor back, so the app can always just store it
    let watermark = second["next_cursor"].as_str().unwrap().to_string();
    let empty = list(format!("cursor={watermark}")).await;
    assert_eq!(empty["items"], json!([]));
    assert_eq!(empty["next_cursor"], watermark.as_str());

    // a status change made by support shows up in the next sync
    call(
        &app,
        Method::POST,
        &format!("/incidents/{}/update", created[0]),
        Some(&assistencia),
        Some(json!({ "new_status": "resolvida" })),
    )
    .await;
    let changed = list(format!("cursor={watermark}")).await;
    assert_eq!(ids(&changed), created[..1]);
    assert_eq!(changed["items"][0]["status"], "resolvida");

    // and so does a new attachment
    let watermark = changed["next_cursor"].as_str().unwrap().to_string();
    upload(
        &app,
        &operador,
        &created[1],
        &Uuid::new_v4().to_string(),
        "image/jpeg",
        b"jpeg",
    )
    .await;
    let changed = list(format!("cursor={watermark}")).await;
    assert_eq!(ids(&changed), created[1..2]);
    assert_eq!(
        changed["items"][0]["attachments"].as_array().unwrap().len(),
        1
    );

    let resolved = list("status=resolvida".into()).await;
    assert_eq!(ids(&resolved), created[..1]);
    let open = list("status=recebida&severity=critica".into()).await;
    assert_eq!(ids(&open).len(), 2);

    let (status, _) = call(
        &app,
        Method::GET,
        "/incidents?cursor=nonsense",
        Some(&operador),
        None,
    )
    .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn attachments_upload_and_download(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let outro = login(&app, OUTRO).await;
    let assistencia = login(&app, ASSISTENCIA).await;

    let incident = create_incident(&app, &operador, PRENSA).await;
    let incident_id = incident["id"].as_str().unwrap();
    let attachment_id = Uuid::new_v4().to_string();
    let photo: &[u8] = b"\xff\xd8\xff\xe0 pretend this is a jpeg";

    let (status, body) = upload(
        &app,
        &operador,
        incident_id,
        &attachment_id,
        "image/jpeg",
        photo,
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{body}");
    assert_eq!(body["kind"], "foto");
    assert_eq!(body["content_type"], "image/jpeg");
    assert_eq!(body["size_bytes"], photo.len());
    assert!(body.get("storage_key").is_none());

    // re-sending after a lost response returns the stored attachment
    let (status, again) = upload(
        &app,
        &operador,
        incident_id,
        &attachment_id,
        "image/jpeg",
        photo,
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(again, body);

    let download = |token: String| {
        let app = app.clone();
        let uri = format!("/attachments/{attachment_id}");
        async move {
            let request = Request::get(uri)
                .header(header::AUTHORIZATION, format!("Bearer {token}"))
                .body(Body::empty())
                .unwrap();
            let response = app.oneshot(request).await.unwrap();
            let status = response.status();
            let content_type = response.headers().get(header::CONTENT_TYPE).cloned();
            let body = response.into_body().collect().await.unwrap().to_bytes();
            (status, content_type, body)
        }
    };
    let (status, content_type, data) = download(assistencia.clone()).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(content_type.unwrap(), "image/jpeg");
    assert_eq!(data.as_ref(), photo);
    let (status, _, _) = download(outro.clone()).await;
    assert_eq!(status, StatusCode::NOT_FOUND);

    let (_, detail) = call(
        &app,
        Method::GET,
        &format!("/incidents/{incident_id}"),
        Some(&operador),
        None,
    )
    .await;
    assert_eq!(detail["attachments"][0]["id"], attachment_id.as_str());

    let (status, body) = upload(
        &app,
        &operador,
        incident_id,
        &Uuid::new_v4().to_string(),
        "audio/mp4; codecs=mp4a",
        b"aac",
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    assert_eq!(body["kind"], "audio");
    assert_eq!(body["content_type"], "audio/mp4");

    let (status, _) = upload(
        &app,
        &operador,
        incident_id,
        &Uuid::new_v4().to_string(),
        "text/plain",
        b"hi",
    )
    .await;
    assert_eq!(status, StatusCode::UNSUPPORTED_MEDIA_TYPE);

    let (status, _) = upload(
        &app,
        &operador,
        incident_id,
        &Uuid::new_v4().to_string(),
        "image/png",
        b"",
    )
    .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);

    let too_big: &'static [u8] = vec![0u8; MAX_UPLOAD_BYTES + 1].leak();
    let (status, _) = upload(
        &app,
        &operador,
        incident_id,
        &Uuid::new_v4().to_string(),
        "image/png",
        too_big,
    )
    .await;
    assert_eq!(status, StatusCode::PAYLOAD_TOO_LARGE);

    // incident not synced yet, the app should retry later
    let (status, _) = upload(
        &app,
        &operador,
        &Uuid::new_v4().to_string(),
        &Uuid::new_v4().to_string(),
        "image/png",
        b"png",
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);

    let (status, _) = upload(
        &app,
        &outro,
        incident_id,
        &Uuid::new_v4().to_string(),
        "image/png",
        b"png",
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);

    let other = create_incident(&app, &operador, PRENSA).await;
    let (status, _) = upload(
        &app,
        &operador,
        other["id"].as_str().unwrap(),
        &attachment_id,
        "image/jpeg",
        photo,
    )
    .await;
    assert_eq!(status, StatusCode::CONFLICT);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn device_tokens_follow_the_last_user(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let assistencia = login(&app, ASSISTENCIA).await;
    let register = |token: String| {
        let app = app.clone();
        async move {
            call(
                &app,
                Method::POST,
                "/devices",
                Some(&token),
                Some(json!({ "fcm_token": "fcm-abc" })),
            )
            .await
            .0
        }
    };

    assert_eq!(register(operador).await, StatusCode::NO_CONTENT);
    assert_eq!(register(assistencia).await, StatusCode::NO_CONTENT);

    let owners: Vec<String> = sqlx::query_scalar(
        "SELECT u.email FROM devices d JOIN users u ON u.id = d.user_id WHERE d.fcm_token = 'fcm-abc'",
    )
    .fetch_all(&pool)
    .await
    .unwrap();
    assert_eq!(owners, [ASSISTENCIA]);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn manufacturers_only_see_the_machines_they_made(pool: PgPool) {
    let app = app(&pool).await;

    // every incident on Fabricante Demo's machines, at both of its seeded clients
    let assistencia = login(&app, ASSISTENCIA).await;
    assert_eq!(count(&app, &assistencia, "/incidents").await, 12);
    let gestor = login(&app, GESTOR).await;
    assert_eq!(count(&app, &gestor, "/incidents").await, 12);
    let atlantico = login(&app, ASSISTENCIA_ATLANTICO).await;
    assert_eq!(count(&app, &atlantico, "/incidents").await, 2);

    // Metalúrgica Norte buys from both, so its staff see both manufacturers' incidents
    let norte = login(&app, OPERADOR_NORTE).await;
    assert_eq!(count(&app, &norte, "/incidents").await, 6);

    // but neither manufacturer sees the other's, even at the client they share
    for (token, other) in [(&assistencia, PRESSAO_BAIXA), (&atlantico, FUGA_OLEO)] {
        let (status, _) = call(
            &app,
            Method::GET,
            &format!("/incidents/{other}"),
            Some(token),
            None,
        )
        .await;
        assert_eq!(status, StatusCode::NOT_FOUND);
    }
    let (_, page) = call(&app, Method::GET, "/incidents", Some(&atlantico), None).await;
    for incident in page["items"].as_array().unwrap() {
        assert_eq!(incident["handled_by"], ATLANTICO);
        assert_eq!(incident["organization_id"], METALURGICA);
    }

    // technicians see the incidents they've been assigned, Bruno's 5 and Marco's 1, plus their
    // manufacturer's machines to report new ones
    let (tecnico, user) = login_as(&app, TECNICO).await;
    assert_eq!(user["organization_kind"], "fabricante");
    assert_eq!(count(&app, &tecnico, "/incidents").await, 5);
    assert_eq!(count(&app, &tecnico, "/machines").await, 12);
    let marco = login(&app, TECNICO_ATLANTICO).await;
    let (_, page) = call(&app, Method::GET, "/incidents", Some(&marco), None).await;
    assert_eq!(page["items"].as_array().unwrap().len(), 1);
    assert_eq!(page["items"][0]["id"], PRESSAO_BAIXA);
    let (status, _) = call(
        &app,
        Method::GET,
        &format!("/incidents/{FUGA_AR}"),
        Some(&marco),
        None,
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn support_assigns_technicians(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let tecnico = login(&app, TECNICO).await;
    let assistencia = login(&app, ASSISTENCIA).await;

    let incident = create_incident(&app, &operador, PRENSA).await;
    let incident_id = incident["id"].as_str().unwrap().to_string();
    let uri = format!("/incidents/{incident_id}/update");
    let assign = |technician: &str| json!({ "assigned_to": technician });

    let (status, _) = call(
        &app,
        Method::POST,
        &uri,
        Some(&operador),
        Some(assign(BRUNO)),
    )
    .await;
    assert_eq!(status, StatusCode::FORBIDDEN);
    let (status, _) = call(
        &app,
        Method::POST,
        &uri,
        Some(&tecnico),
        Some(assign(BRUNO)),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);

    // only the technicians of the manufacturer handling it: not an operator, not support
    // staff, not another manufacturer's technician, not someone who doesn't exist
    let nobody = Uuid::new_v4().to_string();
    for not_assignable in [ANA, CARLA, MARCO, nobody.as_str()] {
        let (status, body) = call(
            &app,
            Method::POST,
            &uri,
            Some(&assistencia),
            Some(assign(not_assignable)),
        )
        .await;
        assert_eq!(status, StatusCode::BAD_REQUEST, "{body}");
    }

    let assignment = json!({ "id": Uuid::new_v4(), "assigned_to": BRUNO });
    let (status, assigned) = call(
        &app,
        Method::POST,
        &uri,
        Some(&assistencia),
        Some(assignment.clone()),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{assigned}");
    assert_eq!(assigned["assigned_to"], BRUNO);
    assert_eq!(assigned["assigned_to_name"], "Bruno Técnico");
    assert_eq!(assigned["status"], "recebida");
    assert_eq!(assigned["updates"][0]["assigned_to"], BRUNO);
    assert_eq!(assigned["updates"][0]["assigned_to_name"], "Bruno Técnico");
    assert_eq!(assigned["updates"][0]["message"], Value::Null);
    // so the next sync delivers it
    assert_ne!(assigned["updated_at"], incident["updated_at"]);

    // a retry with the same id doesn't add a second entry
    let (status, retried) = call(
        &app,
        Method::POST,
        &uri,
        Some(&assistencia),
        Some(assignment),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(retried, assigned);

    // reassigning replaces the technician, together with a status change and a message
    let (status, body) = call(
        &app,
        Method::POST,
        &uri,
        Some(&assistencia),
        Some(json!({
            "message": "A Eva está mais perto",
            "new_status": "em_analise",
            "assigned_to": EVA,
        })),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{body}");
    assert_eq!(body["assigned_to"], EVA);
    assert_eq!(body["status"], "em_analise");

    // Bruno keeps seeing it after the reassignment, he worked on it, and can still write in it.
    // A message keeps the assignee
    let (status, body) = call(
        &app,
        Method::POST,
        &uri,
        Some(&tecnico),
        Some(json!({ "message": "Tenho a peça na carrinha, se for preciso" })),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{body}");
    assert_eq!(body["assigned_to"], EVA);
    assert_eq!(body["updates"].as_array().unwrap().len(), 3);
    assert_eq!(body["updates"][2]["author_name"], "Bruno Técnico");
    assert_eq!(body["updates"][2]["assigned_to"], Value::Null);

    // but it's Eva's now, so only she or support staff change its status
    let (status, _) = call(
        &app,
        Method::POST,
        &uri,
        Some(&tecnico),
        Some(json!({ "new_status": "resolvida" })),
    )
    .await;
    assert_eq!(status, StatusCode::FORBIDDEN);

    // a technician's queue: Eva now has it on top of her 2 seeded ones, Bruno keeps his 5
    let queue = |technician: &'static str| {
        let app = app.clone();
        let assistencia = assistencia.clone();
        async move {
            let uri = format!("/incidents?assigned_to={technician}");
            let (_, page) = call(&app, Method::GET, &uri, Some(&assistencia), None).await;
            page["items"]
                .as_array()
                .unwrap()
                .iter()
                .map(|i| i["id"].as_str().unwrap().to_string())
                .collect::<Vec<_>>()
        }
    };
    let eva = queue(EVA).await;
    assert_eq!(eva.len(), 3);
    assert!(eva.contains(&incident_id));
    let bruno = queue(BRUNO).await;
    assert_eq!(bruno.len(), 5);
    assert!(!bruno.contains(&incident_id));
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn assigned_technicians_change_the_status(pool: PgPool) {
    let app = app(&pool).await;
    let marco = login(&app, TECNICO_ATLANTICO).await;
    let update = |incident: &str, body: Value| {
        let (app, marco) = (app.clone(), marco.clone());
        let uri = format!("/incidents/{incident}/update");
        async move { call(&app, Method::POST, &uri, Some(&marco), Some(body)).await }
    };

    let (status, body) = update(
        PRESSAO_BAIXA,
        json!({ "message": "Filtro substituído", "new_status": "resolvida" }),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{body}");
    assert_eq!(body["status"], "resolvida");
    assert_eq!(body["assigned_to"], MARCO);

    // but technicians never assign, not even themselves
    let (status, _) = update(PRESSAO_BAIXA, json!({ "assigned_to": MARCO })).await;
    assert_eq!(status, StatusCode::FORBIDDEN);
    let (status, _) = update(FUGA_AR, json!({ "new_status": "em_analise" })).await;
    assert_eq!(status, StatusCode::NOT_FOUND);

    // what a technician reports themselves stays visible to them
    let reported = create_incident(&app, &marco, COMPRESSOR_ATLANTICO).await;
    assert_eq!(reported["handled_by"], ATLANTICO);
    assert_eq!(count(&app, &marco, "/incidents").await, 2);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn support_lists_technicians_least_busy_first(pool: PgPool) {
    let app = app(&pool).await;
    for email in [OPERADOR, TECNICO] {
        let token = login(&app, email).await;
        let (status, _) = call(&app, Method::GET, "/technicians", Some(&token), None).await;
        assert_eq!(status, StatusCode::FORBIDDEN);
    }

    let assistencia = login(&app, ASSISTENCIA).await;
    let (status, body) = call(&app, Method::GET, "/technicians", Some(&assistencia), None).await;
    assert_eq!(status, StatusCode::OK, "{body}");
    let listed: Vec<(&str, i64)> = body
        .as_array()
        .unwrap()
        .iter()
        .map(|t| {
            (
                t["name"].as_str().unwrap(),
                t["open_incidents"].as_i64().unwrap(),
            )
        })
        .collect();
    // resolved incidents don't count, this also checks the seed's history ended up applied
    assert_eq!(
        listed,
        [
            ("Eva Técnica", 0),
            ("Filipe Técnico", 2),
            ("Bruno Técnico", 3)
        ]
    );
    assert_eq!(body[0]["organization_id"], FABRICANTE);

    // a gestor can do what support staff do, and each manufacturer lists only its own
    let gestor = login(&app, GESTOR).await;
    assert_eq!(count(&app, &gestor, "/technicians").await, 3);
    let atlantico = login(&app, ASSISTENCIA_ATLANTICO).await;
    let (_, body) = call(&app, Method::GET, "/technicians", Some(&atlantico), None).await;
    assert_eq!(names(&body), ["Marco Técnico"]);
    assert_eq!(body[0]["open_incidents"], 1);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn support_registers_machines(pool: PgPool) {
    let app = app(&pool).await;
    let operador = login(&app, OPERADOR).await;
    let tecnico = login(&app, TECNICO).await;
    let assistencia = login(&app, ASSISTENCIA).await;
    let outro = login(&app, OUTRO).await;
    let machine = |organization_id: &str, serial_number: &str, model: &str| json!({ "organization_id": organization_id, "serial_number": serial_number, "model": model });
    let put = |token: &str, id: &str, body: Value| {
        let app = app.clone();
        let (token, uri) = (token.to_string(), format!("/machines/{id}"));
        async move { call(&app, Method::PUT, &uri, Some(&token), Some(body)).await }
    };
    let id = Uuid::new_v4().to_string();
    let new_press = machine(CLIENTE_DEMO, " FT-0009 ", "Prensa hidráulica PH-300");

    for token in [&operador, &tecnico] {
        let (status, _) = put(token, &id, new_press.clone()).await;
        assert_eq!(status, StatusCode::FORBIDDEN);
    }

    let (status, created) = put(&assistencia, &id, new_press.clone()).await;
    assert_eq!(status, StatusCode::CREATED, "{created}");
    assert_eq!(created["id"], id.as_str());
    assert_eq!(created["serial_number"], "FT-0009");
    assert_eq!(created["organization_id"], CLIENTE_DEMO);
    assert_eq!(created["manufacturer_id"], FABRICANTE);
    assert_eq!(created["model"], "Prensa hidráulica PH-300");

    // a retry after a lost response gets the same machine back
    let (status, retried) = put(&assistencia, &id, new_press).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(retried, created);

    // sending it again fixes a typo, but the machine can't move to another client
    let fixed = machine(CLIENTE_DEMO, "FT-0009", "Prensa hidráulica PH-350");
    let (status, edited) = put(&assistencia, &id, fixed).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(edited["model"], "Prensa hidráulica PH-350");
    let moved = machine(OTHER_ORG, "FT-0009", "Prensa hidráulica PH-350");
    let (status, _) = put(&assistencia, &id, moved).await;
    assert_eq!(status, StatusCode::CONFLICT);

    // the client sees it on their next sync, other clients don't
    let uri = "/machines?serial_number=FT-0009";
    let (_, body) = call(&app, Method::GET, uri, Some(&operador), None).await;
    assert_eq!(body[0]["id"], id.as_str());
    let (_, body) = call(&app, Method::GET, uri, Some(&outro), None).await;
    assert_eq!(body, json!([]));

    // serial numbers are unique across every client
    for serial_number in ["FT-0009", "FT-0001"] {
        let other = Uuid::new_v4().to_string();
        let (status, _) = put(
            &assistencia,
            &other,
            machine(OTHER_ORG, serial_number, "Outra"),
        )
        .await;
        assert_eq!(status, StatusCode::CONFLICT);
    }

    // machines belong to a client the manufacturer serves, and need a serial number and a model
    let nowhere = Uuid::new_v4().to_string();
    for invalid in [
        machine(FABRICANTE, "FT-0010", "Bancada de testes"),
        machine(&nowhere, "FT-0010", "Prensa"),
        machine(CLIENTE_DEMO, "   ", "Prensa"),
        machine(CLIENTE_DEMO, "FT-0010", ""),
    ] {
        let other = Uuid::new_v4().to_string();
        let (status, _) = put(&assistencia, &other, invalid).await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
    }
    let atlantico = login(&app, ASSISTENCIA_ATLANTICO).await;
    let other = Uuid::new_v4().to_string();
    let (status, _) = put(
        &atlantico,
        &other,
        machine(CLIENTE_DEMO, "CA-2003", "Compressor"),
    )
    .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);

    // and only the manufacturer that made a machine edits it, to the others it doesn't exist
    let (status, _) = put(
        &atlantico,
        PRENSA,
        machine(METALURGICA, "FT-0001", "Prensa"),
    )
    .await;
    assert_eq!(status, StatusCode::NOT_FOUND);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn support_registers_clients(pool: PgPool) {
    let app = app(&pool).await;
    let assistencia = login(&app, ASSISTENCIA).await;
    let atlantico = login(&app, ASSISTENCIA_ATLANTICO).await;
    let register = |token: &str, body: Value| {
        let (app, token) = (app.clone(), token.to_string());
        async move { call(&app, Method::POST, "/clients", Some(&token), Some(body)).await }
    };
    let client = |nif: &str, name: &str, email: &str| {
        json!({
            "nif": nif,
            "name": name,
            "manager": { "name": "Rui Gestor", "email": email },
        })
    };
    let textil = client("500000069", " Têxteis do Ave, Lda. ", "rui@textil.test");

    for email in [OPERADOR, TECNICO, GESTOR_CLIENTE] {
        let token = login(&app, email).await;
        let (status, _) = register(&token, textil.clone()).await;
        assert_eq!(status, StatusCode::FORBIDDEN);
    }

    let (status, created) = register(&assistencia, textil.clone()).await;
    assert_eq!(status, StatusCode::CREATED, "{created}");
    let organization = &created["organization"];
    let client_id = organization["id"].as_str().unwrap().to_string();
    assert_eq!(organization["name"], "Têxteis do Ave, Lda.");
    assert_eq!(organization["kind"], "cliente");
    assert_eq!(organization["nif"], "500000069");
    let manager = &created["manager"]["user"];
    assert_eq!(manager["role"], "gestor");
    assert_eq!(manager["organization_id"], client_id.as_str());
    assert_eq!(manager["must_change_password"], true);
    assert_eq!(
        created["manager"]["temporary_password"]
            .as_str()
            .unwrap()
            .len(),
        12
    );

    // a retry only finds the link that's already there
    let (status, again) = register(&assistencia, textil.clone()).await;
    assert_eq!(status, StatusCode::OK, "{again}");
    assert_eq!(again["organization"]["id"], client_id.as_str());
    assert_eq!(again["manager"], Value::Null);

    // a second manufacturer selling to the same client gets linked to it, not a copy of it,
    // and its own name for the client doesn't overwrite the registered one
    let (status, linked) = register(
        &atlantico,
        client("500000069", "Têxteis Ave", "outro@textil.test"),
    )
    .await;
    assert_eq!(status, StatusCode::OK, "{linked}");
    assert_eq!(linked["organization"]["id"], client_id.as_str());
    assert_eq!(linked["organization"]["name"], "Têxteis do Ave, Lda.");
    assert_eq!(linked["manager"], Value::Null);
    let (_, body) = call(&app, Method::GET, "/organizations", Some(&atlantico), None).await;
    assert_eq!(
        names(&body),
        [
            "Compressores Atlântico, Lda.",
            "Metalúrgica Norte, S.A.",
            "Têxteis do Ave, Lda."
        ]
    );

    // both can register their machines there
    for (token, serial_number) in [(&assistencia, "FT-0201"), (&atlantico, "CA-2101")] {
        let (status, body) = call(
            &app,
            Method::PUT,
            &format!("/machines/{}", Uuid::new_v4()),
            Some(token),
            Some(json!({ "organization_id": &client_id, "serial_number": serial_number, "model": "Tear TJ-12" })),
        )
        .await;
        assert_eq!(status, StatusCode::CREATED, "{body}");
    }

    let (status, _) = register(
        &assistencia,
        client("500000068", "Check digit errado", "a@b.test"),
    )
    .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    let (status, _) = register(&assistencia, client("500000077", "   ", "a@b.test")).await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    let (status, _) = register(
        &assistencia,
        client("500000077", "Sem gestor", "not-an-email"),
    )
    .await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
    // Compressores Atlântico's NIF
    let (status, _) = register(&assistencia, client("500000042", "Fabricante", "a@b.test")).await;
    assert_eq!(status, StatusCode::CONFLICT);

    // a manager email that's taken rolls the whole registration back
    let (status, _) = register(&assistencia, client("500000077", "Fiação Norte", OPERADOR)).await;
    assert_eq!(status, StatusCode::CONFLICT);
    let (status, body) = register(
        &assistencia,
        client("500000077", "Fiação Norte", "gestor@fiacao.test"),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{body}");
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn new_accounts_change_their_temporary_password(pool: PgPool) {
    let app = app(&pool).await;
    let gestor = login(&app, GESTOR_CLIENTE).await;
    let (status, created) = call(
        &app,
        Method::POST,
        "/users",
        Some(&gestor),
        Some(json!({ "name": "Sara Operadora", "email": "sara@fixtag.test", "role": "operador" })),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED, "{created}");
    let temporary = created["temporary_password"].as_str().unwrap().to_string();

    let (status, session) = call(
        &app,
        Method::POST,
        "/login",
        None,
        Some(json!({ "email": "sara@fixtag.test", "password": &temporary })),
    )
    .await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(session["user"]["must_change_password"], true);
    let token = session["access_token"].as_str().unwrap();

    // nothing else works until the password is changed
    let (status, body) = call(&app, Method::GET, "/machines", Some(token), None).await;
    assert_eq!(status, StatusCode::FORBIDDEN);
    assert!(body["error"].as_str().unwrap().contains("/me/password"));

    let change =
        |current: &str, new: &str| json!({ "current_password": current, "new_password": new });
    for invalid in [
        change("wrong-password", "um-segredo-novo"),
        change(&temporary, "curta"),
        change(&temporary, &temporary),
    ] {
        let (status, _) = call(
            &app,
            Method::POST,
            "/me/password",
            Some(token),
            Some(invalid),
        )
        .await;
        assert_eq!(status, StatusCode::BAD_REQUEST);
    }

    let (status, changed) = call(
        &app,
        Method::POST,
        "/me/password",
        Some(token),
        Some(change(&temporary, "um-segredo-novo")),
    )
    .await;
    assert_eq!(status, StatusCode::OK, "{changed}");
    assert_eq!(changed["user"]["must_change_password"], false);
    let new_token = changed["access_token"].as_str().unwrap();
    assert_eq!(count(&app, new_token, "/machines").await, 8);

    // the old refresh token was revoked, and only the new password logs in
    let (status, _) = call(
        &app,
        Method::POST,
        "/refresh",
        None,
        Some(json!({ "refresh_token": session["refresh_token"] })),
    )
    .await;
    assert_eq!(status, StatusCode::UNAUTHORIZED);
    for (password, expected) in [
        (temporary.as_str(), StatusCode::UNAUTHORIZED),
        ("um-segredo-novo", StatusCode::OK),
    ] {
        let (status, _) = call(
            &app,
            Method::POST,
            "/login",
            None,
            Some(json!({ "email": "sara@fixtag.test", "password": password })),
        )
        .await;
        assert_eq!(status, expected);
    }
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn accounts_are_created_inside_the_organization(pool: PgPool) {
    let app = app(&pool).await;
    let create = |token: &str, name: &str, email: &str, role: &str| {
        let (app, token) = (app.clone(), token.to_string());
        let body = json!({ "name": name, "email": email, "role": role });
        async move {
            call(&app, Method::POST, "/users", Some(&token), Some(body))
                .await
                .0
        }
    };
    let gestor = login(&app, GESTOR).await;
    let assistencia = login(&app, ASSISTENCIA).await;
    let gestor_cliente = login(&app, GESTOR_CLIENTE).await;
    let operador = login(&app, OPERADOR).await;

    // a gestor creates any role its organization has, support staff only technicians
    let cases = [
        (&gestor, "assistencia", StatusCode::CREATED),
        (&gestor, "tecnico", StatusCode::CREATED),
        (&gestor, "gestor", StatusCode::CREATED),
        (&gestor, "operador", StatusCode::BAD_REQUEST),
        (&assistencia, "tecnico", StatusCode::CREATED),
        (&assistencia, "assistencia", StatusCode::FORBIDDEN),
        (&gestor_cliente, "operador", StatusCode::CREATED),
        (&gestor_cliente, "tecnico", StatusCode::BAD_REQUEST),
        (&operador, "operador", StatusCode::FORBIDDEN),
    ];
    for (i, (token, role, expected)) in cases.into_iter().enumerate() {
        let email = format!("pessoa{i}@fixtag.test");
        assert_eq!(
            create(token, "Pessoa", &email, role).await,
            expected,
            "{role}"
        );
    }

    assert_eq!(
        create(&gestor, "Pessoa", "Pessoa0@FixTag.test", "tecnico").await,
        StatusCode::CONFLICT
    );
    assert_eq!(
        create(&gestor, "Pessoa", "sem-arroba", "tecnico").await,
        StatusCode::BAD_REQUEST
    );
    assert_eq!(
        create(&gestor, "  ", "nome@fixtag.test", "tecnico").await,
        StatusCode::BAD_REQUEST
    );

    // the database refuses the wrong role for the kind of organization too
    let result = sqlx::query(
        "INSERT INTO users (organization_id, organization_kind, name, email, password_hash, role)
         VALUES ($1, 'cliente', 'Interno', 'interno@fixtag.test', 'x', 'tecnico')",
    )
    .bind(Uuid::parse_str(CLIENTE_DEMO).unwrap())
    .execute(&pool)
    .await;
    assert!(result.is_err());

    // everyone sees their own organization's people, and the new technicians can be assigned
    let (_, body) = call(&app, Method::GET, "/users", Some(&operador), None).await;
    assert_eq!(
        names(&body),
        [
            "Ana Operadora",
            "Diogo Operador",
            "Isabel Gestora",
            "Pessoa"
        ]
    );
    assert_eq!(count(&app, &assistencia, "/technicians").await, 5);
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn organizations_are_scoped(pool: PgPool) {
    let app = app(&pool).await;

    // a client sees itself and the manufacturers it buys from
    let operador = login(&app, OPERADOR).await;
    let (status, body) = call(&app, Method::GET, "/organizations", Some(&operador), None).await;
    assert_eq!(status, StatusCode::OK);
    assert_eq!(
        names(&body),
        ["Cliente Demo, Lda.", "Fabricante Demo, S.A."]
    );
    assert_eq!(body[0]["kind"], "cliente");
    assert_eq!(body[0]["nif"], "500000018");
    let norte = login(&app, OPERADOR_NORTE).await;
    let (_, body) = call(&app, Method::GET, "/organizations", Some(&norte), None).await;
    assert_eq!(
        names(&body),
        [
            "Compressores Atlântico, Lda.",
            "Fabricante Demo, S.A.",
            "Metalúrgica Norte, S.A."
        ]
    );

    // a manufacturer sees itself and the clients it serves
    for email in [TECNICO, ASSISTENCIA] {
        let token = login(&app, email).await;
        let (_, body) = call(&app, Method::GET, "/organizations", Some(&token), None).await;
        assert_eq!(
            names(&body),
            [
                "Cliente Demo, Lda.",
                "Fabricante Demo, S.A.",
                "Metalúrgica Norte, S.A.",
                "Outro Cliente"
            ]
        );
    }
}

#[sqlx::test(migrator = "fixtag_api::MIGRATOR")]
async fn organizations_are_renamed_by_their_gestor_or_manufacturer(pool: PgPool) {
    let app = app(&pool).await;
    let rename = |email: &'static str, id: &'static str, name: &'static str| {
        let app = app.clone();
        async move {
            let token = login(&app, email).await;
            let uri = format!("/organizations/{id}");
            let body = json!({ "name": name });
            call(&app, Method::PUT, &uri, Some(&token), Some(body)).await
        }
    };

    let (status, body) = rename(GESTOR_CLIENTE, CLIENTE_DEMO, " Cliente Demo, S.A. ").await;
    assert_eq!(status, StatusCode::OK, "{body}");
    assert_eq!(body["name"], "Cliente Demo, S.A.");
    let (status, _) = rename(ASSISTENCIA, METALURGICA, "Metalúrgica do Norte, S.A.").await;
    assert_eq!(status, StatusCode::OK);
    let (status, _) = rename(GESTOR, FABRICANTE, "Fabricante Demo, Lda.").await;
    assert_eq!(status, StatusCode::OK);

    let cases = [
        (OPERADOR, CLIENTE_DEMO, StatusCode::FORBIDDEN),
        (ASSISTENCIA, FABRICANTE, StatusCode::FORBIDDEN),
        (GESTOR_CLIENTE, FABRICANTE, StatusCode::FORBIDDEN),
        (ASSISTENCIA_ATLANTICO, CLIENTE_DEMO, StatusCode::NOT_FOUND),
        (ASSISTENCIA, ATLANTICO, StatusCode::NOT_FOUND),
    ];
    for (email, id, expected) in cases {
        let (status, _) = rename(email, id, "Outro nome").await;
        assert_eq!(status, expected, "{email} renaming {id}");
    }
    let (status, _) = rename(GESTOR_CLIENTE, CLIENTE_DEMO, "   ").await;
    assert_eq!(status, StatusCode::BAD_REQUEST);
}
