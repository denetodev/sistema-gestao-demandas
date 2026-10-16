# Guia de operação do SGD

Para quem administra o sistema (Admin e Gestor): tarefas do dia a dia e o que fazer quando algo dá errado.
Não tem senhas nem chaves: elas ficam no painel de cada serviço.

## Onde as coisas estão

| O quê | Onde | Endereço |
|---|---|---|
| Sistema (frontend) | Vercel, projeto `sistema-gestao-demandas` | `https://sistema-gestao-demandas-eta.vercel.app` |
| API (backend) | Railway, projeto `observant-essence` | `https://sistema-gestao-demandas-production-7ad0.up.railway.app` |
| Banco, login e arquivos | Supabase, projeto `sctjksvryeboakgjmnqg` | painel do Supabase |
| Código | GitHub | `denetodev/sistema-gestao-demandas` |

**Use sempre o endereço `-eta` da Vercel.** Os endereços com um código no meio (por exemplo `...-66iot9yc6-...`) são de um
deploy antigo e congelado: apontam para uma versão que já não conversa com a API.

## Tarefas de quem administra

### Aprovar um cadastro novo
1. Pessoas, aba **Aguardando aprovação**, abra o cadastro.
2. Confira área e cargo, escolha o **perfil** e aprove. Para rejeitar, use Rejeitar (não tem volta: a pessoa não pode ser aprovada depois).
3. Só Admin concede ou altera o perfil Admin. O Gestor não mexe em Admin.

### A pessoa já existe no sistema (por exemplo, veio da planilha) e criou uma conta nova
Não aprove: isso cria uma **duplicata** sem o histórico.
1. Abra o cadastro pendente da conta nova.
2. No bloco **Esta pessoa já existe no sistema?**, escolha o cadastro que já existe e clique em **Vincular a este cadastro**.
3. A conta passa a ser dessa pessoa, com todo o histórico; o cadastro pendente é removido (não dá para desfazer). Chave C e e-mail
   informados no cadastro entram na pessoa se ela ainda não os tinha.

### Definir a Referência de equipe
Pessoas, editar a pessoa, campo **Referência de equipe da área** (também aparece na aprovação). Referência não é um perfil:
é um Profissional que referencia a própria área e edita o trabalho dos colegas dela. Para tirar, deixe o campo vazio.

### Colocar alguém numa demanda
Só quem participa de uma demanda (e a Referência da área de alguém do time, o Gestor e o Admin) altera algo nela.
- A própria pessoa usa **Participar desta demanda**, no topo da demanda.
- Ou um participante, Gestor ou Admin adiciona em **Participantes**.
- Cancelar uma demanda é só do Gestor, do Admin ou do Responsável principal.

### Campanhas e projetos
- Qualquer profissional cria e edita campanha; só Gestor e Admin excluem.
- Projetos e os demais cadastros base (diretorias, áreas, cargos, tipos de atividade, demandantes) são só de Gestor e Admin.
- A página da campanha mostra quantas pessoas e demandas ela reuniu e o valor gerado, sem as demandas canceladas.

### Relatório mensal
A pessoa gera e aprova o próprio relatório, e depois ele fica travado. Só Gestor ou Admin reabrem.

## Equipamentos (empréstimo do estúdio)

**Quem entra no módulo.** Admin e Gestor veem e fazem tudo. Profissional só entra com um papel dado em *Pessoas* (campo "Papel nos equipamentos"): sem papel, nem para consultar. Visualizador nunca entra e não recebe papel. Para um Admin ou Gestor que também queira ser Operador ou Gestor de equipamentos, o papel não é necessário.

**Papéis.**
- *Operador de estúdio*: registra o empréstimo (com o OK do termo), entrega, recebe a devolução, corrige pedido que espera autorização, prorroga e anexa fotos.
- *Gestor de equipamentos* (e Gestor e Admin): autoriza ou recusa, edita o inventário e o texto do termo. Quem registrou um pedido não o autoriza, exceto o Admin.

**Caminho de um empréstimo.** Aguardando autorização, autorizado, retirado (com a pessoa), devolvido. Pode terminar recusado ou cancelado. Todo empréstimo passa por autorização, dentro ou fora do prédio. Externo (sai do prédio) e atrasado ficam em vermelho e vão para o topo. "Atrasado" não é um status: é retirado com a devolução prevista já vencida. O selo no menu mostra pedidos esperando e atrasados e se atualiza a cada 2 minutos.

**Reserva e agenda.** O pedido tem retirada prevista (em branco é agora) e devolução prevista, e a lista de equipamentos do pedido mostra só o que está livre em todo esse período. Um equipamento que volta amanhã pode ser reservado para a semana que vem; dois pedidos nunca ocupam a mesma peça ao mesmo tempo (lote: nunca passam da quantidade total). Devolver às 12h e outra pessoa retirar às 12h não é conflito. Equipamento retirado e vencido continua bloqueado até ser devolvido. Ao entregar, o sistema confere que o equipamento voltou do empréstimo anterior e não deixa uma retirada adiantada invadir a reserva de outra pessoa. A tela *Agenda* mostra tudo isso em um Gantt: uma linha por equipamento, uma barra por período, a linha vermelha é o agora; clicar no nome mostra com quem está.

**Dia a dia.**
- Prorrogar e corrigir a data também conferem conflito: não dá para estender por cima da reserva de outra pessoa.
- Trocar os equipamentos de um pedido: cancele e registre de novo (o OK do termo vale para aquela lista). Dados do pedido (pessoa, data, local, demanda) podem ser corrigidos enquanto ele espera autorização.
- Quem já está com o equipamento pode ter a devolução prorrogada; o motivo e quem prorrogou ficam na auditoria.
- Equipamento não se apaga: é desativado, e só quando não está em empréstimo em aberto.
- O termo é versionado. Mudar o texto cria uma versão nova e os empréstimos antigos guardam a versão aceita.
- Fotos de saída e devolução são opcionais: JPEG ou PNG, até 5 MB, no máximo 6 por etapa, no bucket privado (pasta `emprestimos/`).

