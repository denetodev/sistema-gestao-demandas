#!/usr/bin/env node
/**
 * Gera o SQL de importação da planilha "Demandas Multimídia 26-UGR" (abas 1T, 2T e 3T 2026) para o Supabase.
 *
 *   node database/importacao/gerar-importacao-ugr.cjs "C:/caminho/Demandas Multimídia 26-UGR.xlsx"
 *
 * Saída em database/importacao/saida-ugr/ (ignorada pelo git: tem dados reais de pessoas):
 *   01-carga-NN.sql, 01-conferir-carga.sql, 02-importar.sql, 03-ensaio-com-rollback.sql, relatorio-conferencia.md
 *
 * "Multimídia" está para a UGR como "HOUSE" está para o CRM: é o nome informal da equipe; UGR é a diretoria do BB.
 * Regras (ver relatorio-conferencia.md):
 *   - linhas com a mesma descrição são a mesma demanda lançada por profissionais diferentes (vídeo + operação...):
 *     viram UMA demanda (canceladas não se misturam às ativas)
 *   - "Projeto" lista serviços da tabela de valores (aba DADOS); vira uma peça por serviço; Valor = QTD x preços
 *   - cada responsável vira participante e ganha uma atividade "Produção"; a peça é do primeiro responsável da linha
 *   - Cliente + Diretoria da planilha viram Demandante (PESSOA) naquela diretoria; a demanda é da diretoria UGR
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const ARQUIVO = process.argv[2];
if (!ARQUIVO || !fs.existsSync(ARQUIVO)) {
  console.error('Uso: node gerar-importacao-ugr.cjs <planilha.xlsx>');
  process.exit(1);
}
const SS = 'planilha-multimidia-ugr';
const DIRETORIA = 'UGR';
const DIRETORIA_ANTIGA = 'UGR — Gerenciamento de Redes'; // nome do seed, renomeado para UGR
const TIPO_ATIVIDADE = 'Produção';
const ABAS = [
  { nome: '1T 2026', cod: 1 },
  { nome: '2T 2026', cod: 2 },
  { nome: '3T 2026', cod: 3 },
]; // a planilha chama as abas "1T 2026"... (a 1T pode vir sem o ano em versões antigas)

// ---------------------------------------------------------------- configuração do domínio
const AREA_POR_TAG = { Videomakers: 'Videomaker', Operadores: 'Operador', Designers: 'Design', Redatores: 'Redação', Editores: 'Edição', Motion: 'Motion' };
const CARGO_POR_TAG = { Videomakers: 'Videomaker', Operadores: 'Operador', Designers: 'Designer', Redatores: 'Redator', Editores: 'Editor', Motion: 'Motion Designer' };
const STATUS = { Concluído: 'CONCLUIDA', 'Não Iniciada': 'NAO_INICIADA', 'Em Andamento': 'EM_ANDAMENTO', Aprovação: 'EM_APROVACAO', Cancelado: 'CANCELADA' };
const PRECEDENCIA_STATUS = ['EM_ANDAMENTO', 'EM_APROVACAO', 'NAO_INICIADA', 'CONCLUIDA', 'CANCELADA'];

// equipe atual (já cadastrada no banco) e quem entra agora. A chave é como a planilha escreve o nome.
const ATIVOS_EXISTENTES = {
  Guilherme: 'Guilherme Vinicius Targino Belo', Mauro: 'Mauro Cesar da Silva', Alessandro: 'Alessandro Fernandes Paciano',
  Stela: 'Stela Marie Linhares Rodrigues', Julio: 'Julio Cesar Nunes Damasceno', Thiago: 'Thiago Otavio Gomes de Oliveira',
  Klebson: 'Klebson de Oliveira', Daniel: 'Daniel Lima dos Santos',
};
const PESSOAS = {
  Raul: { nome: 'Raul Carlos Dantas Nogueira', exibicao: 'Raul', tag: 'Designers', status: 'ATIVO' },
};
// Quem aparece na planilha e já saiu da equipe (nome completo desconhecido: entram como estão na planilha, INATIVOS).
// A área vem da tag que a pessoa mais usou. Guilherme Otone != Guilherme Vinicius Targino Belo (confirmado pelo Neto).
const EX_EQUIPE = {
  'Guilherme Otone': { nome: 'Guilherme Otone', exibicao: 'Guilherme Otone' },
  Isadora: {}, Wagner: {}, Aurélio: {}, Nicolas: {}, Natan: {}, Nayara: {}, Wellington: {}, Camila: {},
  Lukas: {}, Weslley: {}, Rebecca: {}, Wilhan: {}, Caio: {}, Luca: {}, Paulo: {}, 'Hugo Dourado': {}, Helcio: {}, Raquel: {},
};
// clientes escritos de mais de um jeito
const CLIENTES = { JG: 'João Gabriel (JG)' };

// ---------------------------------------------------------------- leitura do xlsx
function extrair(arquivo, destino) {
  try {
    execFileSync('unzip', ['-oq', arquivo, '-d', destino]);
  } catch {
    execFileSync('powershell', ['-NoProfile', '-Command', `Add-Type -AssemblyName System.IO.Compression.FileSystem; [IO.Compression.ZipFile]::ExtractToDirectory('${arquivo.replace(/'/g, "''")}', '${destino.replace(/'/g, "''")}')`]);
  }
}

function lerAbas(arquivo, nomes) {
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
  const resultado = {};
  for (const nomeAba of nomes) {
    const aba = [...wb.matchAll(/<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"/g)].find((m) => dec(m[1]) === nomeAba);
    if (!aba) throw new Error(`Aba "${nomeAba}" não encontrada`);
    const alvo = rels.match(new RegExp(`<Relationship [^>]*Id="${aba[2]}"[^>]*Target="([^"]+)"|<Relationship [^>]*Target="([^"]+)"[^>]*Id="${aba[2]}"`));
    const xml = ler('xl/' + (alvo[1] ?? alvo[2]).replace(/^\/?(xl\/)?/, ''));
    const linhas = [];
    for (const r of xml.matchAll(/<row [^>]*r="(\d+)"[^>]*>([\s\S]*?)<\/row>/g)) {
      const cel = { n: Number(r[1]) };
      for (const c of r[2].matchAll(/<c ([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
        const ref = c[1].match(/r="([A-Z]+)\d+"/)[1];
        const t = (c[1].match(/ t="([^"]+)"/) || [])[1];
        const v = (c[2] || '').match(/<v>([\s\S]*?)<\/v>/);
        const inl = (c[2] || '').match(/<is>[\s\S]*?<t[^>]*>([\s\S]*?)<\/t>/);
        let valor = null;
        if (t === 's' && v) valor = compartilhadas[Number(v[1])];
        else if (inl) valor = dec(inl[1]).normalize('NFC');
        else if (v) valor = dec(v[1]);
        if (valor !== null && valor !== '') cel[ref] = valor;
      }
      linhas.push(cel);
    }
    resultado[nomeAba] = linhas;
  }
  fs.rmSync(dir, { recursive: true, force: true });
  return resultado;
}

const MESES = { Jan: 1, Fev: 2, Mar: 3, Abr: 4, Mai: 5, Jun: 6, Jul: 7, Ago: 8, Set: 9, Out: 10, Nov: 11, Dez: 12 };
const serial = (v) => new Date(Date.UTC(1899, 11, 30) + Number(v) * 86400000).toISOString().slice(0, 10);
const arred = (n) => Math.round(n * 100) / 100;
const sql = (s) => (s === null || s === undefined ? 'null' : `'${String(s).replace(/'/g, "''")}'`);
const limpo = (s) => String(s ?? '').normalize('NFC').replace(/\s+/g, ' ').trim();
const preencheu = (s) => limpo(s) !== '';
/** "R$ 1.000,00", "R$800,00" ou número puro ("1000.0"). */
const dinheiro = (v) => {
  if (v === undefined || v === null || v === '') return null;
  const s = String(v);
  const n = /R\$|,/.test(s) ? Number(s.replace(/R\$|\s|\./g, '').replace(',', '.')) : Number(s);
  return Number.isNaN(n) ? null : n;
};

