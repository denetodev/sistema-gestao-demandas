-- database/migrations/V11__evidencia-arquivo.sql
-- Upload real de evidências: imagens (JPEG/PNG) guardadas no Supabase Storage.
--
-- - O bucket "evidencias" é privado e aceita só image/jpeg e image/png, até 5 MB.
-- - Ninguém acessa o bucket direto: o backend sobe o arquivo e entrega URL assinada de curta
--   duração (usa a service role key, que ignora RLS). storage.objects já nasce com RLS ligado
--   e sem policies para anon/authenticated.
-- - Evidência com arquivo guarda o caminho no bucket em arquivo_path; link externo continua
--   em conteudo (tipo LINK).

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('evidencias', 'evidencias', false, 5242880, array['image/jpeg', 'image/png'])
on conflict (id) do nothing;

alter table evidencia
    add column arquivo_path    text,
    add column arquivo_mime    varchar(40),
    add column arquivo_tamanho integer;

-- arquivo só em evidência do tipo IMAGEM
alter table evidencia
    add constraint evidencia_arquivo_so_imagem
    check (arquivo_path is null or tipo = 'IMAGEM');
