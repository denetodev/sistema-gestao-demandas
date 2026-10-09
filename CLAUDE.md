# CLAUDE.md

Sistema de Gestão de Demandas (SGD) da Aristocrata no Banco do Brasil. Monorepo: `backend/`, `frontend/`, `database/`, `docs/`.
Quem desenvolve é o Neto (full stack). Responda em português, informal e objetivo. Não use travessão nem hífen no meio de frases (use dois pontos, vírgula ou reticências).

## Stack

- **Backend:** Spring Boot 4.1.1, Java 21, Maven, pacotes por camada em `dev.denetodev.sgd_api`. Spring Security como OAuth2 Resource Server validando o JWT do Supabase. Sem Lombok.
- **Frontend:** Angular 20, PrimeNG (tema Aura, cor Orange), signals e `httpResource`.
- **Banco:** Supabase (projeto `sctjksvryeboakgjmnqg`), RLS ligada em todas as tabelas, Data API desligada. O backend conecta como dono do banco. Evidências no bucket privado `evidencias` (Storage).
- **Sem Docker.** Deploy previsto: Railway (backend) e Vercel (frontend).

## Comandos

```bash
# backend (a partir de backend/)
./mvnw spring-boot:run
./mvnw -B -ntp test -Dtest='!SgdApiApplicationTests'   # SgdApiApplicationTests precisa de banco real

# frontend (a partir de frontend/)
npm ci
npx ng serve                       # configuração development, API real via proxy
npx ng serve -c mock               # só no computador do trabalho (rede do BB bloqueia o Supabase)
npx ng build
npx ng test --watch=false --browsers=ChromeHeadless
```

Variáveis de ambiente do backend: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `CORS_ALLOWED_ORIGINS`.
O `.env.example` mostra os nomes, mas o Spring não lê `.env` sozinho: exporte na sessão do shell ou configure na IDE. Nunca imprima nem comite segredos.

O mock do frontend fica atrás de `environment.usarMock` (`false` por padrão; `true` só em `environment.mock.ts`). Mock e sessão falsa são carregados por `import()` dinâmico.

## Regras de domínio (não podem quebrar)

- **Referência de Equipe não é perfil:** é uma Pessoa com `referencia_area_id`. Edita e cancela atividades, peças e evidências de colegas da própria área e gerencia só os `TipoPeca` da própria área. Gestor/Admin definem e removem a referência ao aprovar ou ao editar a pessoa.
- **Perfis:** ADMIN, GESTOR, PROFISSIONAL, VISUALIZADOR. VISUALIZADOR é somente leitura e não tem relatório. GESTOR não pode conceder nem alterar ADMIN.
- **Autocadastro** fica AGUARDANDO (conta sem `aprovado_em`) até ADMIN ou GESTOR aprovar ou rejeitar. REJEITADO é terminal. CPF já existente (pessoa importada, sem login) exige vínculo manual por ADMIN ou GESTOR.
- **Valor da demanda = soma das peças** (`valor = quantidade x valor_unitario`, congelado do `TipoPeca` no lançamento). `demanda.valor` só existe nas importadas.
- **Dashboard de valor gerado** respeita o escopo do perfil e ignora demandas canceladas.
- **Relatório mensal** é gerado e aprovado pela própria pessoa e trava depois. Só GESTOR ou ADMIN reabrem um relatório aprovado.
- **Escopos de listagem de demandas:** MINHAS (padrão), EQUIPE (só Referência), DIRETORIA, TODAS (só Gestor e Admin). Cancelar uma demanda: GESTOR, ADMIN ou Responsável principal.
- **Segurança vive no backend.** O frontend nunca é a única barreira. `GET /areas` e `GET /cargos` são os únicos endpoints abertos a conta autenticada ainda não vinculada (a tela de completar cadastro precisa deles).
- O CPF nunca é devolvido por inteiro pela API (só a máscara).
- Linhas importadas têm `source_system = 'planilha-house-crm'` (COE/CRM, 472 demandas) ou `'planilha-multimidia-ugr'` (UGR, 607 demandas). São dados reais: nunca apague nem altere essas linhas em testes.

## Banco de dados

**Toda mudança de schema precisa de um arquivo `database/migrations/V{n}__descricao.sql` novo, aplicado também no Supabase** e documentado no README. O Hibernate roda com `ddl-auto: validate`: se entidade e migration divergirem, o backend não sobe. Nunca edite uma migration já aplicada.

## Convenções de trabalho

- Uma branch por tarefa (`feat/...`, `fix/...`, `docs/...`), commits pequenos em Conventional Commits (`feat(backend): ...`, `fix(frontend): ...`, `docs: ...`), PR para a `main`. Nunca push direto na `main`, nunca force push.
- Todo bug corrigido ganha teste que cobre o caso.
- Dados de teste criados em ambiente real levam o prefixo `[TESTE]` e só são apagados por quem os criou, com confirmação.
- Decisões de produto, de domínio, deploy (domínios, CORS) e qualquer coisa irreversível ou que apague dado real são do Neto: pergunte antes.