const bruto = lerAbas(ARQUIVO, [...ABAS.map((a) => a.nome), 'DADOS']);

// ---------------------------------------------------------------- tabela de valores (aba DADOS)
const PARES = { Designers: ['BD', 'BK'], Redatores: ['BS', 'BZ'], Operadores: ['CH', 'CO'], Videomakers: ['CW', 'DD'], Editores: ['DL', 'DS'], Motion: ['EA', 'EH'] };
const chaveNome = (s) => limpo(s).replace(/"/g, '').toLowerCase();
const tabela = {}; // tag -> Map(nome normalizado -> { nome, preco })
for (const [tag, [cNome, cPreco]] of Object.entries(PARES)) {
  tabela[tag] = new Map();
  for (const r of bruto.DADOS.filter((x) => x.n >= 4 && x[cNome] && x[cNome] !== 'a')) {
    const preco = dinheiro(r[cPreco]);
    if (preco !== null) tabela[tag].set(chaveNome(r[cNome]), { nome: limpo(r[cNome]), preco });
  }
}
const precoTabela = (tag, nome) => {
  const k = chaveNome(nome);
  if (tabela[tag]?.has(k)) return tabela[tag].get(k).preco;
  for (const t of Object.values(tabela)) if (t.has(k)) return t.get(k).preco;
  return null;
};

// ---------------------------------------------------------------- linhas das abas
const anomalias = [];
const linhas = [];
for (const aba of ABAS) {
  for (const r of bruto[aba.nome].filter((x) => x.n >= 8 && x.E && x.C)) {
    const id = aba.cod * 10000 + r.n;
    const tags = (r.Y || '').split(',').map((x) => x.trim()).filter(Boolean);
    const tag = tags[0];
    let dia = Number(r.C) > 40000 ? serial(r.C) : null;
    // em algumas linhas o "Dia" é só o número do dia; o mês vem na coluna Mês
    if (!dia && Number(r.C) >= 1 && Number(r.C) <= 31 && MESES[limpo(r.D)]) dia = `2026-${String(MESES[limpo(r.D)]).padStart(2, '0')}-${String(Number(r.C)).padStart(2, '0')}`;
    const l = {
      id, aba: aba.nome, n: r.n, dia,
      prevista: r.M && Number(r.M) > 40000 ? serial(r.M) : null,
      final: r.O && Number(r.O) > 40000 ? serial(r.O) : null,
      qtd: Number(r.B),
      descricao: limpo(r.E),
      diretoria: preencheu(r.F) ? limpo(r.F) : null,
      cliente: preencheu(r.G) ? (CLIENTES[limpo(r.G)] ?? limpo(r.G)) : null,
      responsaveis: (r.Q || '').split(',').map((x) => x.trim()).filter(Boolean),
      tags, tag, statusPlanilha: r.AG,
      projeto: limpo(r.AJ),
      valor: dinheiro(r.AO),
    };
    if (!l.dia) {
      const alt = l.final ?? l.prevista;
      anomalias.push(`${aba.nome} linha ${r.n}: "Dia" inválido (${r.C}); ${alt ? `usei ${alt}` : 'linha ignorada'} — ${l.descricao}`);
      if (!alt) continue;
      l.dia = alt;
    }
    if (!STATUS[l.statusPlanilha]) {
      anomalias.push(`${aba.nome} linha ${r.n}: status "${l.statusPlanilha ?? '(vazio)'}"; tratei como CONCLUIDA — ${l.descricao}`);
      l.statusPlanilha = 'Concluído';
    }
    l.status = STATUS[l.statusPlanilha];
    if (!AREA_POR_TAG[l.tag]) {
      anomalias.push(`${aba.nome} linha ${r.n}: tag desconhecida "${r.Y ?? ''}"; tratei como Videomakers`);
      l.tag = 'Videomakers';
    }
    if (!(l.qtd > 0) || !Number.isInteger(l.qtd)) {
      anomalias.push(`${aba.nome} linha ${r.n}: QTD inválida ${r.B}; usei 1`);
      l.qtd = 1;
    }
    linhas.push(l);
  }
}
linhas.sort((a, b) => a.dia.localeCompare(b.dia) || a.id - b.id);

// ---------------------------------------------------------------- pessoas
const todosNomes = new Set(linhas.flatMap((l) => l.responsaveis));
const tagPorPessoa = {};
for (const l of linhas) for (const r of l.responsaveis) {
  tagPorPessoa[r] = tagPorPessoa[r] ?? {};
  for (const t of l.tags) tagPorPessoa[r][t] = (tagPorPessoa[r][t] ?? 0) + 1;
}
const tagDominante = (nome) => Object.entries(tagPorPessoa[nome] ?? {}).sort((a, b) => b[1] - a[1])[0]?.[0] ?? 'Operadores';
const respMapa = new Map(); // chave da planilha -> nome completo no sistema
const pessoasNovas = []; // { nome, exibicao, area, cargo, status }
for (const nome of [...todosNomes].sort()) {
  if (ATIVOS_EXISTENTES[nome]) {
    respMapa.set(nome, ATIVOS_EXISTENTES[nome]);
  } else if (PESSOAS[nome]) {
    const p = PESSOAS[nome];
    respMapa.set(nome, p.nome);
    pessoasNovas.push({ nome: p.nome, exibicao: p.exibicao, area: AREA_POR_TAG[p.tag], cargo: CARGO_POR_TAG[p.tag], status: p.status });
  } else if (EX_EQUIPE[nome]) {
    const p = EX_EQUIPE[nome];
    const tag = tagDominante(nome);
    respMapa.set(nome, p.nome ?? nome);
    pessoasNovas.push({ nome: p.nome ?? nome, exibicao: p.exibicao ?? nome, area: AREA_POR_TAG[tag], cargo: CARGO_POR_TAG[tag], status: 'INATIVO' });
  } else {
    anomalias.push(`Responsável sem mapeamento: "${nome}" (as linhas dele ficam sem esse responsável)`);
  }
}
for (const l of linhas) l.responsaveis = l.responsaveis.filter((r) => respMapa.has(r));

// ---------------------------------------------------------------- serviços (peças) e preços
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
/** A peça pertence à área da tag principal da linha: o mesmo nome em áreas diferentes são tipos diferentes. */
const chaveTipo = (tag, nome) => `${tag}|${nome.toLowerCase()}`;
const nomesPorChave = new Map();
for (const l of linhas) {
  l.tipos = dividirTipos(l.projeto).map(normalizarTipo);
  for (const t of l.tipos) {
    t.chave = chaveTipo(l.tag, t.nome);
    if (!nomesPorChave.has(t.chave)) nomesPorChave.set(t.chave, { tag: l.tag, nome: t.nome });
  }
}

// 1) preço mais frequente nas linhas de um serviço só; 2) tabela de valores; 3) combinações com um serviço desconhecido
const preco = new Map();
const origemPreco = new Map(); // chave -> 'planilha' | 'tabela' | 'combinacao'
const divergeTabela = [];
const unidades = new Map();
for (const l of linhas) {
  if (l.tipos.length === 1 && l.tipos[0].mult === 1 && l.valor !== null && l.valor > 0) {
    const k = l.tipos[0].chave;
    const u = arred(l.valor / l.qtd);
    const m = unidades.get(k) ?? new Map();
    m.set(u, (m.get(u) ?? 0) + 1);
    unidades.set(k, m);
  }
}
for (const [k, m] of unidades) {
  const [u, qtd] = [...m.entries()].sort((a, b) => b[1] - a[1])[0];
  preco.set(k, u);
  origemPreco.set(k, 'planilha');
  const { tag, nome } = nomesPorChave.get(k);
  const t = precoTabela(tag, nome);
  if (t !== null && Math.abs(t - u) > 0.011) divergeTabela.push(`${nome} (${AREA_POR_TAG[tag]}): planilha R$ ${u} em ${qtd} linha(s), tabela R$ ${t} — vale a planilha`);
}
for (const [k, { tag, nome }] of nomesPorChave) {
  if (preco.has(k)) continue;
  const t = precoTabela(tag, nome);
  if (t !== null) {
    preco.set(k, t);
    origemPreco.set(k, 'tabela');
  }
}
let mudou = true;
while (mudou) {
  mudou = false;
  for (const l of linhas) {
    if (l.valor === null) continue;
    const desconhecidos = l.tipos.filter((t) => !preco.has(t.chave));
    if (desconhecidos.length !== 1) continue;
    const conhecido = l.tipos.filter((t) => preco.has(t.chave)).reduce((s, t) => s + preco.get(t.chave) * t.mult, 0);
    const d = desconhecidos[0];
    const u = arred((l.valor / l.qtd - conhecido) / d.mult);
    if (u > 0) {
      preco.set(d.chave, u);
      origemPreco.set(d.chave, 'combinacao');
      mudou = true;
    }
  }
}

const naoBatem = [];
const semValor = [];
for (const l of linhas) {
  const resolvidos = l.tipos.length > 0 && l.tipos.every((t) => preco.has(t.chave));
  const soma = resolvidos ? arred(l.tipos.reduce((s, t) => s + preco.get(t.chave) * t.mult, 0) * l.qtd) : null;
  if (l.valor === null && resolvidos) {
    l.valor = soma;
    semValor.push(`${l.aba} linha ${l.n}: sem Valor; calculei ${soma} pela tabela — ${l.projeto}`);
  }
  if (l.valor === null) l.valor = 0;
  if (!l.tipos.length) {
    // sem "Projeto": com valor vira pacote; sem valor não gera peça
    l.pecas = l.valor > 0 ? [{ tag: l.tag, tipo: 'Pacote (valor da planilha)', qtd: l.qtd, unit: arred(l.valor / l.qtd), pacote: true }] : [];
    if (l.valor > 0) naoBatem.push(`${l.aba} linha ${l.n}: sem Projeto, Valor ${l.valor} — ${l.descricao}`);
  } else if (resolvidos && Math.abs(soma - l.valor) <= 0.05) {
    l.pecas = l.tipos.map((t) => ({ tag: l.tag, tipo: t.nome, chave: t.chave, qtd: l.qtd * t.mult, unit: preco.get(t.chave) }));
  } else {
    l.pecas = [{ tag: l.tag, tipo: 'Pacote (valor da planilha)', qtd: l.qtd, unit: arred(l.valor / l.qtd), pacote: true }];
    naoBatem.push(`${l.aba} linha ${l.n}: "${l.projeto.slice(0, 70)}" QTD ${l.qtd}, soma dos serviços ${soma ?? 'sem preço'} x Valor ${l.valor}`);
  }
  l.totalPecas = arred(l.pecas.reduce((s, p) => s + p.qtd * p.unit, 0));
}

const tipos = new Map(); // chave -> { nome, area, preco, ativo, origem }
for (const l of linhas) for (const p of l.pecas) {
  const k = p.pacote ? chaveTipo(p.tag, p.tipo) : p.chave;
  if (tipos.has(k)) continue;
  tipos.set(k, { nome: p.tipo, area: AREA_POR_TAG[p.tag], preco: p.pacote ? 0.01 : preco.get(k), ativo: !p.pacote, origem: p.pacote ? 'pacote' : origemPreco.get(k) });
}
const tiposLista = [...tipos.values()].sort((a, b) => a.area.localeCompare(b.area) || a.nome.localeCompare(b.nome, 'pt-BR'));
tiposLista.forEach((t, i) => (t.id = i + 1));
const idTipo = new Map([...tipos.entries()].map(([k, t]) => [k, t.id]));
const idDaPeca = (p) => idTipo.get(p.pacote ? chaveTipo(p.tag, p.tipo) : p.chave);

// ---------------------------------------------------------------- demandas (linhas com a mesma descrição)
const chaveGrupo = (l) => `${l.descricao.toLowerCase()}${l.status === 'CANCELADA' ? '#cancelada' : ''}`;
const gruposPorChave = new Map();
for (const l of linhas) {
  const k = chaveGrupo(l);
  if (!gruposPorChave.has(k)) gruposPorChave.set(k, []);
  gruposPorChave.get(k).push(l);
}
const moda = (valores) => {
  const c = new Map();
  valores.filter(Boolean).forEach((v) => c.set(v, (c.get(v) ?? 0) + 1));
  return [...c.entries()].sort((a, b) => b[1] - a[1]);
};
const conflitosCliente = [];
const gruposMistos = [];
const grupos = [...gruposPorChave.values()]
  .map((ls) => {
    const primeira = ls[0];
    const status = PRECEDENCIA_STATUS.find((s) => ls.some((l) => l.status === s));
    const clientes = moda(ls.map((l) => l.cliente));
    if (clientes.length > 1) conflitosCliente.push(`${primeira.descricao}: ${clientes.map(([c, n]) => `${c} (${n})`).join(' / ')}`);
    const distintos = new Set(ls.map((l) => l.status));
    if (distintos.size > 1) gruposMistos.push(`${primeira.descricao}: ${[...distintos].join(' + ')} -> ${status}`);
    const cliente = clientes[0]?.[0] ?? null;
    const dirSol = moda(ls.filter((l) => !cliente || l.cliente === cliente).map((l) => l.diretoria))[0]?.[0] ?? moda(ls.map((l) => l.diretoria))[0]?.[0] ?? null;
    const participantes = new Map();
    for (const l of ls) for (const r of l.responsaveis) if (!participantes.has(r)) participantes.set(r, { chave: r, entrada: l.dia, principal: participantes.size === 0 });
    return {
      ref: primeira, refId: primeira.id, aba: primeira.aba, refRow: primeira.n,
      titulo: primeira.descricao, cliente, dirSol, status,
      criacao: ls[0].dia,
      prazo: ls.map((l) => l.prevista ?? l.dia).sort().at(-1),
      entrega: ls.map((l) => l.final ?? l.dia).sort().at(-1),
      valor: arred(ls.reduce((s, l) => s + l.valor, 0)),
      linhas: ls.map((l) => `${l.aba.slice(0, 2)}:${l.n}`).join(', '),
      equipe: [...new Set(ls.flatMap((l) => l.tags))].map((t) => AREA_POR_TAG[t] ?? t).join(', '),
      participantes: [...participantes.values()],
      ls,
    };
  })
  .sort((a, b) => a.criacao.localeCompare(b.criacao) || a.refId - b.refId);
grupos.forEach((g, i) => {
  g.id = i + 1;
  g.ls.forEach((l) => (l.grp = g.id));
});

// ---------------------------------------------------------------- demandantes e diretorias
const demandantes = new Map(); // nome -> { nome, diretoria }: a diretoria é a que mais aparece nas linhas dele
const dirsPorCliente = new Map();
for (const l of linhas) if (l.cliente && l.diretoria) dirsPorCliente.set(l.cliente, [...(dirsPorCliente.get(l.cliente) ?? []), l.diretoria]);
for (const g of grupos) {
  if (!g.cliente || demandantes.has(g.cliente)) continue;
  const dirs = dirsPorCliente.get(g.cliente) ?? [];
  demandantes.set(g.cliente, { nome: g.cliente, diretoria: moda(dirs)[0]?.[0] ?? DIRETORIA, outras: [...new Set(dirs)] });
}
const primeirosNomes = new Map();
for (const d of demandantes.values()) {
  const k = d.nome.split(' ')[0].toLowerCase();
  primeirosNomes.set(k, [...(primeirosNomes.get(k) ?? []), d.nome]);
}
const possiveisDuplicados = [...primeirosNomes.values()].filter((v) => v.length > 1);
const diretoriasUsadas = [...new Set([DIRETORIA, ...[...demandantes.values()].map((d) => d.diretoria)])];
// diretorias que só diferem na caixa viram problema de nome único: sinaliza
const porCaixa = new Map();
for (const d of diretoriasUsadas) porCaixa.set(d.toLowerCase(), [...(porCaixa.get(d.toLowerCase()) ?? []), d]);
const diretoriasParecidas = [...porCaixa.values()].filter((v) => v.length > 1);

// ---------------------------------------------------------------- SQL: carga nas tabelas de apoio (imp_ugr_*)
const instrucoes = [];
const lote = (cabecalho, valores, tamanho = 120) => {
  for (let i = 0; i < valores.length; i += tamanho) instrucoes.push(`${cabecalho}\n${valores.slice(i, i + tamanho).join(',\n')};`);
};
const areasNecessarias = [...new Set([...tiposLista.map((t) => t.area), ...pessoasNovas.map((p) => p.area)])].sort();

lote('insert into imp_ugr_tipo values', tiposLista.map((t) => `(${t.id}, ${sql(t.nome)}, ${sql(t.area)}, ${t.preco}, ${t.ativo})`));
lote('insert into imp_ugr_demandante values', [...demandantes.values()].map((d) => `(${sql(d.nome)}, ${sql(d.diretoria)})`));
lote('insert into imp_ugr_pessoa values', pessoasNovas.map((p) => `(${sql(p.nome)}, ${sql(p.exibicao)}, ${sql(p.area)}, ${sql(p.cargo)}, ${sql(p.status)})`));
lote('insert into imp_ugr_resp values', [...respMapa.entries()].map(([c, n]) => `(${sql(c)}, ${sql(n)})`));
lote('insert into imp_ugr_grupo values', grupos.map((g) =>
  `(${g.id}, ${g.refId}, ${sql(g.aba)}, ${g.refRow}, ${sql(g.titulo)}, ${sql(g.cliente)}, ${sql(g.dirSol)}, ${sql(g.status)}, ${sql(g.criacao)}, ${sql(g.prazo)}, ${sql(g.entrega)}, ${g.valor}, ${sql(g.linhas)}, ${sql(g.equipe)})`), 120);
lote('insert into imp_ugr_part values', grupos.flatMap((g) => g.participantes.map((p) => `(${g.id}, ${sql(p.chave)}, ${p.principal}, ${sql(p.entrada)})`)), 250);
lote('insert into imp_ugr_linha values', linhas.map((l) =>
  `(${l.id}, ${l.grp}, ${sql(l.dia)}, ${sql(l.prevista && l.prevista !== l.dia ? l.prevista : null)}, ${sql(l.final && l.final !== l.dia ? l.final : null)}, ${sql(l.responsaveis.join('|'))})`), 150);
lote('insert into imp_ugr_peca values', linhas.flatMap((l) => l.pecas.map((p, i) => `(${l.id}, ${i + 1}, ${idDaPeca(p)}, ${p.qtd}, ${p.unit})`)), 250);

const TABELAS = ['linha', 'peca', 'part', 'grupo', 'tipo', 'demandante', 'pessoa', 'resp'];
const DDL_APOIO = `-- Tabelas de apoio da importação UGR (apagadas no fim do 02-importar.sql)
drop table if exists ${TABELAS.map((t) => `imp_ugr_${t}`).join(', ')};
create table imp_ugr_tipo (id int primary key, nome text not null, area text not null, preco numeric(14,2) not null, ativo boolean not null);
create table imp_ugr_demandante (nome text primary key, diretoria text not null);
create table imp_ugr_pessoa (nome text primary key, exibicao text not null, area text not null, cargo text not null, status text not null);
create table imp_ugr_resp (chave text primary key, nome text not null);
create table imp_ugr_grupo (grp int primary key, ref_id int not null unique, aba text not null, ref_row int not null, titulo text not null, cliente text, dir_sol text,
  status text not null, criacao text not null, prazo text not null, entrega text not null, valor numeric(14,2) not null, linhas text not null, equipe text not null);
create table imp_ugr_part (grp int not null, resp text not null, principal boolean not null, entrada text not null);
create table imp_ugr_linha (id int primary key, grp int not null, dia text not null, prevista text, final text, responsaveis text not null);
create table imp_ugr_peca (lin int not null, ordem int not null, tipo int not null, qtd int not null, unit numeric(14,2) not null);
-- RLS ligado (sem policies): dados reais não podem ficar expostos pela anon key
${TABELAS.map((t) => `alter table imp_ugr_${t} enable row level security;`).join('\n')}
`;

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
const areaUgr = (alias) => `join area ${alias} on ${alias}.diretoria_id = (select id from diretoria where nome = '${DIRETORIA}') and ${alias}.nome`;
const corpoImportacao = `-- 02-importar.sql  (REPROCESSÁVEL; gerado por gerar-importacao-ugr.cjs)
-- Planilha Multimídia UGR 2026 (abas 1T, 2T, 3T): ${linhas.length} linhas -> ${grupos.length} demandas (mesma descrição = mesma demanda).
-- Reprocessar apaga só o que este script importou antes (source_system = '${SS}').
-- Pré-requisito: tabelas imp_ugr_* carregadas pelos arquivos 01-carga-*.sql.

-- 1. remove importação anterior
delete from evidencia where atividade_id in (select a.id from atividade a join demanda d on d.id = a.demanda_id where d.source_system = '${SS}')
                         or peca_id in (select p.id from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SS}');
delete from atividade where demanda_id in (select id from demanda where source_system = '${SS}');
delete from peca where demanda_id in (select id from demanda where source_system = '${SS}');
delete from pessoa_demanda where demanda_id in (select id from demanda where source_system = '${SS}');
delete from demanda where source_system = '${SS}';

-- 2. diretoria UGR (o seed chamava de "${DIRETORIA_ANTIGA}") e as diretorias dos demandantes
update diretoria set nome = '${DIRETORIA}' where nome = '${DIRETORIA_ANTIGA}' and not exists (select 1 from diretoria where nome = '${DIRETORIA}');
insert into diretoria (nome)
select x.nome from (values ${diretoriasUsadas.map((d) => `(${sql(d)})`).join(', ')}) as x(nome)
 where not exists (select 1 from diretoria d where d.nome = x.nome);

-- 3. áreas da UGR
insert into area (diretoria_id, nome)
select (select id from diretoria where nome = '${DIRETORIA}'), x.nome
  from (values ${areasNecessarias.map((a) => `(${sql(a)})`).join(', ')}) as x(nome)
 where not exists (select 1 from area a where a.diretoria_id = (select id from diretoria where nome = '${DIRETORIA}') and a.nome = x.nome);

-- 4. pessoas novas (sem login e sem e-mail); quem saiu da equipe entra INATIVO
insert into pessoa (nome, nome_exibicao, area_id, cargo_id, status, perfil, aprovado_em)
select i.nome, i.exibicao, a.id, c.id, i.status, 'PROFISSIONAL', now()
  from imp_ugr_pessoa i
  ${areaUgr('a')} = i.area
  join cargo c on c.nome = i.cargo
 where not exists (select 1 from pessoa p where p.nome = i.nome);

-- 5. demandantes (todos são pessoas; a diretoria é a que mais aparece nas linhas dele)
insert into demandante (nome, tipo, diretoria_id, ativo)
select d.nome, 'PESSOA', dir.id, true
  from imp_ugr_demandante d join diretoria dir on dir.nome = d.diretoria
 where not exists (select 1 from demandante x where x.nome = d.nome);

-- 6. serviços da tabela de valores viram tipos de peça da área da UGR
insert into tipo_peca (nome, area_id, valor_referencia, ativo)
select t.nome, a.id, t.preco, t.ativo
  from imp_ugr_tipo t ${areaUgr('a')} = t.area
 where not exists (select 1 from tipo_peca x where x.area_id = a.id and x.nome = t.nome);

insert into tipo_atividade (nome, ativo)
select '${TIPO_ATIVIDADE}', true where not exists (select 1 from tipo_atividade where nome = '${TIPO_ATIVIDADE}');

-- 7. demandas: título = descrição; a diretoria da linha vai nas observações como "solicitante"
insert into demanda (titulo, codigo, diretoria_id, demandante_id, prioridade, status, data_criacao, data_prazo,
                     data_entrega_real, valor, observacoes, source_system, source_sheet, source_row, imported_at)
select left(g.titulo, 220), null, (select id from diretoria where nome = '${DIRETORIA}'),
       (select x.id from demandante x where x.nome = g.cliente limit 1),
       'NORMAL', g.status, g.criacao::date, g.prazo::date, case when g.status = 'CONCLUIDA' then g.entrega::date end, g.valor,
       'Importada da planilha Multimídia UGR (linhas ' || g.linhas || '). Equipe: ' || g.equipe || '.'
         || coalesce(' Diretoria solicitante: ' || g.dir_sol || '.', ''),
       '${SS}', g.aba, g.ref_row, now()
  from imp_ugr_grupo g;

-- 8. participantes: o responsável da linha mais antiga é o principal
insert into pessoa_demanda (pessoa_id, demanda_id, papel, data_entrada)
select p.id, d.id, case when pt.principal then 'RESPONSAVEL_PRINCIPAL' else 'PARTICIPANTE' end, pt.entrada::date
  from imp_ugr_part pt
  join imp_ugr_grupo g on g.grp = pt.grp
  join demanda d on d.source_system = '${SS}' and d.source_sheet = g.aba and d.source_row = g.ref_row
  join imp_ugr_resp m on m.chave = pt.resp
  join pessoa p on p.nome = m.nome;

-- 9. peças: uma por serviço de cada linha, com o valor unitário congelado; a peça é do primeiro responsável da linha
insert into peca (demanda_id, tipo_peca_id, nome, descricao, quantidade, valor_unitario, pessoa_id, data_entrega)
select d.id, tp.id, left(t.nome, 180), left(g.titulo, 500), i.qtd, i.unit,
       (select p.id from pessoa p join imp_ugr_resp m on m.nome = p.nome where m.chave = split_part(l.responsaveis, '|', 1)),
       l.dia::date
  from imp_ugr_peca i
  join imp_ugr_linha l on l.id = i.lin
  join imp_ugr_grupo g on g.grp = l.grp
  join demanda d on d.source_system = '${SS}' and d.source_sheet = g.aba and d.source_row = g.ref_row
  join imp_ugr_tipo t on t.id = i.tipo
  ${areaUgr('a')} = t.area
  join tipo_peca tp on tp.area_id = a.id and tp.nome = t.nome;

-- 10. atividades: uma "${TIPO_ATIVIDADE}" por responsável de cada linha, no dia da linha (linha sem responsável não gera atividade)
insert into atividade (demanda_id, tipo_atividade_id, pessoa_id, descricao, data_realizacao)
select d.id, (select id from tipo_atividade where nome = '${TIPO_ATIVIDADE}'), p.id, left(g.titulo, 500), l.dia::date
  from imp_ugr_linha l
  join imp_ugr_grupo g on g.grp = l.grp
  join demanda d on d.source_system = '${SS}' and d.source_sheet = g.aba and d.source_row = g.ref_row
  cross join lateral unnest(string_to_array(l.responsaveis, '|')) as r(chave)
  join imp_ugr_resp m on m.chave = r.chave
  join pessoa p on p.nome = m.nome;
`;
const CONFERENCIA = `select (select count(*) from demanda where source_system = '${SS}') as demandas,
       (select count(*) from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SS}') as pecas,
       (select count(*) from atividade a join demanda d on d.id = a.demanda_id where d.source_system = '${SS}') as atividades,
       (select count(*) from pessoa_demanda pd join demanda d on d.id = pd.demanda_id where d.source_system = '${SS}') as participantes,
       (select round(sum(p.quantidade * p.valor_unitario), 2) from peca p join demanda d on d.id = p.demanda_id where d.source_system = '${SS}') as soma_pecas,
       (select round(sum(valor), 2) from demanda where source_system = '${SS}') as soma_valor_demandas,
       (select count(*) from demanda where source_system = '${SS}' and demandante_id is null) as sem_demandante,
       (select count(*) from demanda) as demandas_total, (select count(*) from demandante) as demandantes,
       (select count(*) from tipo_peca) as tipos_peca, (select count(*) from pessoa) as pessoas;`;
const importacao = `begin;\n${corpoImportacao}\ndrop table ${TABELAS.map((t) => `imp_ugr_${t}`).join(', ')};\n\n-- conferência: compare com relatorio-conferencia.md antes do COMMIT\n${CONFERENCIA}\n\ncommit;\n`;
const ensaio = `begin;\n${corpoImportacao}\n${CONFERENCIA}\n\nrollback;\n`;

// ---------------------------------------------------------------- relatório de conferência
const fmt = (n) => n.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const totalPecas = linhas.reduce((s, l) => s + l.totalPecas, 0);
const totalValor = linhas.reduce((s, l) => s + l.valor, 0);
const totalCancelado = linhas.filter((l) => l.status === 'CANCELADA').reduce((s, l) => s + l.valor, 0);
const porMes = {};
for (const l of linhas) {
  const m = l.dia.slice(0, 7);
  porMes[m] = porMes[m] ?? { linhas: 0, valor: 0 };
  porMes[m].linhas++;
  porMes[m].valor += l.valor;
}
const porPessoa = {};
for (const l of linhas) for (const r of l.responsaveis.slice(0, 1)) {
  porPessoa[r] = porPessoa[r] ?? { linhas: 0, valor: 0 };
  porPessoa[r].linhas++;
  porPessoa[r].valor += l.valor;
}
const relatorio = `# Conferência da importação: planilha Demandas Multimídia 26-UGR (1T, 2T, 3T)

Gerado por \`gerar-importacao-ugr.cjs\` a partir de \`${path.basename(ARQUIVO)}\`.

## Totais
| Item | Valor |
|---|---|
| Linhas da planilha | ${linhas.length} (${ABAS.map((a) => `${a.nome}: ${linhas.filter((l) => l.aba === a.nome).length}`).join(', ')}) |
| **Demandas geradas** (mesma descrição = mesma demanda) | **${grupos.length}** |
| Peças | ${linhas.reduce((s, l) => s + l.pecas.length, 0)} |
| Atividades (uma por responsável de cada linha) | ${linhas.reduce((s, l) => s + l.responsaveis.length, 0)} |
| Participantes | ${grupos.reduce((s, g) => s + g.participantes.length, 0)} |
| Demandantes (por nome; diretoria = a mais frequente) | ${demandantes.size} |
| Diretorias usadas | ${diretoriasUsadas.length} |
| Tipos de peça | ${tipos.size} |
| Soma do Valor | R$ ${fmt(totalValor)} |
| Soma das peças geradas | R$ ${fmt(totalPecas)} |
| Do total, em linhas canceladas | R$ ${fmt(totalCancelado)} |

## Por mês (coluna Dia)
| Mês | Linhas | Valor |
|---|---|---|
${Object.entries(porMes).sort().map(([m, v]) => `| ${m} | ${v.linhas} | R$ ${fmt(v.valor)} |`).join('\n')}

## Por responsável (primeiro da linha)
| Planilha | Pessoa no sistema | Linhas | Valor |
|---|---|---|---|
${Object.entries(porPessoa).sort((a, b) => b[1].valor - a[1].valor).map(([k, v]) => `| ${k} | ${respMapa.get(k)} | ${v.linhas} | R$ ${fmt(v.valor)} |`).join('\n')}

## Pessoas novas
${pessoasNovas.map((p) => `- ${p.nome} (${p.area} / ${p.cargo}) — ${p.status}`).join('\n')}

## Tipos de peça e preço unitário
| Tipo | Área | Preço | Origem |
|---|---|---|---|
${tiposLista.map((t) => `| ${t.nome}${t.ativo ? '' : ' (inativo)'} | ${t.area} | R$ ${fmt(t.preco)} | ${t.origem} |`).join('\n')}

## Pontos de atenção
- **Preço da planilha diferente da tabela de valores** (vale a planilha): ${divergeTabela.length}${divergeTabela.length ? '\n' + divergeTabela.map((x) => `  - ${x}`).join('\n') : ''}
- **Linhas que não reconciliam** (viram um "Pacote" com o valor exato): ${naoBatem.length}${naoBatem.length ? '\n' + naoBatem.map((x) => `  - ${x}`).join('\n') : ''}
- **Linhas sem Valor** (calculado pela tabela): ${semValor.length}${semValor.length ? '\n' + semValor.map((x) => `  - ${x}`).join('\n') : ''}
- **Anomalias**: ${anomalias.length}${anomalias.length ? '\n' + anomalias.map((x) => `  - ${x}`).join('\n') : ''}
- **Mesma descrição com clientes diferentes** (vale o mais frequente): ${conflitosCliente.length}${conflitosCliente.slice(0, 15).map((x) => `\n  - ${x}`).join('')}
- **Mesma descrição com status diferentes** (vale o mais "aberto"): ${gruposMistos.length}${gruposMistos.slice(0, 15).map((x) => `\n  - ${x}`).join('')}
- **Demandantes com o mesmo primeiro nome** (decida se unifica): ${possiveisDuplicados.length ? possiveisDuplicados.map((v) => v.join(' / ')).join('; ') : 'nenhum'}
- **Diretorias que só diferem na caixa**: ${diretoriasParecidas.length ? diretoriasParecidas.map((v) => v.join(' / ')).join('; ') : 'nenhuma'}
- **Linhas sem cliente**: ${grupos.filter((g) => !g.cliente).length} demandas ficam sem demandante.
- Horários (Hora In/Out) não são importados.

## Ordem de execução
1. \`01-carga-NN.sql\` (${arquivosCarga.length} arquivos, em ordem) e \`01-conferir-carga.sql\` (tudo \`true\`).
2. \`03-ensaio-com-rollback.sql\` (não deixa nada no banco) e comparar com este relatório.
3. \`02-importar.sql\` (termina em \`commit\` e apaga o staging).
`;

// ---------------------------------------------------------------- conferência da carga (checksums)
const md5 = (partes) => crypto.createHash('md5').update(partes.join(','), 'utf8').digest('hex');
const d2 = (n) => Number(n).toFixed(2);
const ck = {
  grupo: md5(grupos.map((g) => [g.id, g.refId, d2(g.valor), g.status, g.cliente ?? '', g.dirSol ?? '', g.criacao, g.linhas, g.titulo].join(':'))),
  linha: md5([...linhas].sort((a, b) => a.id - b.id).map((l) => [l.id, l.grp, l.dia, l.prevista && l.prevista !== l.dia ? l.prevista : '', l.final && l.final !== l.dia ? l.final : '', l.responsaveis.join('|')].join(':'))),
  peca: md5([...linhas].sort((a, b) => a.id - b.id).flatMap((l) => l.pecas.map((p, i) => [l.id, i + 1, idDaPeca(p), p.qtd, d2(p.unit)].join(':')))),
  part: md5(grupos.flatMap((g) => g.participantes.map((p) => ({ g: g.id, e: p.entrada, pr: p.principal, r: p.chave })))
    .sort((a, b) => a.g - b.g || a.e.localeCompare(b.e) || Number(b.pr) - Number(a.pr) || (a.r < b.r ? -1 : a.r > b.r ? 1 : 0))
    .map((p) => [p.g, p.r, p.pr ? 't' : 'f', p.e].join(':'))),
};
const conferirCarga = `-- 01-conferir-carga.sql: rode depois dos 01-carga-NN.sql. Todas as colunas devem ser "true".
select
  (select count(*) from imp_ugr_grupo) = ${grupos.length} as grupos_qtd,
  (select count(*) from imp_ugr_linha) = ${linhas.length} as linhas_qtd,
  (select count(*) from imp_ugr_peca) = ${linhas.reduce((t, l) => t + l.pecas.length, 0)} as pecas_qtd,
  (select count(*) from imp_ugr_part) = ${grupos.reduce((t, g) => t + g.participantes.length, 0)} as part_qtd,
  (select count(*) from imp_ugr_tipo) = ${tiposLista.length} as tipos_qtd,
  (select count(*) from imp_ugr_demandante) = ${demandantes.size} as demandantes_qtd,
  (select count(*) from imp_ugr_pessoa) = ${pessoasNovas.length} as pessoas_qtd,
  (select md5(string_agg(grp || ':' || ref_id || ':' || valor::text || ':' || status || ':' || coalesce(cliente, '') || ':' || coalesce(dir_sol, '') || ':' || criacao || ':' || linhas || ':' || titulo, ',' order by grp)) from imp_ugr_grupo) = '${ck.grupo}' as grupos_md5,
  (select md5(string_agg(id || ':' || grp || ':' || dia || ':' || coalesce(prevista, '') || ':' || coalesce(final, '') || ':' || responsaveis, ',' order by id)) from imp_ugr_linha) = '${ck.linha}' as linhas_md5,
  (select md5(string_agg(lin || ':' || ordem || ':' || tipo || ':' || qtd || ':' || unit::text, ',' order by lin, ordem)) from imp_ugr_peca) = '${ck.peca}' as pecas_md5,
  (select md5(string_agg(grp || ':' || resp || ':' || case when principal then 't' else 'f' end || ':' || entrada, ',' order by grp, entrada, principal desc, resp collate "C")) from imp_ugr_part) = '${ck.part}' as part_md5;
`;

const saida = path.join(__dirname, 'saida-ugr');
fs.rmSync(saida, { recursive: true, force: true });
fs.mkdirSync(saida, { recursive: true });
arquivosCarga.forEach((c, i) => fs.writeFileSync(path.join(saida, `01-carga-${String(i + 1).padStart(2, '0')}.sql`), c));
fs.writeFileSync(path.join(saida, '01-conferir-carga.sql'), conferirCarga);
fs.writeFileSync(path.join(saida, '02-importar.sql'), importacao);
fs.writeFileSync(path.join(saida, '03-ensaio-com-rollback.sql'), ensaio);
fs.writeFileSync(path.join(saida, 'relatorio-conferencia.md'), relatorio);
console.log(`Linhas ${linhas.length} -> demandas ${grupos.length} | peças ${linhas.reduce((s, l) => s + l.pecas.length, 0)} | tipos ${tipos.size} | demandantes ${demandantes.size} | pessoas novas ${pessoasNovas.length}`);
console.log(`Soma planilha ${fmt(totalValor)} x peças ${fmt(totalPecas)} | cargas ${arquivosCarga.length} (${arquivosCarga.map((c) => (c.length / 1024).toFixed(0) + ' KB').join(', ')}) | 02: ${(importacao.length / 1024).toFixed(0)} KB`);
console.log(`Divergem da tabela ${divergeTabela.length} | não reconciliam ${naoBatem.length} | sem valor ${semValor.length} | anomalias ${anomalias.length} | conflitos de cliente ${conflitosCliente.length} | status mistos ${gruposMistos.length}`);
