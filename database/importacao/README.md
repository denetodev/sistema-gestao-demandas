# Importação da planilha DEMANDAS HOUSE CRM

O gerador lê a aba **2026** e produz o SQL de importação. Os arquivos gerados têm dados reais de
pessoas e demandas, então ficam em `saida/`, que o git ignora.

```bash
node database/importacao/gerar-importacao.cjs "C:/caminho/DEMANDAS HOUSE CRM 2026-10.xlsx"
```

| Arquivo gerado | Para quê |
|---|---|
| `relatorio-conferencia.md` | Totais, mapeamentos e pontos de atenção. Conferir antes de rodar. |
| `01-carga-NN.sql` | Carrega as tabelas de apoio `imp_hc_*` (staging, com RLS ligado). Pedaços de ~45 KB para caber no SQL Editor / MCP. |
| `01-conferir-carga.sql` | Compara contagens e checksums (md5) da carga com o que o gerador calculou. **Todas as colunas devem ser `true`.** |
| `00-limpar-dados-de-teste.sql` | **Uso único.** Apaga os dados de teste e preserva as contas com login. |
| `02-importar.sql` | **Reprocessável.** Apaga só o que importou antes (`source_system = 'planilha-house-crm'`), importa e remove o staging. |
| `03-ensaio-completo-com-rollback.sql` | Limpeza + importação completas terminando em `rollback`: mostra os totais sem deixar nada no banco. |

## Ordem de execução (SQL Editor do Supabase)
1. Migration `V12` (colunas `nome_exibicao` e `cpf` em `pessoa`), já aplicada.
2. `01-carga-01.sql` … `01-carga-NN.sql`, em ordem.
3. `01-conferir-carga.sql` (tudo `true`).
4. `03-ensaio-completo-com-rollback.sql` e comparar com `relatorio-conferencia.md`.
5. `00-limpar-dados-de-teste.sql`, depois `02-importar.sql` (ambos terminam em `commit`).

Texto da planilha é normalizado para Unicode NFC (a planilha mistura acentos compostos e decompostos).

## Como cada linha vira dado do sistema
- **Linhas com o mesmo código BC são a mesma demanda.** Cada profissional lança a própria linha na planilha, sem checar
  se a demanda já existe; o gerador agrupa por código (854 linhas → 472 demandas). Linhas sem código, ou canceladas, ficam separadas
  (`source_row` guarda a primeira linha do grupo).
- **Status do grupo:** EM_ANDAMENTO > EM_APROVACAO > CONCLUIDA; linhas canceladas viram demandas à parte.
- A coluna **Projeto** lista tipos de peça separados por vírgula, e **Valor = QTD × soma dos preços unitários dos tipos**.
  Os preços saem das linhas que têm um tipo só; o relatório confere que a soma das peças fecha com o total da planilha.
- Cada tipo vira uma **peça** (valor unitário congelado) e cada responsável vira **participante** e ganha uma
  **atividade** "Produção". O responsável da linha mais antiga é o `RESPONSAVEL_PRINCIPAL`.
- **Cliente** vira **Demandante** do tipo PESSOA. Mapeamentos fixos no gerador (`DEMANDANTES`): Adriana (CRM) ≠
  Adriana dos Santos Lima (TESOU/GEASE/MERCADO); Anderson = Anderson Bezerra ("Parcinha"); Fernanda = Fernanda Parizi.
- Pessoas novas entram **sem login**; quem se cadastrar depois é vinculado pelo CPF (ver `V12`).
- Mês de peças e atividades segue a coluna **Dia**.

## Planilha Multimídia UGR (1T, 2T e 3T 2026)
"Multimídia" está para a UGR como "HOUSE" está para o CRM: nomes informais das equipes; CRM e UGR são as diretorias do BB.

```bash
node database/importacao/gerar-importacao-ugr.cjs "C:/caminho/Demandas Multimídia 26-UGR.xlsx"
```

Gera `saida-ugr/` (ignorada pelo git) com o mesmo fluxo: `01-carga-NN.sql` → `01-conferir-carga.sql` → `03-ensaio-com-rollback.sql` → `02-importar.sql`.
Diferenças em relação à HOUSE:
- **Agrupamento:** linhas com a mesma *descrição* (ex.: `20260911_Captação_BBCast_Agro`) são a mesma demanda lançada por vídeo, operação, redação etc.
- **Tabela de valores** (aba DADOS, por função) fornece o preço dos serviços; quando a planilha cobra diferente de forma consistente (ex.: Teleprompter + Painel de LED, R$ 1.300 contra R$ 900 na tabela), vale a planilha. O relatório lista as divergências.
- **Diretoria:** a demanda é da diretoria UGR; a coluna *Diretoria* da planilha é a diretoria solicitante e vai nas observações. O *Cliente* vira Demandante (por nome; a diretoria é a que mais aparece).
- **Equipe:** os 8 atuais já cadastrados + Raul (Designer, UGR). Quem saiu (Guilherme Otone, Isadora, Wagner…) entra como **INATIVO**, com o nome como está na planilha, para manter o histórico.

