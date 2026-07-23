-- Baseline of the pre-existing schema, exactly as Hibernate (ddl-auto=update) created it in
-- Postgres: the `project` catalog and the `api_key` table. On the existing production database this
-- migration is baselined (never executed) via spring.flyway.baseline-on-migrate; fresh databases run
-- it. Column types mirror Hibernate 7's Postgres mapping so `ddl-auto=validate` passes.

create table project (
    uuid                   varchar(255) not null,
    index                  integer,
    name                   varchar(255),
    description            varchar(255),
    description_en         varchar(2000),
    description_de         varchar(2000),
    is_visible             boolean      not null,
    additional_information jsonb,
    repositories           jsonb,
    contributions          jsonb,
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
