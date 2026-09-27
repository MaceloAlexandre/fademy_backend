-- Instituições, categorias e usuários (SPEC RN01–RN04, RN09).

create table instituicao (
    id        uuid primary key,
    nome      text        not null,
    slug      text        not null unique,
    logo_url  text,
    ativa     boolean     not null default true,
    criada_em timestamptz not null
);

create table categoria (
    id   uuid primary key,
    nome text not null,
    slug text not null unique,
    tipo text not null check (tipo in ('content', 'area'))
);

create table usuario (
    id             uuid primary key,
    instituicao_id uuid        not null references instituicao (id),
    nome           text        not null,
    email          text        not null,
    senha_hash     text        not null,
    avatar_url     text,
    papel          text        not null check (papel in ('admin', 'coordenador', 'professor', 'funcionario', 'aluno')),
    privado        boolean     not null default false,
    ativo          boolean     not null default true,
    criado_em      timestamptz not null
);

-- E-mail único no sistema todo, sem diferenciar maiúsculas (RN09).
create unique index usuario_email_unico on usuario (lower(email));
