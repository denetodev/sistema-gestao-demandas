# CLAUDE.md

SGP Aristocrata, Plataforma de Gestão da Produção Criativa (sigla técnica interna: SGD) da Aristocrata no Banco do Brasil. Monorepo: `backend/`, `frontend/`, `database/`, `docs/`.
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
- **Autocadastro** fica AGUARDANDO (conta sem `aprovado_em`) até ADMIN ou GESTOR aprovar ou rejeitar. REJEITADO é terminal. A identidade para ligar conta e cadastro importado é a **chave C** (matrícula: `C` de contratado ou `F` de funcionário do Banco, mais 7 dígitos, sempre maiúscula, única em `pessoa.chave_c`). Chave C de pessoa importada (sem login): a conta nasce pendente, sem histórico e sem permissão, com `vinculo_sugerido_id`; só ADMIN ou GESTOR confirma (`POST /pessoas/{id}/confirmar-vinculo`) e então o login passa para o cadastro importado. Chave C de quem já tem login é recusada com mensagem neutra. O cadastro exige e-mail confirmado (checado em `auth.users`) e tem limite de 5 envios por hora por conta. A conta nova que ficou pendente é ligada à pessoa importada por `POST /pessoas/{pendente}/vincular-a` (tela de Pessoas): a conta vai para a pessoa existente, que mantém o histórico, e o cadastro pendente duplicado é removido. Gestor não vincula a pessoa ADMIN.
- **Equipamentos (módulo):** entra e vê tudo quem é Admin ou Gestor; Profissional só entra com papel em `pessoa.papel_equipamentos` (OPERADOR_DE_ESTUDIO ou GESTOR_DE_EQUIPAMENTOS), e sem papel nem consulta. Não há acesso por equipe nem tela de somente leitura. Visualizador nunca entra e não recebe papel; Gestor não altera o papel de Admin. O Operador registra o empréstimo (com o OK do termo na tela), entrega e recebe a devolução; Gestor de equipamentos, Gestor e Admin autorizam ou recusam e editam o inventário. **Todo empréstimo passa por autorização** (dentro ou fora do prédio), o externo aparece em destaque, e quem registrou não autoriza o próprio pedido, exceto Admin. Atrasado é calculado: retirado e já passou da devolução prevista. **Reserva por período:** o empréstimo tem retirada prevista (vazio é agora) e devolução prevista, e o equipamento só é concedido se couber em TODO o período, sem contar o próprio pedido (individual: um por vez; lote: o pico de unidades ao mesmo tempo, não a soma). Fim de um uso e começo de outro no mesmo instante não conflitam. Retirado e vencido não tem fim e bloqueia qualquer reserva futura daquele equipamento. Editar, prorrogar e entregar conferem conflito de novo; entregar também confere que o equipamento está fisicamente aqui (o empréstimo anterior pode estar atrasado) e a entrega adiantada não invade reserva de outra pessoa. A conta está em `DisponibilidadePeriodo`. "Disponível agora" no inventário ignora reservas futuras. Regras em `EquipamentoPermissao` e `EmprestimoService`.
- **Valor da demanda = soma das peças** (`valor = quantidade x valor_unitario`, congelado do `TipoPeca` no lançamento). `demanda.valor` só existe nas importadas.
- **Dashboard de valor gerado** respeita o escopo do perfil e ignora demandas canceladas.
- **Relatório mensal** é gerado e aprovado pela própria pessoa e trava depois. Só GESTOR ou ADMIN reabrem um relatório aprovado.
- **Alterar uma demanda** (editar, mudar status, lançar atividade ou peça nela): só quem participa dela (participante ativo), a Referência da área de alguém do time ativo, Gestor e Admin. Visualizador nunca. Decidido em `PermissaoService.podeAlterarDemanda`.
- **Hierarquia (nomes na tela):** **Diretoria** (UGR, CRM, URI, DINED, COMIN...; tabela `diretoria`) > **Núcleo** (House-CRM, Multimídia...; tabela `nucleo`) > **Equipe** (a `area`: Design, HTML, Videomaker, Motion...; `area.nucleo_id`) > **Cargo** (Especialista HTML, Designer...). A pessoa está numa equipe, a equipe num núcleo, o núcleo numa diretoria.
- **Escopos de listagem de demandas:** MINHAS (padrão), EQUIPE (demandas com alguém ativo da minha equipe no time; a equipe é a área que a pessoa referencia ou, senão, a própria), NUCLEO (alguém ativo de uma equipe do meu núcleo no time; exige a equipe ligada a um núcleo), DIRETORIA (demandas da minha diretoria), TODAS (só Gestor e Admin). Minhas, Minha equipe, Meu núcleo e Minha diretoria são abertos a todo vinculado (leitura). A lista aceita busca (título ou código, sem diferenciar maiúsculas e acentos), status, diretoria, núcleo e equipe, que só estreitam o escopo; padrão 10 por página, no máximo 100, mais recentes primeiro. Cancelar uma demanda: GESTOR, ADMIN ou Responsável principal.
- **Segurança vive no backend.** O frontend nunca é a única barreira. `GET /areas` e `GET /cargos` são os únicos endpoints abertos a conta autenticada ainda não vinculada (a tela de completar cadastro precisa deles). `GET /health` é o único endpoint público (healthcheck do Railway).
- **Quem escreve o quê (barreira em `SecurityConfig`, testada em `MatrizDePermissoesTest`):** VISUALIZADOR nunca escreve (só edita o próprio perfil). Cadastros base (diretorias, núcleos, áreas, cargos, tipos de atividade, demandantes) só ADMIN e GESTOR. **Projetos e campanhas: criam e editam ADMIN, GESTOR e a Referência de equipe (Profissional com `referencia_area_id`); Profissional comum e Visualizador não** (regra em `PermissaoService.podeGerenciarProjetosECampanhas`, conferida no serviço); só ADMIN e GESTOR excluem ou encerram. Regras por item (dono, Referência de equipe, Responsável principal) ficam nos serviços. **Todo endpoint ou regra de acesso novo entra na matriz de permissões.**
- Linhas importadas têm `source_system = 'planilha-house-crm'` (COE/CRM, 472 demandas) ou `'planilha-multimidia-ugr'` (UGR, 607 demandas). São dados reais: nunca apague nem altere essas linhas em testes.

## Banco de dados

**Toda mudança de schema precisa de um arquivo `database/migrations/V{n}__descricao.sql` novo, aplicado também no Supabase** e documentado no README. O Hibernate roda com `ddl-auto: validate`: se entidade e migration divergirem, o backend não sobe. Nunca edite uma migration já aplicada.

## Convenções de trabalho

- Uma branch por tarefa (`feat/...`, `fix/...`, `docs/...`), commits pequenos em Conventional Commits (`feat(backend): ...`, `fix(frontend): ...`, `docs: ...`), PR para a `main`. Nunca push direto na `main`, nunca force push.
- Todo bug corrigido ganha teste que cobre o caso.
- Dados de teste criados em ambiente real levam o prefixo `[TESTE]` e só são apagados por quem os criou, com confirmação.
- Decisões de produto, de domínio, deploy (domínios, CORS) e qualquer coisa irreversível ou que apague dado real são do Neto: pergunte antes.
