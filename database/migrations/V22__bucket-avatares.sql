-- Fotos de perfil: bucket público (a imagem aparece em listas e no topo e uma tag de imagem não envia token).
-- Só o backend escreve (com a chave de serviço); os nomes dos arquivos levam um UUID, então a URL não é adivinhável.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatares', 'avatares', true, 2097152, array['image/jpeg', 'image/png'])
on conflict (id) do nothing;
