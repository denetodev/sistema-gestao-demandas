# Sistema de Gestão de Demandas — Aristocrata / Banco do Brasil

Plataforma para centralizar a gestão da produção criativa da Aristocrata
(COE/CRM + UGR) no Banco do Brasil, substituindo a planilha
`DEMANDAS HOUSE CRM 2026` por um sistema estruturado e auditável.

- `backend/` — Java 21 + Spring Boot 4 (hospedagem prevista: Render)
- `frontend/` — Angular 20 + PrimeNG (hospedagem prevista: Vercel)
- `database/` — migrations `V{n}`, importação da planilha e decisões de modelagem (Supabase/PostgreSQL)
- `docs/` — arquitetura, requisitos (inclui `wireframe-sgd.html`), API, relatórios

## Estado atual

Feito (backend + frontend + testes):
- Dados mestres, Pessoa (perfis ADMIN/GESTOR/PROFISSIONAL/VISUALIZADOR, aprovação de conta, referência de equipe)
- Demanda (ciclo de vida, auditoria de status, escopo "minhas/equipe/todas"), Participantes com papéis
- Atividade, Peça (valor unitário congelado), Evidência (upload de imagem JPEG/PNG no Supabase Storage, ou link externo)
- Dashboard e Relatório mensal (snapshot ao aprovar, exportação em DOCX)
- Telas de "Acesso negado" e "Conta aguardando aprovação"
- Nome de exibição da pessoa (`nome_exibicao`) e coluna `cpf` (V12)
- Importação da planilha 2026: **472 demandas, 992 peças, 855 atividades, 673 participantes** já carregados no Supabase
- CI no GitHub Actions (build + testes de backend e frontend)

Pendente:
- Vincular conta nova à pessoa já importada pelo CPF (backend/telas de cadastro)
- Pessoas ainda sem área definida: Caio, Erick, Gabriel
- Publicar backend (Render) e frontend (Vercel), testar com a API real e remover os mocks

## Como rodar

**Requisitos:** JDK 21, Node 22, projeto Supabase (Postgres + Storage). Sem Docker.

```bash
# backend (credenciais do Supabase via variáveis de ambiente / application.yml)
cd backend && ./mvnw spring-boot:run
./mvnw test

# frontend
cd frontend && npm ci && npm start
npm test -- --watch=false --browsers=ChromeHeadless
```

**Mocks no frontend:** na máquina do trabalho a rede do BB bloqueia o Supabase, então existem mocks temporários
(`MOCK_SESSAO` e arquivos de mock) para ver as telas sem API. Devem ser removidos quando o sistema rodar com a API real.

## Banco de dados

Toda mudança de schema precisa de um arquivo `database/migrations/V{n}__descricao.sql`, **aplicado também no Supabase**.
O Hibernate roda com `ddl-auto: validate` — se a entidade e a migration divergirem, o backend não sobe.

| Migration | Conteúdo |
|---|---|
| V1–V2 | Núcleo e ajustes pós-revisão |
| V3, V7, V8 | Perfil, foto e status "rejeitado" da pessoa |
| V4–V6 | Atividade, peça, evidência, demandante, referência de equipe, auditoria |
| V9 | Relatório mensal |
| V10 | RLS habilitado em todas as tabelas (a segurança vive no backend, que acessa o banco com role própria) |
| V11 | Evidência com arquivo (Storage) |
| V12 | `pessoa.nome_exibicao` e `pessoa.cpf` (único, validado) |

A importação da planilha está em [`database/importacao/`](database/importacao/README.md).

## Segurança
- **A segurança vive no backend.** O frontend só esconde botões.
- O CPF nunca é devolvido por inteiro pela API.

## Como trabalhamos

**Branches:** uma branch por tarefa (`feat/...`), commits pequenos no estilo `feat(...)`, `fix:`, `test+ci:`.
Nada é enviado direto para a `main`; o merge é feito por revisão (PR).

**Sem Docker.** O desenvolvimento acontece numa máquina corporativa sem permissão para instalar softwares.
Backend builda nativo (Maven) no Render, sem Dockerfile.

**Ambientes:** por enquanto só 1 ambiente (dev), no projeto Supabase. Homologação/produção separados entram
quando o sistema tiver algo real rodando.

## Roadmap

M0 Fundação → M1 Banco de dados → M2 Backend/API → M3 Auth → M4 Vertical slice Demanda →
M5 Atividades/Peças/Evidências → M6 Relatórios → M7 BI → M8 IA → M9 Integrações → M10 Deploy/CI-CD.
