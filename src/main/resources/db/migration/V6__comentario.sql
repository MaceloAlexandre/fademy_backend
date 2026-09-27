-- Comentários de posts (SPEC RN32, RN33).

create table comentario (
    id             uuid primary key,
    post_id        uuid        not null references post (id) on delete cascade,
    autor_id       uuid        not null references usuario (id),
    instituicao_id uuid        not null references instituicao (id),
    texto          text        not null,
    criado_em      timestamptz not null
);

create index comentario_post on comentario (post_id, id);
