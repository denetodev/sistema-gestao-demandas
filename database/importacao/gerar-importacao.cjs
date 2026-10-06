#!/usr/bin/env node
/**
 * Gera o SQL de importação da planilha "DEMANDAS HOUSE CRM" (aba 2026) para o Supabase.
 *
 *   node database/importacao/gerar-importacao.cjs "C:/caminho/DEMANDAS HOUSE CRM 2026-10.xlsx"
 *
 * Saída (pasta database/importacao/saida/, ignorada pelo git porque tem dados reais de pessoas):
 *   00-limpar-dados-de-teste.sql   uso único: apaga os dados de teste (preserva contas com login)
 *   01-carga-NN.sql                carrega as tabelas de apoio imp_hc_* (arquivos pequenos, em ordem)
 *   02-importar.sql                reprocessável: apaga só o que importou antes e reimporta
 *   03-ensaio-completo-com-rollback.sql  limpeza + importação inteiras, terminando em ROLLBACK
 *   relatorio-conferencia.md       totais, mapeamentos e anomalias para conferir ANTES de rodar
 *
 * Regras de mapeamento (ver relatorio-conferencia.md):
 *   - linhas com o mesmo código BC são a mesma demanda lançada por pessoas diferentes: viram UMA demanda
 *     (linhas sem código ficam uma demanda cada; canceladas não se misturam às ativas)
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
// Ajustes de demandantes confirmados pelo Neto (a planilha traz só o primeiro nome em alguns casos)
const DEMANDANTES = {
  Adriana: { nome: 'Adriana Monteiro da Silva' },
  'Adriana dos Santos': { nome: 'Adriana dos Santos Lima', diretoria: 'TESOU/GEASE/MERCADO' },
  Anderson: { nome: 'Anderson Bezerra', observacao: 'Conhecido como Parcinha' },
  Fernanda: { nome: 'Fernanda Parizi' },
};
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
    compartilhadas.push(dec([...m[1].matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map((x) => x[1]).join('')).normalize('NFC'));
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
    const descricao = r.E.normalize('NFC').replace(/\s+/g, ' ').trim();
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
  if (l.cliente && DEMANDANTES[l.cliente]) l.cliente = DEMANDANTES[l.cliente].nome;
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

// ---------------------------------------------------------------- demandas (agrupadas por código BC)
/**
 * Na planilha cada profissional lança a demanda do seu jeito e a mesma demanda aparece várias vezes.
 * Todas as linhas com o mesmo código BC viram UMA demanda. Linhas sem código ficam uma demanda cada.
 * Linhas canceladas não se misturam às ativas do mesmo código (o valor cancelado fica fora do total).
 */
const chaveGrupo = (l) => {
  if (!l.codigo) return `linha-${l.n}`;
  return l.status === 'Cancelado' ? `${l.codigo}#cancelada` : l.codigo;
};
const PRECEDENCIA_STATUS = ['EM_ANDAMENTO', 'EM_APROVACAO', 'CONCLUIDA', 'CANCELADA'];

