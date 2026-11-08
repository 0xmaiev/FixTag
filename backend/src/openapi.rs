use utoipa::{
    Modify,OpenApi,
    openapi::security::{HttpAuthScheme,HttpBuilder,SecurityScheme},
};
#[derive(OpenApi)]
#[openapi(
    info(
        title = "FixTag API",
        description = "Incident reporting for industrial machines. Clients own machines made by \
                       one or more manufacturers,and each manufacturer's staff only see the \
                       machines it made and their incidents. Errors are returned as \
                       `{\"error\": \"...\"}` with a 4xx/5xx status.",
    ),
    paths(
        crate::routes::health::health,
        crate::routes::login::login,
        crate::routes::refresh::refresh,
        crate::routes::devices::register_token,
        crate::routes::organizations::fetch_organizations,
        crate::routes::organizations::register_client,
        crate::routes::organizations::rename_organization,
        crate::routes::users::fetch_users,
        crate::routes::users::create_user,
        crate::routes::users::change_password,
        crate::routes::technicians::fetch_technicians,
        crate::routes::machines::fetch_machines,
        crate::routes::machines::create_machine,
        crate::routes::incidents::fetch_incidents,
        crate::routes::incidents::fetch_incident_by_id,
        crate::routes::incidents::create_incident,
        crate::routes::incidents::update_incident,
        crate::routes::incidents::add_attachment,
        crate::routes::attachments::fetch_attachment,
    ),
    modifiers(&BearerAuth),
)]
pub struct ApiDoc;

struct BearerAuth;

impl Modify for BearerAuth {
    fn modify(&self,openapi: &mut utoipa::openapi::OpenApi) {
        openapi
            .components
            .get_or_insert_with(Default::default)
            .add_security_scheme(
                "bearer",
                SecurityScheme::Http(
                    HttpBuilder::new()
                        .scheme(HttpAuthScheme::Bearer)
                        .bearer_format("JWT")
                        .build(),
                ),
            );
    }
}

/// Raw file bytes,only used to document binary bodies.
#[derive(utoipa::ToSchema)]
#[schema(value_type = String,format = Binary)]
pub struct Binary(#[allow(dead_code)] Vec<u8>);
