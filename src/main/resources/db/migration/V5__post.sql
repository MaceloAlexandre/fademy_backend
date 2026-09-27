-- Posts e suas categorias (SPEC RN28–RN31).

create table post (
    id             uuid primary key,
    autor_id       uuid        not null references usuario (id),
    instituicao_id uuid        not null references instituicao (id),
    texto          text        not null,
    imagens        text[]      not null default '{}',
    link_url       text,
    comentarios    integer     not null default 0,
    criado_em      timestamptz not null
);

-- O id é UUIDv7 (ordenado pelo tempo): "id desc" é do mais novo para o mais antigo.
create index post_autor on post (autor_id, id desc);
create index post_instituicao on post (instituicao_id, id desc);

create table post_categoria (
    post_id      uuid not null references post (id) on delete cascade,
    categoria_id uuid not null references categoria (id),
    primary key (post_id, categoria_id)
);
