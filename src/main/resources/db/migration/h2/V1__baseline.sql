-- Baseline of the pre-existing schema, exactly as Hibernate (ddl-auto=update) created it on H2
-- (local dev + tests): the `project` catalog and the `api_key` table. Mirrors Hibernate 7's H2
-- mapping so `ddl-auto=validate` passes. Differs from the Postgres baseline only in the JSON columns
-- (H2 `json` vs Postgres `jsonb`).

create table project (
    uuid                   varchar(255) not null,
    index                  integer,
    name                   varchar(255),
    description            varchar(255),
    description_en         varchar(2000),
    description_de         varchar(2000),
    is_visible             boolean      not null,
    additional_information json,
    repositories           json,
    contributions          json,
    primary key (uuid)
);

create table api_key (
    id           varchar(255)             not null,
    name         varchar(255)             not null,
    key_hash     varchar(255)             not null,
    prefix       varchar(16)              not null,
    created_at   timestamp(6) with time zone not null,
    last_used_at timestamp(6) with time zone,
    active       boolean                  not null,
    primary key (id),
    constraint uk_api_key_key_hash unique (key_hash)
);