const gruposPorChave = new Map();
for (const l of linhas) {
  const k = chaveGrupo(l);
  if (!gruposPorChave.has(k)) gruposPorChave.set(k, []);
  gruposPorChave.get(k).push(l);
}
const conflitosCliente = [];
const gruposMistos = [];
const grupos = [...gruposPorChave.entries()]
  .map(([chave, ls]) => {
    ls.sort((a, b) => a.dia.localeCompare(b.dia) || a.n - b.n);
    const primeira = ls[0];
    const status = PRECEDENCIA_STATUS.find((s) => ls.some((l) => STATUS[l.status] === s));
    const contagemClientes = new Map();
    ls.forEach((l) => l.cliente && contagemClientes.set(l.cliente, (contagemClientes.get(l.cliente) ?? 0) + 1));
    const clientes = [...contagemClientes.entries()].sort((a, b) => b[1] - a[1]);
    if (clientes.length > 1) conflitosCliente.push(`${primeira.codigo ?? 'linha ' + primeira.n}: ${clientes.map(([c, n]) => `${c} (${n})`).join(' / ')}`);
    const statusDistintos = new Set(ls.map((l) => STATUS[l.status]));
    if (statusDistintos.size > 1) gruposMistos.push(`${primeira.codigo}: ${[...statusDistintos].join(' + ')} -> ${status}`);

    // participantes: o responsável da linha mais antiga é o principal
    const participantes = new Map();
    for (const l of ls) for (const r of l.responsaveis) if (!participantes.has(r)) participantes.set(r, { chave: r, entrada: l.dia, principal: participantes.size === 0 });
    return {
      chave,
      refRow: Math.min(...ls.map((l) => l.n)),
      codigo: primeira.codigo,
      titulo: primeira.descricao,
      cliente: clientes[0]?.[0] ?? null,
      outrosClientes: clientes.slice(1).map(([c]) => c),
      status,
      criacao: ls[0].dia,
      prazo: ls.map((l) => l.prevista ?? l.dia).sort().at(-1),
      entrega: ls.map((l) => l.final ?? l.dia).sort().at(-1),
      valor: arred(ls.reduce((s, l) => s + l.valor, 0)),
      linhas: ls.map((l) => l.n).sort((a, b) => a - b),
      equipe: [...new Set(ls.map((l) => l.tag))].join(', '),
      participantes: [...participantes.values()],
      ls,
    };
  })
  .sort((a, b) => a.refRow - b.refRow);
grupos.forEach((g, i) => {
  g.id = i + 1;
  g.ls.forEach((l) => (l.grp = g.id));
});

// ---------------------------------------------------------------- demandantes
const demandantes = new Map(); // nome -> { diretoria, observacao }
for (const l of linhas) {
  if (!l.cliente || demandantes.has(l.cliente)) continue;
  const ajuste = Object.values(DEMANDANTES).find((d) => d.nome === l.cliente) ?? {};
  demandantes.set(l.cliente, { diretoria: ajuste.diretoria ?? DIRETORIA, observacao: ajuste.observacao ?? null });
}
const primeiros = new Map();
for (const d of demandantes.keys()) {
  const k = d.split(' ')[0];
  primeiros.set(k, [...(primeiros.get(k) ?? []), d]);
}
const possiveisDuplicados = [...primeiros.values()].filter((v) => v.length > 1);

// ---------------------------------------------------------------- SQL: limpeza (uso único)
const corpoLimpeza = `-- 00-limpar-dados-de-teste.sql  (USO ÚNICO; gerado por gerar-importacao.cjs)
-- Apaga os dados de teste do Supabase antes da importação real.
-- PRESERVA: contas com login (auth_user_id), diretorias, áreas reais, cargos e tipos de atividade.
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
`;
const limpeza = `begin;\n${corpoLimpeza}
select (select count(*) from demanda) as demandas, (select count(*) from pessoa) as pessoas,
       (select count(*) from pessoa where auth_user_id is not null) as pessoas_com_login,
       (select count(*) from tipo_peca) as tipos_peca;
commit;
`;

// ---------------------------------------------------------------- SQL: carga em tabelas de apoio (imp_hc_*)
const tiposLista = [...tipos.values()];
tiposLista.forEach((t, i) => (t.id = i + 1));
const idTipo = new Map(tiposLista.map((t) => [chaveTipo(t.nome), t.id]));

const dataCurta = (v) => sql(v);
const instrucoes = []; // cada item é um INSERT completo
const lote = (cabecalho, valores, tamanho = 120) => {
  for (let i = 0; i < valores.length; i += tamanho) instrucoes.push(`${cabecalho}\n${valores.slice(i, i + tamanho).join(',\n')};`);
};
const pessoasNovas = Object.values(PESSOAS).filter((p) => !p.existente);