**Consultas úteis no banco** (somente leitura):
```sql
-- quem está com quê e se está atrasado
select e.retirante_nome, q.nome as equipamento, e.prevista_devolucao, e.prevista_devolucao < now() as atrasado
from emprestimo e
join emprestimo_item i on i.emprestimo_id = e.id and i.devolvido_em is null
join equipamento q on q.id = i.equipamento_id
where e.status = 'RETIRADO'
order by e.prevista_devolucao;

-- trilha de um empréstimo (mudanças de situação, correções e prorrogações)
select campo, valor_anterior, valor_novo, motivo, created_at
from auditoria where tabela = 'emprestimo' and registro_id = '<id do empréstimo>' order by created_at;
```

**Dados de teste.** Empréstimos de teste em ambiente real levam `[TESTE]` na finalidade e só são apagados por quem os criou, com confirmação.

## Quando algo dá errado

Comece pelo mais barato: abra `https://sistema-gestao-demandas-production-7ad0.up.railway.app/health`.
Se responder `{"status":"UP"}`, a API está de pé.

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| "Não foi possível conectar ao servidor" | Você abriu um endereço antigo da Vercel, ou há um deploy em andamento | Abra o endereço `-eta`, dê **Ctrl+Shift+R**, espere uns 3 minutos se acabou de haver merge |
| `/health` não responde | API caída | Railway, serviço, aba Deployments e Logs. Veja se o último deploy falhou |
| `/health` responde, mas o sistema não carrega dados | Banco indisponível | Veja o Supabase (projeto pausado?) e os logs do Railway |
| Login volta para a tela errada | URL do site não está no Supabase | Supabase, Authentication, URL Configuration: Site URL e Redirect URLs com o endereço `-eta` |
| Erro de CORS no console (F12) | O endereço do site não está liberado na API | Railway, variável `CORS_ALLOWED_ORIGINS` com o endereço `-eta`, sem barra no final |
| 404 em todas as páginas da Vercel | Configuração de build do projeto | Vercel, Settings: Root Directory `frontend`; o resto vem do `frontend/vercel.json` |
| 403 ao editar algo | A pessoa não participa da demanda, ou o perfil não permite | Veja "Colocar alguém numa demanda" |

## Deploy

- **Merge na `main` publica sozinho**, no Railway (API) e na Vercel (site). Leva de 2 a 5 minutos. Vários merges seguidos geram
  várias janelas curtas de instabilidade: espere o último terminar antes de testar.
- O Railway só troca de versão depois que `/health` responde; se a nova versão não sobe, a anterior continua no ar.
- O CI (GitHub Actions) roda build e testes de backend e frontend em todo PR.
- Mudança de banco precisa de um arquivo `database/migrations/V{n}__descricao.sql`, **aplicado também no Supabase**, antes de
  publicar o código que depende dela (o backend não sobe se a entidade e o schema divergirem).

### Variáveis do backend (Railway)
`DATABASE_URL` (use o endereço do **pooler** do Supabase, que funciona em IPv4), `DATABASE_USERNAME`, `DATABASE_PASSWORD`,
`SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` e `CORS_ALLOWED_ORIGINS`. Mudar uma variável no painel exige aplicar a mudança
pendente (botão Deploy) para valer.

## Importação das planilhas

As planilhas House CRM e Multimídia UGR viram demandas pelos geradores em `database/importacao/`. O passo a passo, as conferências
e o ensaio com rollback estão em `database/importacao/README.md`. Pontos que importam:
- Reimportar apaga e recria só o que foi importado antes (`source_system` da planilha). Nada criado à mão no sistema é apagado.
- O vínculo de campanhas já vai dentro do `02-importar.sql`: reimportar devolve as campanhas.
- Sempre rode o ensaio (`03-ensaio...`) e compare com o relatório de conferência antes do `02-importar.sql`.

## Segurança e credenciais

- Nunca coloque senha, chave ou token em chat, issue, PR ou arquivo do repositório. O `.env` local é ignorado pelo git.
- Se uma credencial vazou, troque na origem e atualize onde é usada:
  1. **Senha do banco:** Supabase, Project Settings, Database, redefinir senha. Atualize `DATABASE_PASSWORD` no Railway e no `.env` local.
  2. **Chave secreta (service role):** Supabase, Project Settings, API Keys. Crie a nova, atualize `SUPABASE_SERVICE_ROLE_KEY` no Railway e no `.env`, e só então apague a antiga.
  3. **Senha de uma conta de usuário:** pela recuperação de senha do sistema ou no Supabase, Authentication.
- O frontend só carrega a chave **pública** do Supabase (`anon`/publishable), que é feita para ficar no navegador. A segurança dos dados vive
  na API: o botão escondido na tela nunca é a única barreira.

## Riscos de operação para acompanhar

- **Plano do Supabase:** projetos gratuitos são pausados após cerca de uma semana sem uso e o plano gratuito não garante backup com
  restauração. Confira o plano e a política de backup, porque o banco tem dados reais.
- **Custo do Railway:** depois do crédito inicial é cobrado por uso.
- **Contas de login:** as pessoas importadas não têm conta, e a chave C delas é preenchida à mão na tela de Pessoas. Cada uma se cadastra e o Admin vincula (veja acima).
