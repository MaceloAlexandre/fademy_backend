-- Imagens guardadas pela chave no armazenamento; a URL é montada na resposta com o
-- S3_PUBLIC_URL vigente, para as fotos não quebrarem quando o endereço muda (#53).

alter table usuario add column avatar_chave text;
update usuario set avatar_chave = substring(avatar_url from '(avatars/[^/?#]+)$');
alter table usuario drop column avatar_url;

update post
set imagens = array(select substring(url from '(posts/[^/?#]+)$')
                    from unnest(imagens) url
                    where substring(url from '(posts/[^/?#]+)$') is not null);