lote('insert into imp_hc_tipo values', tiposLista.map((t) => `(${t.id}, ${sql(t.nome)}, ${sql(t.area)}, ${t.preco}, ${t.ativo})`));
lote('insert into imp_hc_demandante values', [...demandantes.entries()].map(([nome, d]) => `(${sql(nome)}, ${sql(d.diretoria)}, ${sql(d.observacao)})`));
lote('insert into imp_hc_pessoa values', pessoasNovas.map((p) => `(${sql(p.nome)}, ${sql(p.exibicao)}, ${sql(p.area)}, ${sql(p.cargo)})`));
lote('insert into imp_hc_resp values', Object.entries(PESSOAS).map(([c, p]) => `(${sql(c)}, ${sql(p.nome)})`));
lote('insert into imp_hc_grupo values', grupos.map((g) =>
  `(${g.id}, ${g.refRow}, ${sql(g.codigo)}, ${sql(g.cliente)}, ${sql(g.status)}, ${sql(g.criacao)}, ${sql(g.prazo)}, ${sql(g.entrega)}, ${g.valor}, ${sql(g.linhas.join(', '))}, ${sql(g.equipe)}, ${sql(g.outrosClientes.length ? g.outrosClientes.join('; ') : null)})`), 150);
lote('insert into imp_hc_part values', grupos.flatMap((g) => g.participantes.map((p) => `(${g.id}, ${sql(p.chave)}, ${p.principal}, ${sql(p.entrada)})`)), 250);
lote('insert into imp_hc_linha values', linhas.map((l) =>
  `(${l.n}, ${l.grp}, ${sql(l.dia)}, ${sql(l.prevista && l.prevista !== l.dia ? l.prevista : null)}, ${sql(l.final && l.final !== l.dia ? l.final : null)}, ${sql(l.descricao)}, ${sql(l.responsaveis.join('|'))}, ${sql(STATUS[l.status])})`), 100);
lote('insert into imp_hc_peca values', linhas.flatMap((l) => l.pecas.map((p, i) => `(${l.n}, ${i + 1}, ${idTipo.get(chaveTipo(p.tipo))}, ${p.qtd}, ${p.unit})`)), 200);

const DDL_APOIO = `-- Tabelas de apoio da importação (apagadas no fim do 02-importar.sql)
drop table if exists imp_hc_linha, imp_hc_peca, imp_hc_part, imp_hc_grupo, imp_hc_tipo, imp_hc_demandante, imp_hc_pessoa, imp_hc_resp;
create table imp_hc_tipo (id int primary key, nome text not null, area text not null, preco numeric(14,2) not null, ativo boolean not null);
create table imp_hc_demandante (nome text primary key, diretoria text not null, observacao text);
create table imp_hc_pessoa (nome text primary key, exibicao text not null, area text not null, cargo text not null);
create table imp_hc_resp (chave text primary key, nome text not null);
create table imp_hc_grupo (grp int primary key, ref_row int not null unique, codigo text, cliente text, status text not null,
  criacao text not null, prazo text not null, entrega text not null, valor numeric(14,2) not null, linhas text not null, equipe text not null, outros_clientes text);
create table imp_hc_part (grp int not null, resp text not null, principal boolean not null, entrada text not null);
create table imp_hc_linha (src_row int primary key, grp int not null, dia text not null, prevista text, final text,
  descricao text not null, responsaveis text not null, status text not null);
create table imp_hc_peca (src_row int not null, ordem int not null, tipo int not null, qtd int not null, unit numeric(14,2) not null);
-- RLS ligado (sem policies): estas tabelas têm dados reais e não podem ficar expostas pela anon key
${['linha', 'peca', 'part', 'grupo', 'tipo', 'demandante', 'pessoa', 'resp'].map((t) => `alter table imp_hc_${t} enable row level security;`).join('\n')}
`;

