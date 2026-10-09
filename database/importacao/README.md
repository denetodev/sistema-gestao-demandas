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
