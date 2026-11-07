# FixTag API

Rust (Axum + sqlx) REST API backed by PostgreSQL. The full contract is in
[`docs/api/openapi.json`](../docs/api/openapi.json), and a running server also serves it at `GET /openapi.json`.

## Running it

```sh
docker compose up -d --wait db              # from the repo root, Postgres on localhost:5433
mkdir -p ~/.fixtag && cp backend/config.example.json ~/.fixtag/config.json   # set jwt_secret
cd backend
cargo run -- seed                           # migrations + demo data, safe to re-run
cargo run                                   # listens on 0.0.0.0:3000
```

Migrations run on every start. Use `-p <path>` to point at another config file. Every request
is logged, set `RUST_LOG` to change the verbosity.

While the app is in development, schema changes go straight into `0001_init.sql`, so after one the
server refuses to start until the database is recreated (this wipes its data):

```sh
docker compose down -v && docker compose up -d --wait db    # from the repo root
cd backend && cargo run -- seed
```

Two manufacturers and two clients. Fabricante Demo serves both clients, Compressores Atlântico only
Metalúrgica Norte, which ends up with machines from both:

| Organization            | Kind         | NIF         | Machines                                     |
|-------------------------|--------------|-------------|----------------------------------------------|
| Cliente Demo            | client       | `500000018` | `FT-0001` to `FT-0008`, by Fabricante Demo   |
| Metalúrgica Norte       | client       | `500000034` | `FT-0101` to `FT-0103` by Fabricante Demo, `CA-2001` and `CA-2002` by Compressores Atlântico |
| Fabricante Demo         | manufacturer | `500000026` |                                              |
| Compressores Atlântico  | manufacturer | `500000042` |                                              |

| Email                      | Name               | Role          | Organization           |
|----------------------------|--------------------|---------------|------------------------|
| `operador@fixtag.test`     | Ana Operadora      | `operador`    | Cliente Demo           |
| `operador2@fixtag.test`    | Diogo Operador     | `operador`    | Cliente Demo           |
| `gestor2@fixtag.test`      | Isabel Gestora     | `gestor`      | Cliente Demo           |
| `operador3@fixtag.test`    | Gabriela Operadora | `operador`    | Metalúrgica Norte      |
| `gestor3@fixtag.test`      | João Gestor        | `gestor`      | Metalúrgica Norte      |
| `assistencia@fixtag.test`  | Carla Assistência  | `assistencia` | Fabricante Demo        |
| `tecnico@fixtag.test`      | Bruno Técnico      | `tecnico`     | Fabricante Demo        |
| `tecnico2@fixtag.test`     | Eva Técnica        | `tecnico`     | Fabricante Demo        |
| `tecnico3@fixtag.test`     | Filipe Técnico     | `tecnico`     | Fabricante Demo        |
| `gestor@fixtag.test`       | Helena Gestora     | `gestor`      | Fabricante Demo        |
| `assistencia2@fixtag.test` | Luísa Assistência  | `assistencia` | Compressores Atlântico |
| `tecnico4@fixtag.test`     | Marco Técnico      | `tecnico`     | Compressores Atlântico |
| `gestor4@fixtag.test`      | Nuno Gestor        | `gestor`      | Compressores Atlântico |

Every password is `fixtag123`. The QR codes on the machines should encode just the serial number. The
14 seeded incidents come with their history: some wait for a technician, some are in progress, some
are resolved. The app's demo flavor (`DemoSeed.kt`) uses the same ids. New accounts are created
through the API (`POST /users`, `POST /clients`) with a temporary password; to insert one by hand,
use a hash from `cargo run -- hash-password <password>`.

### From the Android emulator

Use `http://10.0.2.2:3000`. WSL forwards Windows' localhost to the server, so this works with the
server running inside WSL. Android blocks plain HTTP by default, so allow cleartext for `10.0.2.2`
with a network security config. A physical phone on Wi-Fi needs the Windows machine's LAN IP plus
a `netsh interface portproxy` rule into WSL, or WSL's mirrored networking mode.

### Tests

```sh
cp ../.env.example ../.env    # once, points DATABASE_URL at the compose database
cargo test
```

Each test creates and drops its own database, so the dev data is never touched.

## Endpoints

All endpoints except `/`, `/login`, `/refresh` and `/openapi.json` need `Authorization: Bearer <access_token>`.
Errors are always `{"error": "..."}`.

| Method | Path                                          | What it does                                    |
|--------|-----------------------------------------------|-------------------------------------------------|
| GET    | `/`                                           | Health check (also checks the database)         |
| POST   | `/login`                                      | Email + password → tokens + user profile        |
| POST   | `/refresh`                                    | Refresh token → new tokens (rotated)            |
| POST   | `/devices`                                    | Register the FCM token for the logged in user   |
| POST   | `/me/password`                                | Change the password, the temporary one included |
| GET    | `/organizations`                              | Own organization plus the ones it's linked to   |
| PUT    | `/organizations/{id}`                         | Rename an organization                          |
| POST   | `/clients`                                    | Register a client with its gestor, or link to it by NIF |
| GET    | `/users`                                      | The people in the user's organization           |
| POST   | `/users`                                      | Create an account in the user's organization    |
| GET    | `/technicians`                                | Technicians support can assign, least busy first |
| GET    | `/machines?serial_number=`                    | Machines the user can see                       |
| PUT    | `/machines/{id}`                              | Register a machine (id generated by the app) or edit it |
| GET    | `/incidents?cursor=&limit=&status=&severity=&machine_id=&assigned_to=` | Incident list / incremental sync |
| GET    | `/incidents/{id}`                             | One incident                                    |
| PUT    | `/incidents/{id}`                             | Create (id generated by the app) or edit        |
| POST   | `/incidents/{id}/update`                      | Add a message, change the status and/or assign a technician |
| PUT    | `/incidents/{id}/attachments/{attachment_id}` | Upload one file as the raw body                 |
| GET    | `/attachments/{id}`                           | Download a file                                 |

