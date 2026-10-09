# Deploy: Railway (backend) e Vercel (frontend)

O banco e o Storage continuam no Supabase. Faça nesta ordem: **Railway → Vercel → fechar CORS e Supabase Auth → teste**.

## 1. Railway (backend)

1. Entre em railway.com com a conta do GitHub, **New Project → Deploy from GitHub repo** e escolha este repositório (autorize o acesso se pedir).
2. Abra o serviço criado, aba **Settings**:
   - **Root Directory:** `backend`
   - **Branch:** `main` (ou a branch que quer testar)
   - **Build Command:** `chmod +x mvnw && ./mvnw -DskipTests package` (o `chmod` evita "permission denied" quando o `mvnw` veio do Windows)
   - **Start Command:** `java -jar target/*.jar`
   - **Healthcheck Path:** `/health`
   - **Region:** a mais próxima do Supabase (us-west-2 fica na Califórnia / US West)
   - Se o build reclamar da versão do Java, crie a variável `NIXPACKS_JDK_VERSION=21` (builder Nixpacks) ou `RAILPACK_JDK_VERSION=21` (Railpack).
3. Aba **Variables** (veja `backend/.env.example`):

   | Variável | De onde vem |
   |---|---|
   | `DATABASE_URL` | Supabase → botão **Connect** → **Session pooler** → copie host/porta e monte `jdbc:postgresql://<host>:5432/postgres?sslmode=require` |
   | `DATABASE_USERNAME` | no mesmo lugar, parecido com `postgres.<ref-do-projeto>` |
   | `DATABASE_PASSWORD` | a senha do banco (Settings → Database; se resetar, atualize onde mais ela é usada) |
   | `SUPABASE_URL` | `https://<ref-do-projeto>.supabase.co` |
   | `SUPABASE_SERVICE_ROLE_KEY` | Supabase → Settings → API Keys → service_role (**segredo; só aqui**) |
   | `CORS_ALLOWED_ORIGINS` | deixe `http://localhost:4200` por enquanto; troque no passo 3 |

   Use o **pooler**: a conexão direta do Supabase é só IPv6 e o Railway não alcança.
4. Aba **Settings → Networking → Generate Domain**. Anote a URL (`https://algo.up.railway.app`).
5. Veja **Deployments → View logs**. Deu certo quando aparecer `Started SgdApiApplication`.
6. Teste no navegador: `<url>/health` mostra `{"status":"ok"}`, e `<url>/pessoas/me` mostra erro 401 (esperado, sem token).

Se o backend não subir, quase sempre é `DATABASE_URL`/usuário/senha errados (log: "password authentication failed" ou "connection refused") ou o Hibernate reclamando de coluna (migration não aplicada no banco).

## 2. Vercel (frontend)

1. Em `frontend/src/environments/environment.ts`, troque `apiUrl` pela URL do Railway (sem barra no final) e faça commit.
2. vercel.com com a conta do GitHub, **Add New → Project**, escolha o repositório.
3. **Root Directory:** `frontend`. Framework: Angular (detectado). Build `npm run build`. Não precisa de variáveis (a chave publishable do Supabase é pública).
4. **Deploy.** Anote a URL (`https://algo.vercel.app`). O `frontend/vercel.json` faz as rotas internas (`/demandas/123`) funcionarem ao atualizar a página.

## 3. Fechar o ciclo
- Railway → Variables: `CORS_ALLOWED_ORIGINS` = URL da Vercel, sem barra no final (várias, separadas por vírgula). O Railway reinicia sozinho.
- Supabase → Authentication → URL Configuration: **Site URL** = URL da Vercel e inclua-a em **Redirect URLs**. Sem isso, o e-mail de confirmação de conta aponta para o lugar errado.

## 4. Roteiro de teste na rede
Login → Dashboard → lista de demandas → abrir uma demanda → subir uma imagem de evidência (testa o Storage) → baixar o DOCX do relatório → criar conta nova (CPF + aprovação por um Gestor).

## Cuidados
- A `service_role` nunca vai para o frontend nem para o git.
- Cada push na branch configurada gera um novo deploy automático.
- `show-sql` fica desligado em produção; ligue com `SHOW_SQL=true` só para investigar.
