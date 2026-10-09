# API do Dashboard

`GET /dashboard`, qualquer pessoa vinculada. A regra de quem vê o quê vive no backend (`DashboardService`).

| Parâmetro | Tipo | Observação |
|---|---|---|
| `visao` | `MINHA` \| `EQUIPE` \| `DIRETORIA` | Sem valor, escolhe pelo perfil (ver tabela abaixo) |
| `pessoaId` | UUID | Só em `EQUIPE`: abre o individual de um colega da área referenciada |
| `diretoriaId` | UUID | Só em `DIRETORIA` (Gestor/Admin). Sem valor, soma todas as diretorias |
| `porPessoa` | boolean | Só Gestor/Admin. Inclui `porPessoa[]` (produtividade individual fica escondida por padrão) |
| `mes` | `yyyy-MM` | Padrão: mês atual. O valor do ano e o do mês usam o ano deste mês |

## Quem pode o quê

| Quem | Visão padrão | Visões permitidas |
|---|---|---|
| Profissional | `MINHA` | `MINHA` |
| Referência de Equipe | `EQUIPE` | `MINHA`, `EQUIPE` (agregado ou colega da própria área) |
| Gestor / Admin | `DIRETORIA` | `MINHA`, `DIRETORIA` (qualquer diretoria), `porPessoa` |
| Visualizador | `DIRETORIA` | `DIRETORIA`, presa à própria diretoria (ignora `diretoriaId`) |

Violação devolve 403.

## Resposta

- `resumo`: pessoas no recorte, demandas por status (todos os status, inclusive zerados), demandas atrasadas, atividades no mês, valor no mês e no ano.
- `calendario`: dias com atividade nos últimos 12 meses (`data`, `quantidade`), para o mapa de calor.
- `porArea`: só na visão `DIRETORIA`.
- `porPessoa`: só com `porPessoa=true`.

## Regras de cálculo

- **Quem entra nos agregados:** Profissionais ativos e aprovados. Gestor/Admin/Visualizador não entram.
- **Valor gerado:** soma de `quantidade × valor_unitario` das peças produzidas pela pessoa, com demanda não cancelada. Cada peça tem um único "produzida por", então a soma nunca conta a mesma peça duas vezes.
- **Mês da peça:** `data_entrega`; sem ela, a data de criação do lançamento (UTC).
- **Demanda atrasada:** `data_prazo` anterior a hoje e status diferente de `CONCLUIDA`/`CANCELADA`.
- **Demandas do resumo:** em `MINHA`/`EQUIPE`, as demandas em que as pessoas participam (vínculo vigente). Em `DIRETORIA`, as demandas da diretoria.

## Valor da demanda (`demanda.valor`)

Decisão de 05/10/2026: o campo manual vale **só para linhas importadas da planilha** (`source_system` preenchido) que ainda não têm peças. O valor exposto em `valorCalculado` é a soma das peças; sem peças, cai para `valor` apenas se a demanda for importada. `POST`/`PUT /demandas` não aceitam mais `valor`, então editar uma demanda importada não apaga o valor original.