Incidents always come with their `attachments` (metadata only) and `updates` (history, oldest first)
embedded, so one sync call gives the app everything it needs to show them.

## Sync protocol for the app

**Tokens.** The access token lasts 15 minutes. On a 401, call `/refresh` and store *both* new tokens,
because refresh tokens are single use. A refresh token stays valid for 60 seconds after being used,
so retrying after a lost response doesn't log the user out. A 401 from `/refresh` means log in again.

**Sending (WorkManager).** Every write is idempotent, so the worker can retry anything. Send in this
order, so whatever a request refers to is already on the server:

1. Support staff only: new machines with `PUT /machines/{id}` and a UUID generated by the app, so a
   machine can be registered offline. `201` means created, `200` that it already existed (a retry,
   or an edit). Clients and accounts are online only (`POST /clients`, `POST /users`): the NIF and
   the email decide who they are, so the server has to answer, and the temporary password only
   comes back in the response.
2. `PUT /incidents/{id}` with the UUID the app generated when the incident was created.
   `201` means created, `200` means it already existed (a retry, or an edit by the author).
3. Then each photo/audio file: `PUT /incidents/{id}/attachments/{attachment_id}` with the raw
   bytes and the file's `Content-Type` (`image/jpeg`, `audio/mp4`, ...). `404` means the incident
   hasn't been sent yet, so retry later. The default size limit is 25 MB (`max_upload_bytes`).
4. Messages, status changes and assignments: `POST /incidents/{id}/update` with an `id` generated
   by the app, so a retry doesn't add the same entry twice.

**Receiving.** `GET /incidents?cursor=<saved>` returns incidents ordered by last change. Keep calling
with `next_cursor` while `has_more` is true, upserting each item into Room, then save the last
`next_cursor`. The next sync starts from it and only gets incidents that changed since: new
incidents, edits, status changes, assignments, messages and attachments all count as a change. The
first sync has no cursor and gets everything. Organizations, machines and technicians are small, so
fetch those lists whole on every sync.

**Temporary passwords.** Accounts created through the API start with `must_change_password` set in
the login response. Until the user changes it at `POST /me/password`, every other endpoint answers
`403`, so send them to a change-password screen first. The response carries a new pair of tokens,
and the password change logs out every other device.

## Access rules

A client can buy machines from several manufacturers, and a manufacturer has many clients
(`client_manufacturers`). Every machine has an owner (`organization_id`, the client) and a maker
(`manufacturer_id`), and each incident goes to the machine's maker (`handled_by`).

| Role          | Organization | Sees                                                       | Can also                                       |
|---------------|--------------|------------------------------------------------------------|------------------------------------------------|
| `operador`    | client       | every machine of the client, whoever made it, and their incidents | report incidents, message, attach files |
| `gestor`      | client       | same as `operador`                                         | create accounts, rename the client             |
| `assistencia` | manufacturer | the machines it made, at every client it serves, and their incidents | change status, assign its technicians, register clients and machines, create `tecnico` accounts |
| `tecnico`     | manufacturer | its machines, but only the incidents they reported or have ever been assigned | change the status of the ones assigned to them now |
| `gestor`      | manufacturer | same as `assistencia`                                      | everything `assistencia` does, create accounts, rename the manufacturer |

- Each kind of organization has its own roles, and the database refuses any other combination.
  Accounts are always created in the creator's own organization, except a new client's first
  `gestor`, created with the client.
- A technician keeps seeing an incident after it's reassigned: incremental sync can't tell a phone
  to forget one, so access is never taken away.
- Only the author can edit an incident's description, severity and location. The machine can't change.
- Assigning doesn't change the status, send `new_status` in the same update for that. An incident
  only goes to a technician of the manufacturer handling it.
- Anyone who can see an incident can add messages and files.
- A manufacturer only registers machines at the clients it serves, and only edits its own. Serial
  numbers are unique across every machine.
- A client is identified by its NIF: when a second manufacturer registers a client that already
  exists, it's only linked to it, so the client's staff see both manufacturers' machines.
- Anything the caller can't see gets the same `404` as something that doesn't exist.

## Not done yet

- Unlinking a client from a manufacturer, for the same reason access is never taken away from
  technicians.
- Platform admins: manufacturers are only created by the seed for now.
- Sending push notifications. Devices are registered, but status changes and assignments don't send
  an FCM message yet. That needs a Firebase service account. Until then, the app picks changes up
  when it syncs.
- Picking the closest technician. `GET /technicians` sorts by open incidents, distance would need the
  app to report where each technician is.
- Attachments are stored on the local disk (`storage_dir`), behind the `Storage` trait so S3 or
  similar could be added later.
