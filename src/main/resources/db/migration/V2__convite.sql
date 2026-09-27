-- Convites (SPEC RN10–RN13).

create table convite (
    id               uuid primary key,
    instituicao_id   uuid        not null references instituicao (id),
    convidado_por_id uuid        not null references usuario (id),
    papel_alvo       text        not null check (papel_alvo in ('admin', 'coordenador', 'professor', 'funcionario', 'aluno')),
    token            text        not null unique,
    max_usos         integer     not null check (max_usos >= 1),
    usos             integer     not null default 0,
    status           text        not null check (status in ('active', 'revoked', 'expired')),
    expira_em        timestamptz not null,
    criado_em        timestamptz not null
);