// empacota as instruções em arquivos de até ~45 KB (o primeiro leva o DDL)
const LIMITE = 45 * 1024;
const arquivosCarga = [];
let atual = DDL_APOIO;
for (const ins of instrucoes) {
  if (atual.length + ins.length > LIMITE && atual.length > 0) {
    arquivosCarga.push(atual);
    atual = '';
  }
  atual += (atual ? '\n' : '') + ins + '\n';
}
if (atual.trim()) arquivosCarga.push(atual);

// ---------------------------------------------------------------- SQL: importação a partir das tabelas de apoio
const SS = SOURCE_SYSTEM;
const corpoImportacao = `-- 02-importar.sql  (REPROCESSÁVEL; gerado por gerar-importacao.cjs)
-- Aba "${ABA}": ${linhas.length} linhas da planilha -> ${grupos.length} demandas (linhas com o mesmo código BC viram uma só).
-- Reprocessar apaga só o que este script importou antes (source_system = '${SS}').
-- Pré-requisito: migration V12 e as tabelas imp_hc_* carregadas pelos arquivos 01-carga-*.sql.

-- 1. remove importação anterior
delete from evidencia where atividade_id in (select a.id from atividade a join demanda d on d.id = a.demanda_id where d.source_system = '${SS}')
                         or peca_id in (select p.id from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SS}');
delete from atividade where demanda_id in (select id from demanda where source_system = '${SS}');
delete from peca where demanda_id in (select id from demanda where source_system = '${SS}');
delete from pessoa_demanda where demanda_id in (select id from demanda where source_system = '${SS}');
delete from demanda where source_system = '${SS}';

-- 2. diretorias dos demandantes que não são do CRM
insert into diretoria (nome)
select distinct d.diretoria from imp_hc_demandante d
 where d.diretoria <> '${DIRETORIA}' and not exists (select 1 from diretoria x where x.nome = d.diretoria);

-- 3. pessoas: Neto já existe (conta com login); os demais entram sem login e sem e-mail
update pessoa set nome = ${sql(PESSOAS.Deusdete.nome)}, nome_exibicao = ${sql(PESSOAS.Deusdete.exibicao)}
 where id = 'e7bb90d9-7b9b-475e-aa32-3b215e000ee4';

insert into pessoa (nome, nome_exibicao, area_id, cargo_id, status, perfil, aprovado_em)
select i.nome, i.exibicao, a.id, c.id, 'ATIVO', 'PROFISSIONAL', now()
  from imp_hc_pessoa i
  join area a on a.nome = i.area
  join cargo c on c.nome = i.cargo
 where not exists (select 1 from pessoa p where p.nome = i.nome);

-- 4. demandantes (todos são pessoas)
insert into demandante (nome, tipo, diretoria_id, observacao, ativo)
select d.nome, 'PESSOA', (select id from diretoria where nome = d.diretoria), d.observacao, true
  from imp_hc_demandante d
 where not exists (select 1 from demandante x where x.nome = d.nome);

-- 5. tipos de peça (preço de referência derivado da própria planilha) e tipo de atividade
insert into tipo_peca (nome, area_id, valor_referencia, ativo)
select t.nome, a.id, t.preco, t.ativo
  from imp_hc_tipo t join area a on a.nome = t.area
 where not exists (select 1 from tipo_peca x where x.area_id = a.id and x.nome = t.nome);

insert into tipo_atividade (nome, ativo)
select '${TIPO_ATIVIDADE}', true where not exists (select 1 from tipo_atividade where nome = '${TIPO_ATIVIDADE}');

-- 6. demandas: o título é a descrição da linha mais antiga do código
insert into demanda (titulo, codigo, diretoria_id, demandante_id, prioridade, status, data_criacao, data_prazo,
                     data_entrega_real, valor, observacoes, source_system, source_sheet, source_row, imported_at)
select left(l.descricao, 220), g.codigo, (select id from diretoria where nome = '${DIRETORIA}'),
       (select id from demandante x where x.nome = g.cliente limit 1), 'NORMAL', g.status,
       g.criacao::date, g.prazo::date, case when g.status = 'CONCLUIDA' then g.entrega::date end, g.valor,
       'Importada da planilha (aba ${ABA}, linha(s) ' || g.linhas || '). Equipe: ' || g.equipe || '.'
         || coalesce(' Outros demandantes citados nas linhas: ' || g.outros_clientes || '.', ''),
       '${SS}', '${ABA}', g.ref_row, now()
  from imp_hc_grupo g join imp_hc_linha l on l.src_row = g.ref_row;

-- 7. participantes: o responsável da linha mais antiga é o principal
insert into pessoa_demanda (pessoa_id, demanda_id, papel, data_entrada)
select p.id, d.id, case when pt.principal then 'RESPONSAVEL_PRINCIPAL' else 'PARTICIPANTE' end, pt.entrada::date
  from imp_hc_part pt
  join imp_hc_grupo g on g.grp = pt.grp
  join demanda d on d.source_system = '${SS}' and d.source_row = g.ref_row
  join imp_hc_resp m on m.chave = pt.resp
  join pessoa p on p.nome = m.nome;

-- 8. peças: uma por tipo de cada linha, com o valor unitário congelado; a peça é do primeiro responsável da linha
insert into peca (demanda_id, tipo_peca_id, nome, descricao, quantidade, valor_unitario, pessoa_id, data_entrega)
select d.id, tp.id, left(t.nome, 180), left(l.descricao, 500), i.qtd, i.unit,
       (select p.id from pessoa p join imp_hc_resp m on m.nome = p.nome where m.chave = split_part(l.responsaveis, '|', 1)),
       l.dia::date
  from imp_hc_peca i
  join imp_hc_linha l on l.src_row = i.src_row
  join imp_hc_grupo g on g.grp = l.grp
  join demanda d on d.source_system = '${SS}' and d.source_row = g.ref_row
  join imp_hc_tipo t on t.id = i.tipo
  join area a on a.nome = t.area
  join tipo_peca tp on tp.area_id = a.id and tp.nome = t.nome;

-- 9. atividades: uma "${TIPO_ATIVIDADE}" por responsável de cada linha, no dia da linha
insert into atividade (demanda_id, tipo_atividade_id, pessoa_id, descricao, data_realizacao)
select d.id, (select id from tipo_atividade where nome = '${TIPO_ATIVIDADE}'), p.id, left(l.descricao, 500), l.dia::date
  from imp_hc_linha l
  join imp_hc_grupo g on g.grp = l.grp
  join demanda d on d.source_system = '${SS}' and d.source_row = g.ref_row
  cross join lateral unnest(string_to_array(l.responsaveis, '|')) as r(chave)
  join imp_hc_resp m on m.chave = r.chave
  join pessoa p on p.nome = m.nome;
`;
const CONFERENCIA = `select (select count(*) from demanda where source_system = '${SS}') as demandas,
       (select count(*) from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SS}') as pecas,
       (select count(*) from atividade a join demanda d on d.id = a.demanda_id where d.source_system = '${SS}') as atividades,
       (select count(*) from pessoa_demanda pd join demanda d on d.id = pd.demanda_id where d.source_system = '${SS}') as participantes,
       (select round(sum(p.quantidade * p.valor_unitario), 2) from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SS}') as soma_pecas,
       (select round(sum(valor), 2) from demanda where source_system = '${SS}') as soma_valor_demandas,
       (select round(sum(p.quantidade * p.valor_unitario), 2) from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SS}' and d.status <> 'CANCELADA') as soma_sem_canceladas,
       (select count(*) from demandante) as demandantes, (select count(*) from tipo_peca) as tipos_peca,
       (select count(*) from pessoa) as pessoas, (select count(*) from pessoa where auth_user_id is not null) as pessoas_com_login;`;
