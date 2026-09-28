-- database/migrations/V7__pessoa-foto-url.sql
-- Foto de perfil da pessoa, guardada como URL de texto (upload direto do
-- Angular para o Supabase Storage, sem passar pelo backend).
-- Aplicado via SQL Editor do Supabase, documentado aqui no mesmo lote
-- de commit que introduz o código correspondente.

alter table pessoa add column foto_url text;