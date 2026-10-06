#!/usr/bin/env node
/**
 * Gera o SQL de importação da planilha "DEMANDAS HOUSE CRM" (aba 2026) para o Supabase.
 *
 *   node database/importacao/gerar-importacao.cjs "C:/caminho/DEMANDAS HOUSE CRM 2026-10.xlsx"
 *
 * Saída (pasta database/importacao/saida/, ignorada pelo git porque tem dados reais de pessoas):
 *   00-limpar-dados-de-teste.sql   uso único: apaga os dados de teste (preserva contas com login)
 *   01-importar-planilha-2026.sql  reprocessável: apaga só o que ele mesmo importou e reimporta
 *   relatorio-conferencia.md       totais, mapeamentos e anomalias para conferir ANTES de rodar
 *
 * Regras de mapeamento (ver relatorio-conferencia.md):
 *   - uma linha da planilha = uma demanda (source_row = número da linha)
 *   - a coluna "Projeto" lista tipos de peça separados por vírgula; vira uma peça por tipo, e
 *     Valor = QTD x soma dos preços unitários dos tipos (os preços saem das linhas de um tipo só)
 *   - cada responsável vira participante e ganha uma atividade "Produção"; a peça é do primeiro
 *   - Cliente vira Demandante (PESSOA) na diretoria COE/CRM
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const ARQUIVO = process.argv[2];
if (!ARQUIVO || !fs.existsSync(ARQUIVO)) {
  console.error('Uso: node gerar-importacao.cjs <planilha.xlsx>');
  process.exit(1);
}
const ABA = '2026';
const SOURCE_SYSTEM = 'planilha-house-crm';
const PRIMEIRA_LINHA_DADOS = 8;

// ---------------------------------------------------------------- configuração do domínio
const PESSOAS = {
  Carol: { nome: 'Ana Caroliny Santos de Sousa', exibicao: 'Carol', area: 'HTML', cargo: 'Especialista HTML' },
  Cícero: { nome: 'Cicero Londerly Batista Junior', exibicao: 'Cicero', area: 'Design', cargo: 'Designer' },
  Deusdete: { existente: true, nome: 'Deusdete Dias Torres Neto', exibicao: 'Neto' },
  Lucas: { nome: 'Lucas Corsino da Conceição', exibicao: 'Lucas', area: 'Design', cargo: 'Designer' },
  Thaís: { nome: 'Thais Trindade Caldas', exibicao: 'Thais', area: 'Design', cargo: 'Designer' },
};
// Novos integrantes do CRM que ainda não aparecem na aba 2026 (área e cargo a confirmar)
const PESSOAS_PENDENTES = [
  { nome: 'Caio Diniz Nobre', exibicao: 'Caio' },
  { nome: 'Erick Gabriel de Araujo Guedes', exibicao: 'Erick' },
  { nome: 'Gabriel Rodrigues Rocha', exibicao: 'Gabriel' },
];
const AREA_POR_TAG = { Designers: 'Design', Desenvolvedores: 'HTML' };
const STATUS = { Concluído: 'CONCLUIDA', 'Em Andamento': 'EM_ANDAMENTO', Aprovação: 'EM_APROVACAO', Cancelado: 'CANCELADA' };
const DIRETORIA = 'COE/CRM'; // "CRM" na planilha
const TIPO_ATIVIDADE = 'Produção';
const IDS_TESTE_SEM_LOGIN = [
  '40000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000002',
  '40000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000004',
  '40000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000006',
];

// ---------------------------------------------------------------- leitura do xlsx
/** xlsx é um zip: usa unzip se existir (Git Bash, Linux, macOS) e cai para o .NET no Windows. */
function extrair(arquivo, destino) {
  try {
    execFileSync('unzip', ['-oq', arquivo, '-d', destino]);
  } catch {
    execFileSync('powershell', ['-NoProfile', '-Command', `Add-Type -AssemblyName System.IO.Compression.FileSystem; [IO.Compression.ZipFile]::ExtractToDirectory('${arquivo.replace(/'/g, "''")}', '${destino.replace(/'/g, "''")}')`]);
  }
}

