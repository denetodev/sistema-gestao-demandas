# Importação da planilha DEMANDAS HOUSE CRM

O gerador lê a aba **2026** e produz o SQL de importação. Os arquivos gerados têm dados reais de
pessoas e demandas, então ficam em `saida/`, que o git ignora.

```bash
node database/importacao/gerar-importacao.cjs "C:/caminho/DEMANDAS HOUSE CRM 2026-10.xlsx"
```

| Arquivo gerado | Para quê |
|---|---|
| `relatorio-conferencia.md` | Totais, mapeamentos e pontos de atenção. Conferir antes de rodar. |
| `99-ensaio-amostra-com-rollback.sql` | Ensaio com ~15 linhas de casos de borda; termina em `rollback` e não deixa nada no banco. |
| `00-limpar-dados-de-teste.sql` | **Uso único.** Apaga os dados de teste e preserva as contas com login. |
| `01-importar-planilha-2026.sql` | **Reprocessável.** Apaga só o que importou antes (`source_system = 'planilha-house-crm'`) e reimporta. |

## Ordem de execução (SQL Editor do Supabase)
1. Migration `V12` (colunas `nome_exibicao` e `cpf` em `pessoa`), já aplicada.
2. `00-limpar-dados-de-teste.sql`
3. `01-importar-planilha-2026.sql`
4. Comparar o `select` de conferência no fim do 01 com o `relatorio-conferencia.md`.

Os dois scripts rodam numa transação e terminam em `commit`. Para só conferir, troque o `commit` por `rollback`.

## Como cada linha vira dado do sistema
- **1 linha = 1 demanda** (`source_row` guarda o número da linha na planilha).
- A coluna **Projeto** lista tipos de peça separados por vírgula, e **Valor = QTD × soma dos preços unitários dos tipos**.
  Os preços saem das linhas que têm um tipo só; o relatório confere que a soma das peças fecha com o total da planilha.
- Cada tipo vira uma **peça** (valor unitário congelado) e cada responsável vira **participante** e ganha uma **atividade** "Produção".
- **Cliente** vira **Demandante** do tipo PESSOA, na diretoria COE/CRM.
- Mês de peças e atividades segue a coluna **Dia**.
