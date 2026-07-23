-- Persisted users + federated OIDC login (Authentik) on H2 (local dev + tests). Mirrors Hibernate
-- 7's H2 mapping so `ddl-auto=validate` passes. Differs from the Postgres version in the `role`
-- column: H2 maps a STRING enum to a native ENUM type (Postgres uses varchar + CHECK).

create table app_user (
    id            uuid                     not null,
    username      varchar(255)             not null,
    password_hash varchar(255),
    role          enum ('ADMIN', 'OWNER', 'USER') not null,
    enabled       boolean                  not null,
    created_at    timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_app_user_username unique (username)
);

-- Single-use CSRF state for the authorization-code flow (10-min TTL, evicted hourly).
create table oauth_state (
    state     varchar(64)              not null,
    issued_at timestamp(6) with time zone not null,
    primary key (state)
);

-- Links an external provider identity to a local user. Email lives here, not on app_user.
create table oauth_identity (
    id               uuid                     not null,
    user_id          uuid                     not null,
    provider         varchar(32)              not null,
    provider_subject varchar(255)             not null,
    email            varchar(255),
    created_at       timestamp(6) with time zone not null,
    primary key (id),
    constraint uq_oauth_identity unique (provider, provider_subject),
    constraint fk_oauth_identity_user foreign key (user_id) references app_user (id)
);
create index idx_oauth_identity_user on oauth_identity (user_id);