function lerAba(arquivo, nomeAba) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'xlsx-'));
  extrair(path.resolve(arquivo), dir);
  const ler = (p) => fs.readFileSync(path.join(dir, p), 'utf8');
  const dec = (s) => s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&amp;/g, '&');

  const compartilhadas = [];
  for (const m of ler('xl/sharedStrings.xml').matchAll(/<si>([\s\S]*?)<\/si>/g)) {
    compartilhadas.push(dec([...m[1].matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map((x) => x[1]).join('')));
  }
  const wb = ler('xl/workbook.xml');
  const rels = ler('xl/_rels/workbook.xml.rels');
  const aba = [...wb.matchAll(/<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"/g)].find((m) => dec(m[1]) === nomeAba);
  if (!aba) throw new Error(`Aba "${nomeAba}" não encontrada`);
  const alvo = rels.match(new RegExp(`<Relationship [^>]*Id="${aba[2]}"[^>]*Target="([^"]+)"|<Relationship [^>]*Target="([^"]+)"[^>]*Id="${aba[2]}"`));
  const xml = ler('xl/' + (alvo[1] ?? alvo[2]).replace(/^\/?(xl\/)?/, ''));

  const linhas = [];
  for (const r of xml.matchAll(/<row [^>]*r="(\d+)"[^>]*>([\s\S]*?)<\/row>/g)) {
    const n = Number(r[1]);
    if (n < PRIMEIRA_LINHA_DADOS) continue;
    const cel = { n };
    for (const c of r[2].matchAll(/<c ([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const ref = c[1].match(/r="([A-Z]+)\d+"/)[1];
      const t = (c[1].match(/ t="([^"]+)"/) || [])[1];
      const v = (c[2] || '').match(/<v>([\s\S]*?)<\/v>/);
      const inl = (c[2] || '').match(/<is>[\s\S]*?<t[^>]*>([\s\S]*?)<\/t>/);
      let valor = null;
      if (t === 's' && v) valor = compartilhadas[Number(v[1])];
      else if (inl) valor = dec(inl[1]);
      else if (v) valor = dec(v[1]);
      if (valor !== null && valor !== '') cel[ref] = valor;
    }
    linhas.push(cel);
  }
  fs.rmSync(dir, { recursive: true, force: true });
  return linhas;
}

const serial = (v) => new Date(Date.UTC(1899, 11, 30) + Number(v) * 86400000).toISOString().slice(0, 10);
const hora = (f) => {
  if (f === undefined) return null;
  const min = Math.round(Number(f) * 24 * 60) % (24 * 60);
  return `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`;
};
const arred = (n) => Math.round(n * 100) / 100;
const sql = (s) => (s === null || s === undefined ? 'null' : `'${String(s).replace(/'/g, "''")}'`);
const num = (n) => (n === null || n === undefined ? 'null' : String(n));

// ---------------------------------------------------------------- leitura e normalização das linhas
const anomalias = [];
const linhas = lerAba(ARQUIVO, ABA)
  .filter((r) => r.E && r.C)
  .map((r) => {
    const descricao = r.E.replace(/\s+/g, ' ').trim();
    const codigo = (descricao.match(/(?<![A-Z0-9])BC\d{4}[A-Z]{2}/) || [null])[0];
    const responsaveis = (r.Q || '').split(',').map((x) => x.trim()).filter(Boolean);
    return {
      n: r.n,
      dia: serial(r.C),
      prevista: r.M ? serial(r.M) : null,
      final: r.O ? serial(r.O) : null,
      qtd: Number(r.B),
      descricao,
      codigo,
      cliente: (r.G || '').replace(/\s+/g, ' ').trim() || null,
      responsaveis,
      tag: r.Y,
      status: r.AG,
      projeto: (r.AJ || '').trim(),
      valor: arred(Number(r.AO)),
      horaIn: hora(r.H),
      horaOut: hora(r.K),
    };
  });

for (const l of linhas) {
  if (!STATUS[l.status]) anomalias.push(`Linha ${l.n}: status desconhecido "${l.status}"`);
  if (!l.responsaveis.length) anomalias.push(`Linha ${l.n}: sem responsável`);
  for (const r of l.responsaveis) if (!PESSOAS[r]) anomalias.push(`Linha ${l.n}: responsável sem mapeamento "${r}"`);
  if (!AREA_POR_TAG[l.tag]) anomalias.push(`Linha ${l.n}: tag desconhecida "${l.tag}"`);
  if (!(l.qtd > 0) || !Number.isInteger(l.qtd)) anomalias.push(`Linha ${l.n}: QTD inválida ${l.qtd}`);
  if (!l.cliente) anomalias.push(`Linha ${l.n}: sem cliente (vira demanda sem demandante)`);
  if (!l.final) anomalias.push(`Linha ${l.n}: sem "Entr. Final"`);
}

// ---------------------------------------------------------------- tipos de peça e preços
/** Divide "A, B (x, y), C" em tipos, ignorando vírgulas dentro de parênteses. */
function dividirTipos(texto) {
  const partes = [];
  let atual = '';
  let nivel = 0;
  for (const ch of texto) {
    if (ch === '(') nivel++;
    if (ch === ')') nivel = Math.max(0, nivel - 1);
    if (ch === ',' && nivel === 0) {
      partes.push(atual);
      atual = '';
    } else {
      atual += ch;
    }
  }
  partes.push(atual);
  return partes.map((p) => p.replace(/"/g, '').replace(/\s+/g, ' ').trim()).filter(Boolean);
}

function normalizarTipo(texto) {
  let mult = 1;
  let nome = texto;
  const m = nome.match(/^(\d+)\s*x\s+(.*)$/i);
  if (m) {
    mult = Number(m[1]);
    nome = m[2];
  }
  nome = nome.replace(/^Banners\b/i, 'Banner').replace(/Quadrados$/i, 'Quadrado').replace(/Retângulos$/i, 'Retângulo');
  return { nome, mult };
}

const chaveTipo = (nome) => nome.toLowerCase();
const nomesPorChave = new Map(); // primeira grafia vista
for (const l of linhas) {
  l.tipos = dividirTipos(l.projeto).map(normalizarTipo);
  for (const t of l.tipos) if (!nomesPorChave.has(chaveTipo(t.nome))) nomesPorChave.set(chaveTipo(t.nome), t.nome);
}

// 1) preço dos tipos que aparecem sozinhos (um tipo, multiplicidade 1)
const preco = new Map();
const conflitos = [];
for (const l of linhas) {
  if (l.tipos.length === 1 && l.tipos[0].mult === 1) {
    const k = chaveTipo(l.tipos[0].nome);
    const unit = arred(l.valor / l.qtd);
    if (preco.has(k) && Math.abs(preco.get(k) - unit) > 0.011) conflitos.push(`${nomesPorChave.get(k)}: R$ ${preco.get(k)} x R$ ${unit} (linha ${l.n})`);
    else preco.set(k, unit);
  }
}
// 2) os demais saem das combinações com exatamente um tipo desconhecido
let mudou = true;
while (mudou) {
  mudou = false;
  for (const l of linhas) {
    const desconhecidos = l.tipos.filter((t) => !preco.has(chaveTipo(t.nome)));
    if (desconhecidos.length !== 1) continue;
    const conhecido = l.tipos.filter((t) => preco.has(chaveTipo(t.nome))).reduce((s, t) => s + preco.get(chaveTipo(t.nome)) * t.mult, 0);
    const d = desconhecidos[0];
    const unit = arred((l.valor / l.qtd - conhecido) / d.mult);
    if (unit > 0) {
      preco.set(chaveTipo(d.nome), unit);
      mudou = true;
    }
  }
}
// 3) o que sobrou sem preço entra com o valor da própria linha (marcado no relatório)
const semPreco = [...nomesPorChave.keys()].filter((k) => !preco.has(k));

// peças por linha, com conferência do total contra o Valor da planilha
const naoBatem = [];
for (const l of linhas) {
  const resolvidos = l.tipos.every((t) => preco.has(chaveTipo(t.nome)));
  const soma = resolvidos ? arred(l.tipos.reduce((s, t) => s + preco.get(chaveTipo(t.nome)) * t.mult, 0) * l.qtd) : null;
  if (resolvidos && Math.abs(soma - l.valor) <= 0.05) {
    l.pecas = l.tipos.map((t) => ({ tipo: t.nome, qtd: l.qtd * t.mult, unit: preco.get(chaveTipo(t.nome)) }));
    l.diferenca = arred(l.valor - soma);
  } else {
    // não reconcilia: uma peça única "Pacote (planilha)" com o valor exato da linha
    l.pecas = [{ tipo: 'Pacote (valor da planilha)', qtd: l.qtd, unit: arred(l.valor / l.qtd), pacote: true }];
    naoBatem.push(`Linha ${l.n}: "${l.projeto.slice(0, 70)}" QTD ${l.qtd}, soma dos tipos ${soma ?? 'sem preço'} x Valor ${l.valor}`);
  }
}
// a soma de qtd*unit pode diferir centavos do Valor por arredondamento; guardamos a diferença
for (const l of linhas) {
  l.totalPecas = arred(l.pecas.reduce((s, p) => s + p.qtd * p.unit, 0));
}

// área de cada tipo = a tag que mais o usa
const tagsPorTipo = new Map();
for (const l of linhas) for (const p of l.pecas) {
  const k = chaveTipo(p.tipo);
  const m = tagsPorTipo.get(k) ?? {};
  m[l.tag] = (m[l.tag] ?? 0) + 1;
  tagsPorTipo.set(k, m);
}
const tipos = new Map(); // chave -> {nome, area, preco}
for (const l of linhas) for (const p of l.pecas) {
  const k = chaveTipo(p.tipo);
  if (tipos.has(k)) continue;
  const tags = tagsPorTipo.get(k);
  const tag = Object.entries(tags).sort((a, b) => b[1] - a[1])[0][0];
  tipos.set(k, { nome: nomesPorChave.get(k) ?? p.tipo, area: AREA_POR_TAG[tag] ?? 'Design', preco: p.pacote ? 0.01 : preco.get(k), ativo: !p.pacote });
}

// ---------------------------------------------------------------- demandantes
const demandantes = [...new Set(linhas.map((l) => l.cliente).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'pt-BR'));
const primeiros = new Map();
for (const d of demandantes) {
  const k = d.split(' ')[0];
  primeiros.set(k, [...(primeiros.get(k) ?? []), d]);
}
const possiveisDuplicados = [...primeiros.values()].filter((v) => v.length > 1);

// ---------------------------------------------------------------- SQL: limpeza (uso único)
const limpeza = `-- 00-limpar-dados-de-teste.sql  (USO ÚNICO; gerado por gerar-importacao.cjs)
-- Apaga os dados de teste do Supabase antes da importação real.
-- PRESERVA: contas com login (auth_user_id), diretorias, áreas reais, cargos e tipos de atividade.
-- Roda numa transação: se algo estranho aparecer, dê ROLLBACK em vez de COMMIT.
begin;

delete from relatorio_mensal;
delete from evidencia;
delete from atividade;
delete from peca;
delete from pessoa_demanda;
delete from auditoria where tabela = 'demanda';
delete from demanda;
delete from campanha;
delete from projeto;
delete from demandante;
delete from tipo_peca;

-- pessoas de teste sem login (as 3 contas com login ficam intactas)
delete from pessoa
 where auth_user_id is null
   and id in (${IDS_TESTE_SEM_LOGIN.map((i) => `'${i}'`).join(', ')});

-- área de teste, se ninguém a usa
delete from area a
 where a.nome = 'Área de Teste'
   and not exists (select 1 from pessoa p where p.area_id = a.id)
   and not exists (select 1 from pessoa p where p.referencia_area_id = a.id);

-- conferência
select (select count(*) from demanda) as demandas, (select count(*) from pessoa) as pessoas,
       (select count(*) from pessoa where auth_user_id is not null) as pessoas_com_login,
       (select count(*) from tipo_peca) as tipos_peca;

commit;
`;

// ---------------------------------------------------------------- SQL: importação (reprocessável)
function montarImportacao(ls) {
const valoresLinha = ls.map((l) =>
  `(${l.n}, ${sql(l.dia)}::date, ${sql(l.prevista)}::date, ${sql(l.final)}::date, ${l.qtd}, ${sql(l.descricao)}, ${sql(l.codigo)}, ${sql(l.cliente)}, ${sql(l.responsaveis.join('|'))}, ${sql(l.tag)}, ${sql(STATUS[l.status])}, ${l.valor}, ${sql(l.horaIn)}, ${sql(l.horaOut)})`);
const valoresPeca = ls.flatMap((l) => l.pecas.map((p, i) => `(${l.n}, ${i + 1}, ${sql(p.tipo)}, ${p.qtd}, ${p.unit})`));
const usados = new Set(ls.flatMap((l) => l.pecas.map((p) => chaveTipo(p.tipo))));
const valoresTipo = [...tipos.entries()].filter(([k]) => usados.has(k)).map(([, t]) => `(${sql(t.nome)}, ${sql(t.area)}, ${t.preco}, ${t.ativo})`);
const pessoasNovas = Object.values(PESSOAS).filter((p) => !p.existente);
const valoresPessoa = pessoasNovas.map((p) => `(${sql(p.nome)}, ${sql(p.exibicao)}, ${sql(p.area)}, ${sql(p.cargo)})`);
const mapaResp = Object.entries(PESSOAS).map(([chave, p]) => `(${sql(chave)}, ${sql(p.nome)})`);

return `-- 01-importar-planilha-2026.sql  (REPROCESSÁVEL; gerado por gerar-importacao.cjs)
-- Aba "${ABA}" da planilha DEMANDAS HOUSE CRM: ${ls.length} linhas -> ${ls.length} demandas.
-- Reprocessar apaga só o que este script importou antes (source_system = '${SOURCE_SYSTEM}').
-- Pré-requisito: migration V12 (pessoa.nome_exibicao). Roda numa transação (COMMIT no fim).
begin;

-- 1. dados da planilha em tabelas temporárias
create temp table imp_linha (
  src_row int primary key, dia date not null, prevista date, final date, qtd int not null,
  descricao text not null, codigo text, cliente text, responsaveis text not null, tag text not null,
  status text not null, valor numeric(14,2) not null, hora_in text, hora_out text
) on commit drop;
insert into imp_linha values
${valoresLinha.join(',\n')};

create temp table imp_peca (src_row int, ordem int, tipo text not null, qtd int not null, unit numeric(14,2) not null) on commit drop;
insert into imp_peca values
${valoresPeca.join(',\n')};

create temp table imp_tipo (nome text primary key, area text not null, preco numeric(14,2) not null, ativo boolean not null) on commit drop;
insert into imp_tipo values
${valoresTipo.join(',\n')};

create temp table imp_pessoa (nome text primary key, exibicao text not null, area text not null, cargo text not null) on commit drop;
insert into imp_pessoa values
${valoresPessoa.join(',\n')};

create temp table imp_resp (chave text primary key, nome text not null) on commit drop;
insert into imp_resp values
${mapaResp.join(',\n')};

-- 2. remove importação anterior (reprocessamento)
delete from evidencia where atividade_id in (select a.id from atividade a join demanda d on d.id = a.demanda_id where d.source_system = '${SOURCE_SYSTEM}')
                         or peca_id in (select p.id from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SOURCE_SYSTEM}');
delete from atividade where demanda_id in (select id from demanda where source_system = '${SOURCE_SYSTEM}');
delete from peca where demanda_id in (select id from demanda where source_system = '${SOURCE_SYSTEM}');
delete from pessoa_demanda where demanda_id in (select id from demanda where source_system = '${SOURCE_SYSTEM}');
delete from demanda where source_system = '${SOURCE_SYSTEM}';

-- 3. pessoas: Neto já existe (conta com login); os demais entram sem login e sem e-mail
update pessoa set nome = ${sql(PESSOAS.Deusdete.nome)}, nome_exibicao = ${sql(PESSOAS.Deusdete.exibicao)}
 where id = 'e7bb90d9-7b9b-475e-aa32-3b215e000ee4';

insert into pessoa (nome, nome_exibicao, area_id, cargo_id, status, perfil, aprovado_em)
select i.nome, i.exibicao, a.id, c.id, 'ATIVO', 'PROFISSIONAL', now()
  from imp_pessoa i
  join area a on a.nome = i.area
  join cargo c on c.nome = i.cargo
 where not exists (select 1 from pessoa p where p.nome = i.nome);

-- 4. demandantes (todos são pessoas do CRM)
insert into demandante (nome, tipo, diretoria_id, ativo)
select distinct l.cliente, 'PESSOA', (select id from diretoria where nome = '${DIRETORIA}'), true
  from imp_linha l
 where l.cliente is not null
   and not exists (select 1 from demandante d where d.nome = l.cliente);

-- 5. tipos de peça: preço unitário de referência derivado da própria planilha
insert into tipo_peca (nome, area_id, valor_referencia, ativo)
select t.nome, a.id, t.preco, t.ativo
  from imp_tipo t join area a on a.nome = t.area
 where not exists (select 1 from tipo_peca x where x.area_id = a.id and x.nome = t.nome);

insert into tipo_atividade (nome, ativo)
select '${TIPO_ATIVIDADE}', true
 where not exists (select 1 from tipo_atividade where nome = '${TIPO_ATIVIDADE}');

-- 6. demandas
insert into demanda (titulo, codigo, diretoria_id, demandante_id, prioridade, status, data_criacao, data_prazo,
                     data_entrega_real, valor, observacoes, source_system, source_sheet, source_row, imported_at)
select left(l.descricao, 220), l.codigo, (select id from diretoria where nome = '${DIRETORIA}'),
       (select id from demandante d where d.nome = l.cliente limit 1), 'NORMAL', l.status, l.dia, l.prevista,
       case when l.status = 'CONCLUIDA' then coalesce(l.final, l.dia) end, l.valor,
       'Importada da planilha (aba ${ABA}, linha ' || l.src_row || ').'
         || coalesce(' Horário: ' || l.hora_in || ' às ' || l.hora_out || '.', '') || ' Equipe: ' || l.tag || '.',
       '${SOURCE_SYSTEM}', '${ABA}', l.src_row, now()
  from imp_linha l;

-- 7. participantes: o primeiro responsável é o principal
insert into pessoa_demanda (pessoa_id, demanda_id, papel, data_entrada)
select p.id, d.id, case when r.ord = 1 then 'RESPONSAVEL_PRINCIPAL' else 'PARTICIPANTE' end, l.dia
  from imp_linha l
  join demanda d on d.source_system = '${SOURCE_SYSTEM}' and d.source_row = l.src_row
  cross join lateral unnest(string_to_array(l.responsaveis, '|')) with ordinality as r(chave, ord)
  join imp_resp m on m.chave = r.chave
  join pessoa p on p.nome = m.nome;

-- 8. peças: uma por tipo; o valor unitário fica congelado como na planilha; a peça é do primeiro responsável
insert into peca (demanda_id, tipo_peca_id, nome, quantidade, valor_unitario, pessoa_id, data_entrega)
select d.id, tp.id, left(i.tipo, 180), i.qtd, i.unit,
       (select p.id from pessoa p join imp_resp m on m.nome = p.nome
         where m.chave = split_part(l.responsaveis, '|', 1)),
       l.dia
  from imp_peca i
  join imp_linha l on l.src_row = i.src_row
  join demanda d on d.source_system = '${SOURCE_SYSTEM}' and d.source_row = l.src_row
  join imp_tipo t on t.nome = i.tipo
  join area a on a.nome = t.area
  join tipo_peca tp on tp.area_id = a.id and tp.nome = t.nome;

-- 9. atividades: uma "${TIPO_ATIVIDADE}" por responsável, no dia da linha
insert into atividade (demanda_id, tipo_atividade_id, pessoa_id, descricao, data_realizacao)
select d.id, (select id from tipo_atividade where nome = '${TIPO_ATIVIDADE}'), p.id, left(l.descricao, 500), l.dia
  from imp_linha l
  join demanda d on d.source_system = '${SOURCE_SYSTEM}' and d.source_row = l.src_row
  cross join lateral unnest(string_to_array(l.responsaveis, '|')) as r(chave)
  join imp_resp m on m.chave = r.chave
  join pessoa p on p.nome = m.nome;

-- 10. conferência (compare com relatorio-conferencia.md antes do COMMIT)
select (select count(*) from demanda where source_system = '${SOURCE_SYSTEM}') as demandas,
       (select count(*) from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SOURCE_SYSTEM}') as pecas,
       (select count(*) from atividade a join demanda d on d.id = a.demanda_id where d.source_system = '${SOURCE_SYSTEM}') as atividades,
       (select count(*) from pessoa_demanda pd join demanda d on d.id = pd.demanda_id where d.source_system = '${SOURCE_SYSTEM}') as participantes,
       (select round(sum(p.quantidade * p.valor_unitario), 2) from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SOURCE_SYSTEM}') as soma_pecas,
       (select round(sum(valor), 2) from demanda where source_system = '${SOURCE_SYSTEM}') as soma_valor_demandas,
       (select count(*) from demandante) as demandantes,
       (select count(*) from tipo_peca) as tipos_peca;

commit;

-- Novos integrantes do CRM ainda sem área/cargo definidos (pessoa.area_id é obrigatório). Quando souber a área:
${PESSOAS_PENDENTES.map((p) => `-- insert into pessoa (nome, nome_exibicao, area_id, status, perfil, aprovado_em) select ${sql(p.nome)}, ${sql(p.exibicao)}, id, 'ATIVO', 'PROFISSIONAL', now() from area where nome = '<ÁREA>';`).join('\n')}
`;
}
const importacao = montarImportacao(linhas);

// Ensaio: V12 + limpeza + importação das amostra de linhas numa transação que termina em ROLLBACK
// amostra do ensaio: as 8 primeiras linhas + um exemplo de cada caso de borda
const amostra = [];
const vistos = new Set();
for (const l of linhas) {
  const casos = [l.responsaveis.length > 1 && 'varios-responsaveis', l.pecas.some((p) => p.pacote) && 'pacote', !l.cliente && 'sem-cliente', !l.final && 'sem-final', l.tipos.length > 3 && 'muitos-tipos', `status-${l.status}`];
  const novo = casos.filter((c) => c && !vistos.has(c));
  if (amostra.length < 8 || novo.length) {
    amostra.push(l);
    novo.forEach((c) => vistos.add(c));
  }
}
const semTransacao = (t) => t.replace(/^begin;$/m, '').replace(/^commit;$/gm, '');
const v12 = fs.readFileSync(path.join(__dirname, '..', 'migrations', 'V12__pessoa-nome-exibicao-cpf.sql'), 'utf8');
const ensaio = `begin;
${v12}
${semTransacao(limpeza)}
${semTransacao(montarImportacao(amostra))}
rollback;
`;

// ---------------------------------------------------------------- relatório de conferência
const fmt = (n) => n.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const porMes = {};
for (const l of linhas) {
  const m = l.dia.slice(0, 7);
  porMes[m] = porMes[m] ?? { demandas: 0, valor: 0 };
  porMes[m].demandas++;
  porMes[m].valor += l.valor;
}
const porPessoa = {};
for (const l of linhas) {
  const k = l.responsaveis[0];
  porPessoa[k] = porPessoa[k] ?? { linhas: 0, valor: 0 };
  porPessoa[k].linhas++;
  porPessoa[k].valor += l.valor;
}
const totalPecas = linhas.reduce((s, l) => s + l.totalPecas, 0);
const totalValor = linhas.reduce((s, l) => s + l.valor, 0);
const codigosDistintos = new Set(linhas.map((l) => l.codigo).filter(Boolean)).size;
const comDiferenca = linhas.filter((l) => Math.abs(l.totalPecas - l.valor) > 0.001);

const relatorio = `# Conferência da importação: planilha DEMANDAS HOUSE CRM, aba ${ABA}

Gerado por \`gerar-importacao.cjs\` a partir de \`${path.basename(ARQUIVO)}\`. **Nada foi executado no banco.**

## Totais
| Item | Valor |
|---|---|
| Linhas lidas (viram demandas) | ${linhas.length} |
| Peças geradas (uma por tipo de cada linha) | ${linhas.reduce((s, l) => s + l.pecas.length, 0)} |
| Atividades geradas (uma por responsável) | ${linhas.reduce((s, l) => s + l.responsaveis.length, 0)} |
| Demandantes (clientes distintos) | ${demandantes.length} |
| Tipos de peça distintos | ${tipos.size} |
| Códigos BC distintos / linhas com código | ${codigosDistintos} / ${linhas.filter((l) => l.codigo).length} |
| Soma do Valor na planilha | R$ ${fmt(totalValor)} |
| Soma das peças geradas (deve ser igual) | R$ ${fmt(totalPecas)} |
| Linhas em que a soma das peças difere do Valor | ${comDiferenca.length} |

## Por mês (data da coluna Dia)
| Mês | Demandas | Valor |
|---|---|---|
${Object.entries(porMes).sort().map(([m, v]) => `| ${m} | ${v.demandas} | R$ ${fmt(v.valor)} |`).join('\n')}

## Por responsável principal
| Responsável (planilha) | Pessoa no sistema | Linhas | Valor |
|---|---|---|---|
${Object.entries(porPessoa).sort((a, b) => b[1].valor - a[1].valor).map(([k, v]) => `| ${k} | ${PESSOAS[k]?.nome ?? '?'} (${PESSOAS[k]?.exibicao ?? ''}) | ${v.linhas} | R$ ${fmt(v.valor)} |`).join('\n')}

## Tipos de peça e preço unitário (derivado da planilha)
| Tipo | Área | Preço unitário |
|---|---|---|
${[...tipos.values()].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')).map((t) => `| ${t.nome}${t.ativo ? '' : ' (inativo; só para linha que não reconcilia)'} | ${t.area} | R$ ${fmt(t.preco)} |`).join('\n')}

## Pontos de atenção
- **Linhas que não reconciliam** (tipo sem preço deduzível ou soma diferente do Valor): ${naoBatem.length}${naoBatem.length ? '\n' + naoBatem.map((x) => `  - ${x}`).join('\n') : ''}
- **Tipos sem preço deduzível**: ${semPreco.length ? semPreco.map((k) => nomesPorChave.get(k)).join('; ') : 'nenhum'}
- **Conflitos de preço** (o mesmo tipo com preços diferentes em linhas de um tipo só): ${conflitos.length ? conflitos.join('; ') : 'nenhum'}
- **Anomalias de dados**: ${anomalias.length}${anomalias.length ? '\n' + anomalias.map((x) => `  - ${x}`).join('\n') : ''}
- **Possíveis demandantes duplicados** (mesmo primeiro nome, grafias diferentes; importados separados, decidir se unifica):
${possiveisDuplicados.map((v) => `  - ${v.join(' / ')}`).join('\n') || '  - nenhum'}
- **Linhas com a data final diferente do dia**: ${linhas.filter((l) => l.final && l.final !== l.dia).length}. O mês de cada peça e atividade segue a coluna **Dia** (o mesmo critério da coluna Mês da planilha).
- **Códigos BC repetidos em várias linhas**: ${linhas.filter((l) => l.codigo).length - codigosDistintos}. O código fica na demanda, sem unicidade; agrupar por campanha fica para depois.

## O que o script faz no banco
1. \`00-limpar-dados-de-teste.sql\` (uma vez): apaga demandas, peças, atividades, evidências, relatórios, campanhas, projetos, demandantes, tipos de peça e as 6 pessoas de teste sem login. **Preserva as 3 contas com login** (Deusdete Neto, Pessoa Teste Vinculo e Pessoa Teste Permissao), diretorias, áreas, cargos e tipos de atividade.
2. \`01-importar-planilha-2026.sql\` (reprocessável): cria pessoas, demandantes, tipos de peça e atividade "${TIPO_ATIVIDADE}", e importa as demandas com participantes, peças e atividades. Reprocessar apaga só o que ele importou.
3. Pessoas novas entram **sem login e sem e-mail**. Quando cada pessoa se cadastrar, um Admin vincula a conta à pessoa existente (\`PATCH\` de vínculo de auth) para não duplicar.
4. Pendentes: ${PESSOAS_PENDENTES.map((p) => `${p.nome} (${p.exibicao})`).join(', ')} (falta a área de cada um; há um bloco comentado no fim do script).
`;

const saida = path.join(__dirname, 'saida');
fs.mkdirSync(saida, { recursive: true });
fs.writeFileSync(path.join(saida, '00-limpar-dados-de-teste.sql'), limpeza);
fs.writeFileSync(path.join(saida, '01-importar-planilha-2026.sql'), importacao);
fs.writeFileSync(path.join(saida, 'relatorio-conferencia.md'), relatorio);
fs.writeFileSync(path.join(saida, '99-ensaio-amostra-com-rollback.sql'), ensaio);
console.log(`Linhas ${linhas.length} | tipos ${tipos.size} | demandantes ${demandantes.length} | não reconciliam ${naoBatem.length} | anomalias ${anomalias.length}`);
console.log(`Soma planilha ${fmt(totalValor)} x peças ${fmt(totalPecas)} | tamanho do SQL ${(importacao.length / 1024).toFixed(0)} KB`);
