# SGP Aristocrata: Plataforma de Gestão da Produção Criativa (Banco do Brasil)

> Nome de produto desde 13/10/2026 (antes "Sistema de Gestão de Demandas"). A sigla técnica interna continua `sgd` no repositório, no pacote `dev.denetodev.sgd_api`, nas migrations e nos serviços de deploy, de propósito: renomear isso quebraria deploy, CI, CORS e redirects do Supabase Auth sem ganho para o usuário. A entidade de domínio "Demanda" continua existindo.

Plataforma para centralizar a gestão da produção criativa da Aristocrata
(COE/CRM + UGR) no Banco do Brasil, substituindo a planilha
`DEMANDAS HOUSE CRM 2026` por um sistema estruturado e auditável.

- `backend/` — Java 21 + Spring Boot 4 (hospedagem prevista: Railway)
- `frontend/` — Angular 20 + PrimeNG (hospedagem prevista: Vercel)
- `database/` — migrations `V{n}`, importação da planilha e decisões de modelagem (Supabase/PostgreSQL)
- `docs/` — arquitetura, requisitos (inclui `wireframe-sgd.html`), API, relatórios
- Operação do dia a dia e solução de problemas: [`docs/operacao/guia-de-operacao.md`](docs/operacao/guia-de-operacao.md)

## Estado atual

Feito (backend + frontend + testes):
- Dados mestres, Pessoa (perfis ADMIN/GESTOR/PROFISSIONAL/VISUALIZADOR, aprovação e rejeição de conta, Referência de Equipe definida na aprovação ou na edição)
- Demanda (ciclo de vida, auditoria de status, escopos Minhas, Minha equipe, Minha diretoria e Todas), Participantes com papéis
- Atividade, Peça (valor unitário congelado), Evidência (upload JPEG/PNG até 5 MB no bucket privado do Supabase Storage, URL assinada de 5 min, ou link externo)
- Dashboard (mapa de calor, resumo e valor gerado por escopo, sem demandas canceladas) e Relatório mensal (snapshot ao aprovar, exportação em DOCX; só Gestor/Admin reabrem)
- Telas de "Acesso negado" e "Conta aguardando aprovação"
- Nome de exibição da pessoa (`nome_exibicao`) e chave C (V16): cadastro da conta pede nome completo + chave C (única); Gestor/Admin edita a chave C e vincula a conta a uma pessoa importada
- Importação das planilhas House CRM (472 demandas) e Multimídia UGR (607 demandas): o banco tem hoje 36 pessoas, 1.079 demandas, 1.985 atividades e 1.682 peças
- CI no GitHub Actions (build + testes de backend e frontend)
- Mock do frontend isolado atrás de `environment.usarMock` (só com `ng serve -c mock`)

Pendente:
- Projeto e Campanha estão vazios (as planilhas não os trazem): definir como popular
- Especialidade, PessoaEspecialidade e PessoaCargoHistorico existem como tabelas sem entidade JPA
- Cadastrar a chave C de quem foi importado (tela de Pessoas) para vincular com a conta de login
- Publicar backend (Railway) e frontend (Vercel) e ajustar `environment.ts` e `CORS_ALLOWED_ORIGINS`
- Testes ponta a ponta de GESTOR, VISUALIZADOR e Referência de Equipe com a API real

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

**Mocks no frontend:** na máquina do trabalho a rede do BB bloqueia o Supabase. Lá use `npx ng serve -c mock`
(sessão falsa e dados de mentira, ligados por `environment.usarMock`). Nas demais configurações o mock não é carregado.

**Variáveis do backend:** `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `SUPABASE_URL`,
`SUPABASE_SERVICE_ROLE_KEY`, `CORS_ALLOWED_ORIGINS` (veja `.env.example`). O Spring não lê `.env` sozinho:
exporte na sessão do shell ou configure na IDE.

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
| V12 | `pessoa.nome_exibicao` e `pessoa.cpf` (único, validado; o CPF foi substituído pela chave C na V16/V17) |
| V13 | `pessoa.inativado_em`: o resumo do relatório mensal só lista quem estava ativo no mês |
| V14 | `nucleo` (diretoria > núcleo > equipe) e `area.nucleo_id` |
| V15 | `pessoa.vinculo_sugerido_id`: conta nova que informou a chave C de um cadastro importado fica pendente com a sugestão; só a aprovação de Gestor/Admin passa o login para o cadastro |
| V16 | `pessoa.chave_c` (única, C ou F + 7 dígitos): substitui o CPF como identidade do vínculo. A coluna `cpf` saiu na V17 |
| V17 | remove `pessoa.cpf` (e as constraints dele) |
| V18 | módulo de equipamentos: `categoria_equipamento`, `equipamento`, `emprestimo`, `emprestimo_item`, `termo_equipamento`, `area.acessa_equipamentos` (hoje sem uso: o acesso é só por papel; a coluna sai numa V21) e `pessoa.papel_equipamentos` |
| V19 | `emprestimo_foto`: fotos opcionais do equipamento na saída e na devolução (arquivo no bucket privado, pasta emprestimos/) |
| V20 | `emprestimo.retirada_prevista`: reserva por período (equipamento livre ou ocupado no intervalo, não só agora) |
| V21 | remove `area.acessa_equipamentos` (acesso agora é por papel da pessoa) |
| V22 | bucket público `avatares` (fotos de perfil, até 2 MB, JPEG ou PNG) |
| V23 | `pessoa.notificacoes_vistas_em`, `pessoa_demanda.adicionado_por` e `adicionado_em` (sininho de avisos) |

A importação da planilha está em [`database/importacao/`](database/importacao/README.md).

## Segurança
- **A segurança vive no backend.** O frontend só esconde botões.

## Como trabalhamos

**Branches:** uma branch por tarefa (`feat/...`), commits pequenos no estilo `feat(...)`, `fix:`, `test+ci:`.
Nada é enviado direto para a `main`; o merge é feito por revisão (PR).

**Sem Docker.** O desenvolvimento acontece numa máquina corporativa sem permissão para instalar softwares.
Backend builda nativo (Maven) no Railway, sem Dockerfile.

**Ambientes:** por enquanto só 1 ambiente (dev), no projeto Supabase. Homologação/produção separados entram
quando o sistema tiver algo real rodando.

## Roadmap

M0 Fundação → M1 Banco de dados → M2 Backend/API → M3 Auth → M4 Vertical slice Demanda →
M5 Atividades/Peças/Evidências → M6 Relatórios → M7 BI → M8 IA → M9 Integrações → M10 Deploy/CI-CD.
