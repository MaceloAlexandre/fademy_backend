-- Follows e bloqueios (SPEC RN21–RN27).

create table seguir (
    id          uuid primary key,
    seguidor_id uuid        not null references usuario (id),
    seguido_id  uuid        not null references usuario (id),
    status      text        not null check (status in ('pending', 'accepted')),
    criado_em   timestamptz not null,
    unique (seguidor_id, seguido_id)
);

create index seguir_seguido on seguir (seguido_id, status);

create table bloqueio (
    id            uuid primary key,
    bloqueador_id uuid        not null references usuario (id),
    bloqueado_id  uuid        not null references usuario (id),
    criado_em     timestamptz not null,
    unique (bloqueador_id, bloqueado_id)
);

create index bloqueio_bloqueado on bloqueio (bloqueado_id);