const importacao = `begin;\n${corpoImportacao}\ndrop table imp_hc_linha, imp_hc_peca, imp_hc_part, imp_hc_grupo, imp_hc_tipo, imp_hc_demandante, imp_hc_pessoa, imp_hc_resp;\n\n-- conferência: compare com relatorio-conferencia.md antes do COMMIT\n${CONFERENCIA}\n\ncommit;\n\n-- Novos integrantes do CRM sem área/cargo definidos (pessoa.area_id é obrigatório). Quando souber a área:\n${PESSOAS_PENDENTES.map((p) => `-- insert into pessoa (nome, nome_exibicao, area_id, status, perfil, aprovado_em) select ${sql(p.nome)}, ${sql(p.exibicao)}, id, 'ATIVO', 'PROFISSIONAL', now() from area where nome = '<ÁREA>';`).join('\n')}\n`;
// ensaio: limpeza + importação completas numa transação que termina em ROLLBACK
const ensaio = `begin;\n${corpoLimpeza}\n${corpoImportacao}\n${CONFERENCIA}\n\nrollback;\n`;

// ---------------------------------------------------------------- relatório de conferência
const fmt = (n) => n.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const porMes = {};
for (const l of linhas) {
  const m = l.dia.slice(0, 7);
  porMes[m] = porMes[m] ?? { linhas: 0, valor: 0 };
  porMes[m].linhas++;
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
const totalCancelado = linhas.filter((l) => l.status === 'Cancelado').reduce((s, l) => s + l.valor, 0);
const comCodigo = grupos.filter((g) => g.codigo && !g.chave.endsWith('#cancelada')).length;
const maiores = [...grupos].sort((a, b) => b.linhas.length - a.linhas.length).slice(0, 5);

const relatorio = `# Conferência da importação: planilha DEMANDAS HOUSE CRM, aba ${ABA}

Gerado por \`gerar-importacao.cjs\` a partir de \`${path.basename(ARQUIVO)}\`.

## Totais
| Item | Valor |
|---|---|
| Linhas da planilha | ${linhas.length} |
| **Demandas geradas** (linhas com o mesmo código BC viram uma só) | **${grupos.length}** (${comCodigo} por código, ${grupos.filter((g) => !g.codigo).length} sem código, ${grupos.filter((g) => g.chave.endsWith('#cancelada')).length} canceladas separadas) |
| Peças (uma por tipo de cada linha) | ${linhas.reduce((s, l) => s + l.pecas.length, 0)} |
| Atividades (uma por responsável de cada linha) | ${linhas.reduce((s, l) => s + l.responsaveis.length, 0)} |
| Participantes (pessoa x demanda) | ${grupos.reduce((s, g) => s + g.participantes.length, 0)} |
| Demandantes | ${demandantes.size} |
| Tipos de peça | ${tipos.size} |
| Soma do Valor na planilha | R$ ${fmt(totalValor)} |
| Soma das peças geradas (deve ser igual) | R$ ${fmt(totalPecas)} |
| Do total, em linhas canceladas (fora do valor gerado) | R$ ${fmt(totalCancelado)} |

Demandas com mais linhas repetidas: ${maiores.map((g) => `${g.codigo ?? 'linha ' + g.refRow} (${g.linhas.length} linhas)`).join('; ')}.

## Por mês (data da coluna Dia)
| Mês | Linhas | Valor |
|---|---|---|
${Object.entries(porMes).sort().map(([m, v]) => `| ${m} | ${v.linhas} | R$ ${fmt(v.valor)} |`).join('\n')}

## Por responsável da linha
| Responsável (planilha) | Pessoa no sistema | Linhas | Valor |
|---|---|---|---|
${Object.entries(porPessoa).sort((a, b) => b[1].valor - a[1].valor).map(([k, v]) => `| ${k} | ${PESSOAS[k]?.nome ?? '?'} (${PESSOAS[k]?.exibicao ?? ''}) | ${v.linhas} | R$ ${fmt(v.valor)} |`).join('\n')}

## Tipos de peça e preço unitário (derivado da planilha)
| Tipo | Área | Preço unitário |
|---|---|---|
${tiposLista.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')).map((t) => `| ${t.nome}${t.ativo ? '' : ' (inativo; só para linha que não reconcilia)'} | ${t.area} | R$ ${fmt(t.preco)} |`).join('\n')}

## Demandantes
${[...demandantes.entries()].sort((a, b) => a[0].localeCompare(b[0], 'pt-BR')).map(([n, d]) => `- ${n}${d.diretoria !== DIRETORIA ? ` (diretoria ${d.diretoria})` : ''}${d.observacao ? ` — ${d.observacao}` : ''}`).join('\n')}

## Pontos de atenção
- **Linhas que não reconciliam** (soma dos tipos x Valor): ${naoBatem.length}${naoBatem.length ? '\n' + naoBatem.map((x) => `  - ${x}`).join('\n') : ''}
- **Tipos sem preço deduzível**: ${semPreco.length ? semPreco.map((k) => nomesPorChave.get(k)).join('; ') : 'nenhum'}
- **Conflitos de preço**: ${conflitos.length ? conflitos.join('; ') : 'nenhum'}
- **Anomalias de dados**: ${anomalias.length}${anomalias.length ? '\n' + anomalias.map((x) => `  - ${x}`).join('\n') : ''}
- **Mesmo código com clientes diferentes** (vale o cliente mais frequente): ${conflitosCliente.length}${conflitosCliente.length ? '\n' + conflitosCliente.slice(0, 15).map((x) => `  - ${x}`).join('\n') : ''}${conflitosCliente.length > 15 ? `\n  - … e mais ${conflitosCliente.length - 15}` : ''}
- **Mesmo código com status diferentes** (vale o mais "aberto": em andamento, em aprovação, concluída): ${gruposMistos.length}${gruposMistos.length ? '\n' + gruposMistos.slice(0, 15).map((x) => `  - ${x}`).join('\n') : ''}${gruposMistos.length > 15 ? `\n  - … e mais ${gruposMistos.length - 15}` : ''}
- **Demandantes com o mesmo primeiro nome ainda separados** (decida se unifica): ${possiveisDuplicados.length ? possiveisDuplicados.map((v) => v.join(' / ')).join('; ') : 'nenhum'}
- **Horários** (Hora In/Out) não são importados: atividade só guarda a data.
- **Pessoas pendentes** (falta a área): ${PESSOAS_PENDENTES.map((p) => `${p.nome} (${p.exibicao})`).join(', ')}.

## Arquivos gerados e ordem de execução
1. \`00-limpar-dados-de-teste.sql\`: uso único; apaga dados de teste e **preserva as contas com login**.
2. \`01-carga-NN.sql\` (${arquivosCarga.length} arquivos, em ordem): carregam as tabelas de apoio \`imp_hc_*\`.
3. \`02-importar.sql\`: importa a partir das tabelas de apoio e as apaga no fim. Reprocessável.
4. \`03-ensaio-completo-com-rollback.sql\`: limpeza + importação completas terminando em ROLLBACK (não deixa nada no banco).
`;

// ---------------------------------------------------------------- conferência da carga (checksums)
// Compara o que está nas tabelas imp_hc_* com o que o gerador montou: pega erro de digitação/truncamento.
const crypto = require('crypto');
const md5 = (linhasTxt) => crypto.createHash('md5').update(linhasTxt.join(','), 'utf8').digest('hex');
const d2 = (n) => Number(n).toFixed(2);
const ck = {
  grupo: md5(grupos.map((g) => [g.id, g.refRow, d2(g.valor), g.status, g.codigo ?? '', g.cliente ?? '', g.criacao, g.linhas.join(', ')].join(':'))),
  linha: md5(linhas.map((l) => [l.n, l.grp, l.dia, l.prevista && l.prevista !== l.dia ? l.prevista : '', l.final && l.final !== l.dia ? l.final : '', l.descricao, l.responsaveis.join('|'), STATUS[l.status]].join(':'))),
  peca: md5(linhas.flatMap((l) => l.pecas.map((p, i) => [l.n, i + 1, idTipo.get(chaveTipo(p.tipo)), p.qtd, d2(p.unit)].join(':')))),
  part: md5(grupos.flatMap((g) => g.participantes.map((p) => ({ g: g.id, e: p.entrada, pr: p.principal, r: p.chave })))
    .sort((a, b) => a.g - b.g || a.e.localeCompare(b.e) || Number(b.pr) - Number(a.pr) || (a.r < b.r ? -1 : a.r > b.r ? 1 : 0))
    .map((p) => [p.g, p.r, p.pr ? 't' : 'f', p.e].join(':'))),
};
const conferirCarga = `-- 01-conferir-carga.sql: rode depois dos 01-carga-NN.sql. Todas as colunas devem ser "true".
select
  (select count(*) from imp_hc_grupo) = ${grupos.length} as grupos_qtd,
  (select count(*) from imp_hc_linha) = ${linhas.length} as linhas_qtd,
  (select count(*) from imp_hc_peca) = ${linhas.reduce((t, l) => t + l.pecas.length, 0)} as pecas_qtd,
  (select count(*) from imp_hc_part) = ${grupos.reduce((t, g) => t + g.participantes.length, 0)} as part_qtd,
  (select count(*) from imp_hc_tipo) = ${tiposLista.length} as tipos_qtd,
  (select count(*) from imp_hc_demandante) = ${demandantes.size} as demandantes_qtd,
  (select md5(string_agg(grp || ':' || ref_row || ':' || valor::text || ':' || status || ':' || coalesce(codigo, '') || ':' || coalesce(cliente, '') || ':' || criacao || ':' || linhas, ',' order by grp)) from imp_hc_grupo) = '${ck.grupo}' as grupos_md5,
  (select md5(string_agg(src_row || ':' || grp || ':' || dia || ':' || coalesce(prevista, '') || ':' || coalesce(final, '') || ':' || descricao || ':' || responsaveis || ':' || status, ',' order by src_row)) from imp_hc_linha) = '${ck.linha}' as linhas_md5,
  (select md5(string_agg(src_row || ':' || ordem || ':' || tipo || ':' || qtd || ':' || unit::text, ',' order by src_row, ordem)) from imp_hc_peca) = '${ck.peca}' as pecas_md5,
  (select md5(string_agg(grp || ':' || resp || ':' || case when principal then 't' else 'f' end || ':' || entrada, ',' order by grp, entrada, principal desc, resp collate "C")) from imp_hc_part) = '${ck.part}' as part_md5;
`;

const saida = path.join(__dirname, 'saida');
fs.rmSync(saida, { recursive: true, force: true });
fs.mkdirSync(saida, { recursive: true });
fs.writeFileSync(path.join(saida, '00-limpar-dados-de-teste.sql'), limpeza);
arquivosCarga.forEach((c, i) => fs.writeFileSync(path.join(saida, `01-carga-${String(i + 1).padStart(2, '0')}.sql`), c));
fs.writeFileSync(path.join(saida, '01-conferir-carga.sql'), conferirCarga);
fs.writeFileSync(path.join(saida, '02-importar.sql'), importacao);
fs.writeFileSync(path.join(saida, '03-ensaio-completo-com-rollback.sql'), ensaio);
fs.writeFileSync(path.join(saida, 'relatorio-conferencia.md'), relatorio);
console.log(`Linhas ${linhas.length} -> demandas ${grupos.length} | peças ${linhas.reduce((s, l) => s + l.pecas.length, 0)} | tipos ${tipos.size} | demandantes ${demandantes.size}`);
console.log(`Soma planilha ${fmt(totalValor)} x peças ${fmt(totalPecas)} | cargas ${arquivosCarga.length} (${arquivosCarga.map((c) => (c.length / 1024).toFixed(0) + ' KB').join(', ')}) | 02: ${(importacao.length / 1024).toFixed(0)} KB`);
console.log(`Conflitos de cliente ${conflitosCliente.length} | status mistos ${gruposMistos.length} | anomalias ${anomalias.length}`);