## Atualizar a partir de uma planilha nova (modo incremental)
A planilha House CRM continua sendo atualizada pela equipe. Reimportar tudo apagaria o que já está no banco, então existe o modo
`--delta`, que **não apaga nada**: carrega só as demandas novas e as que ganharam linhas, valor ou status.

1. Peça ao banco o estado atual (duas consultas somente leitura, no SQL Editor ou pelo MCP) e salve num JSON:
   ```sql
   -- codigos: demandas importadas da House com código BC
   select string_agg(codigo || '|' || status || '|' || coalesce(round(valor,2)::text,'') || '|' ||
          coalesce(array_length(string_to_array(replace((regexp_match(observacoes, 'linha\(s\) ([0-9, ]+)\)'))[1], ' ', ''), ','), 1), 0)::text, ';' order by codigo)
     from demanda where source_system = 'planilha-house-crm' and codigo is not null;
   -- semCodigo: identidade das demandas sem código (título + data de criação)
   select string_agg(substr(md5(titulo || '|' || data_criacao::text), 1, 8), ',')
     from demanda where source_system = 'planilha-house-crm' and codigo is null;
   ```
   Formato do arquivo: `{ "codigos": "<resultado 1>", "semCodigo": ["<hash>", "<hash>"] }` (o segundo resultado vira uma lista).
2. `node database/importacao/gerar-importacao.cjs "C:/caminho/planilha.xlsx" --delta estado-do-banco.json` gera `saida-delta/` (ignorada pelo git):
   `relatorio-delta.md` (o que é novo e o que muda), `01-carga-NN.sql` (só as demandas afetadas), `01-conferir-carga.sql`,
   `02-importar-delta.sql` e `03-ensaio-delta-com-rollback.sql`.
3. Ordem: cargas, `01-conferir-carga.sql` (tudo `true`), ensaio com rollback, `02-importar-delta.sql`.

Como casa com o que já existe: pelo **código BC** (e por ser cancelada ou não); sem código, pelo título e pela data. Demanda existente só ganha as
linhas que faltam (identidade da peça: tipo, descrição, dia e quantidade; da atividade: pessoa, descrição e dia) e tem valor, status e datas atualizados.
Rodar duas vezes não duplica. As áreas casam por nome **e pela diretoria do CRM**, porque existe "Design" também na UGR. O vínculo de campanhas vale só
para as demandas afetadas. **Linhas incompletas** (sem status, tag, projeto ou valor, em geral ainda em preenchimento) ficam de fora e aparecem no relatório.

## Inventário do estúdio
`gerar-inventario.cjs` lê a planilha "Lista de inventário Estúdio BB" (abas do estúdio BB e da Aristocrata) e gera `saida-inventario/`:
`00-apagar-dados-de-teste.sql` (uso único: apaga empréstimos, equipamentos e categorias de teste, preserva termo e papéis), `01-inventario.sql` e
`relatorio-conferencia.md`. Cada unidade física vira um equipamento ("(1/5)" a "(5/5)"), exceto cabos, baterias, carregadores e fontes, que são lote.
Nº DE BEM vira patrimônio. Unidades defeituosas citadas na observação entram inativas (a planilha não diz qual é; ajuste na tela).

## Campanhas (depois da importação)
As planilhas não trazem campanha. `vincular-campanhas.sql` cria as campanhas a partir do assunto que se repete nos títulos
(por exemplo "BBCast Agro", "Desenrola Brasil", "BBBE Livelo") e vincula as demandas importadas. É idempotente e só
preenche demanda ainda **sem** campanha, então não sobrescreve escolhas feitas no sistema. **Os dois geradores já embutem
este corpo no `02-importar.sql`** (na mesma transação), então reimportar devolve as campanhas sozinho; as campanhas em si
não são apagadas na limpeza. Rode o arquivo à parte só para aplicar sem reimportar. Na House CRM, `CAM #n.n` é a squad, não a campanha.
Para incluir uma campanha nova, acrescente uma linha na lista de regras do arquivo.
