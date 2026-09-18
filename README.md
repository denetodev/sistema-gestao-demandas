# Sistema de Gestão de Demandas — Aristocrata / Banco do Brasil

Plataforma para centralizar a gestão da produção criativa da Aristocrata
(COE/CRM + UGR) no Banco do Brasil, substituindo a planilha
`DEMANDAS HOUSE CRMdf 2026` por um sistema estruturado e auditável.

Status: Fase 2 em andamento.

- `backend/` — Java + Spring Boot (Railway)
- `frontend/` — Angular (Vercel)
- `database/` — migrations, seeds e decisões de modelagem (Supabase/PostgreSQL)
- `docs/` — arquitetura, requisitos, API, relatórios

## Progresso

### Fase 1 — Fundação ✅
- Schema PostgreSQL (Supabase) com entidades do núcleo
- Esqueleto Spring Boot conectado via Session Pooler

### Fase 2 — Gestão (em andamento)
- [x] CRUD de dados mestres (Diretoria, Area, Cargo, Cliente, Projeto, Campanha)
- [x] CRUD de Pessoa (relacionamento com Area/Cargo)
- [x] Demanda — ciclo de vida completo (criar, editar, mudar status, cancelar)
- [x] Participantes de Demanda (vínculo N:N com Pessoa, com histórico de entrada/saída)
- [ ] Atividade
- [ ] Peça
- [ ] Evidência

## Stack

Angular + TypeScript · Java + Spring Boot · PostgreSQL via Supabase · Power BI · Git/GitHub

## Como trabalhamos

**Branches:** uma branch curta por milestone/tarefa (ex.: `feat/m1-nucleo`),
merge pra `main` só quando a fatia estiver validada. `main` sempre fica
num estado estável e deployável.

**Commits:** [Conventional Commits](https://www.conventionalcommits.org/)
— `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`.

**Sem Docker.** Decisão deliberada: desenvolvimento acontece numa máquina
corporativa sem permissão de instalar softwares além do que já existe.
Backend builda nativo (Maven) no Railway via Nixpacks, sem Dockerfile.

**Ambientes:** por enquanto só 1 ambiente (dev), criado junto com o
projeto Supabase. Homologação/produção separados entram quando o sistema
tiver algo real rodando (não antes).

## Setup

_(preenchido conforme backend/ e frontend/ forem criados nos próximos milestones)_

## Roadmap

M0 Fundação → M1 Banco de dados → M2 Backend/API → M3 Auth → M4 Vertical
slice Demanda → M5 Atividades/Peças/Evidências → M6 Relatórios → M7 BI →
M8 IA → M9 Integrações → M10 Deploy/CI-CD.

