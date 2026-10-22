#!/usr/bin/env node
/**
 * Gera o SQL de carga do inventário real do estúdio ("Lista de inventário Estúdio BB.xlsx") para o módulo de equipamentos.
 *
 *   node database/importacao/gerar-inventario.cjs "C:/caminho/Lista de inventário Estúdio BB.xlsx"
 *
 * Saída (pasta database/importacao/saida-inventario/, ignorada pelo git):
 *   00-apagar-dados-de-teste.sql   uso único: apaga TODO o conteúdo do módulo (empréstimos, equipamentos e categorias),
 *                                  que até a carga real era só dado de teste. Preserva o termo, os papéis e as pessoas.
 *   01-inventario.sql              cria as categorias e os equipamentos
 *   relatorio-conferencia.md       totais por categoria, regras aplicadas e pontos para conferir
 *
 * Regras de mapeamento (o relatório lista o que cada uma fez):
 *   - abas lidas: "EQUIPAMENTOS ESTÚDIO BB (2)" (patrimônio do BB) e "EQUIPAMENTOS ARISTROCRATA (2)" (da Aristocrata).
 *     Ficam de fora a aba "EQUIPAMENTOS ARISTROCRATA" (só um item solto) e "Notebooks-BB" (notebooks em posse de pessoas,
 *     não é equipamento de empréstimo do estúdio).
 *   - Nº DE BEM vira o patrimônio; "S/N" e vazio ficam sem patrimônio.
 *   - cada unidade física é um equipamento (linha com QUANT. 5 vira 5 registros, "(1/5)" a "(5/5)"), para dar para saber
 *     com quem está cada peça. Exceção: cabos, baterias, carregadores e fontes, que são lote (controle por quantidade).
 *   - unidades defeituosas citadas na observação ("1 queimado", "3 com erro") entram como INATIVAS, para não serem emprestadas.
 *     A planilha não diz QUAL unidade é a defeituosa: as últimas do grupo é que ficam inativas, ajuste na tela se precisar.
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync } = require('child_process');

const ARQUIVO = process.argv[2];
if (!ARQUIVO || !fs.existsSync(ARQUIVO)) {
  console.error('Uso: node gerar-inventario.cjs <Lista de inventário Estúdio BB.xlsx>');
  process.exit(1);
}

const ABAS = [
  { nome: 'EQUIPAMENTOS ESTÚDIO BB (2)', origem: 'BB', data: '06/07/2026', localizacao: 'Estúdio BB' },
  { nome: 'EQUIPAMENTOS ARISTROCRATA (2)', origem: 'Aristocrata', data: '03/06/2026', localizacao: null },
];

// ---------------------------------------------------------------- leitura do xlsx
function extrair(arquivo, destino) {
  try {
    execFileSync('unzip', ['-oq', arquivo, '-d', destino]);
  } catch {
    execFileSync('powershell', ['-NoProfile', '-Command', `Add-Type -AssemblyName System.IO.Compression.FileSystem; [IO.Compression.ZipFile]::ExtractToDirectory('${arquivo.replace(/'/g, "''")}', '${destino.replace(/'/g, "''")}')`]);
  }
}

function lerAbas(arquivo) {
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
  for (const m of wb.matchAll(/<sheet [^>]*name="([^"]+)"[^>]*r:id="([^"]+)"/g)) {
    const nome = dec(m[1]);
    const alvo = rels.match(new RegExp(`<Relationship [^>]*Id="${m[2]}"[^>]*Target="([^"]+)"|<Relationship [^>]*Target="([^"]+)"[^>]*Id="${m[2]}"`));
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
        else if (inl) valor = dec(inl[1]);
        else if (v) valor = dec(v[1]);
        if (valor !== null && valor !== '') cel[ref] = String(valor).replace(/\s+/g, ' ').trim();
      }
      linhas.push(cel);
    }
    resultado[nome] = linhas;
  }
  fs.rmSync(dir, { recursive: true, force: true });
  return resultado;
}

const sql = (s) => (s === null || s === undefined ? 'null' : `'${String(s).replace(/'/g, "''")}'`);

// ---------------------------------------------------------------- categorias: primeira regra que casar vence
/** Casa o começo do nome (o \\b do JavaScript não enxerga letra acentuada, por isso o lookahead). */
const prefixo = (alternativas) => new RegExp(`^(?:${alternativas})(?![\\p{L}\\d])`, 'iu');
const contem = (alternativas) => new RegExp(`(?<![\\p{L}\\d])(?:${alternativas})(?![\\p{L}\\d])`, 'iu');

// primeira regra que casar vence (primeiro pelo começo do nome, depois por palavra solta no meio)
const REGRAS = [
  [prefixo('cabos?|rolo de cabo'), 'Cabos e conectores', 'QUANTIDADE'],
  [prefixo('baterias?|carregadores?|fontes?'), 'Baterias, carregadores e fontes', 'QUANTIDADE'],
  [prefixo('c[aâ]meras?|filmadoras?|drone'), 'Câmeras, filmadoras e drones', 'INDIVIDUAL'],
  [prefixo('estabilizador|gimbal'), 'Estabilizadores e gimbals', 'INDIVIDUAL'],
  [prefixo('lentes?'), 'Lentes', 'INDIVIDUAL'],
  [prefixo('trip[eé]s?|monop[eé]s?|at096a'), 'Tripés e suportes', 'INDIVIDUAL'],
  [prefixo('cases?'), 'Acessórios de câmera', 'INDIVIDUAL'],
  [prefixo('luzes|luz|softbox|bast[aõ]es? led|flash|nanlite'), 'Iluminação', 'INDIVIDUAL'],
  [prefixo('microfones?|conjuntos? lapelas?|lapelas?|receptor|transmissor sennheiser|sistema microfone|espuma'), 'Microfones', 'INDIVIDUAL'],
  [prefixo('gravador|zoom h8|mesas? de som|monitores de [aá]udio|headphone|kit'), 'Gravadores e áudio', 'INDIVIDUAL'],
  [prefixo('mesas? de corte|atem|distribuidor|transmissor de v[ií]deo|spliter|splitter'), 'Switchers e transmissão de vídeo', 'INDIVIDUAL'],
  [prefixo('tv|monitor|desktop|apple|macbook|notebook|wifi'), 'TVs, monitores e informática', 'INDIVIDUAL'],
  [prefixo('teleprompter'), 'Teleprompters', 'INDIVIDUAL'],
  [prefixo('cart[aã]o|cart[oõ]es|leitor|leito'), 'Cartões e leitores', 'INDIVIDUAL'],
  [prefixo('r[eé]guas?|filtro protetor'), 'Energia', 'INDIVIDUAL'],
  // o tipo aparece depois da marca ("CANON Lente 85mm")
  [contem('lentes?'), 'Lentes', 'INDIVIDUAL'],
];

// correções de grafia que se repetem na planilha
const GRAFIA = [
  [/\bCannon\b/g, 'Canon'],
  [/\bSemnheiser\b/gi, 'Sennheiser'],
  [/\bManfroto\b/g, 'Manfrotto'],
  [/\bFemia\b/gi, 'Fêmea'],
  [/\bAristroc/gi, 'Aristoc'],
  [/Câmeras filmadora/g, 'Câmera filmadora'],
  [/MacBook Pro MacBook Pro/g, 'MacBook Pro'],
];
const SINGULAR = {
  Tripés: 'Tripé', Luzes: 'Luz', Microfones: 'Microfone', Filmadoras: 'Filmadora', Câmeras: 'Câmera', Cartões: 'Cartão',
  Bastões: 'Bastão', Conjuntos: 'Conjunto', Cases: 'Case', Monopés: 'Monopé', Mesas: 'Mesa', Monitores: 'Monitor',
  Lentes: 'Lente', Réguas: 'Régua', Leitores: 'Leitor', Leito: 'Leitor', Cabos: 'Cabo', Softbox: 'Softbox', Fontes: 'Fonte',
};

function corrigir(texto) {
  let t = texto;
  for (const [de, para] of GRAFIA) t = t.replace(de, para);
  return t;
}
function singularizar(nome) {
  const [primeira, ...resto] = nome.split(' ');
  return [SINGULAR[primeira] ?? primeira, ...resto].join(' ');
}
function classificar(nome) {
  for (const [re, categoria, controle] of REGRAS) if (re.test(nome)) return { categoria, controle };
  return { categoria: 'Outros', controle: 'INDIVIDUAL' };
}

/** Quantas unidades estão defeituosas segundo a observação ("1 queimado", "3 com erro", "1 com defeito"). */
function defeituosas(obs, quantidade) {
  if (!obs) return 0;
  const m = obs.match(/(\d+)\s+(?:\S+\s+)?(queimad|com erro|com defeito)/i);
  if (m) return Math.min(Number(m[1]), quantidade);
  return 0;
}

// ---------------------------------------------------------------- leitura
const abas = lerAbas(ARQUIVO);
const registros = []; // um por equipamento a criar
const avisos = [];
const lidas = [];

for (const cfg of ABAS) {
  const linhas = abas[cfg.nome];
  if (!linhas) {
    avisos.push(`Aba "${cfg.nome}" não encontrada`);
    continue;
  }
  let totalUnidades = 0;
  for (const l of linhas) {
    if (l.n <= 2 || !l.B) continue; // título e cabeçalho; linhas só com observação solta
    const original = l.B;
    const nome = corrigir(original);
    const quantidade = l.A ? Number(l.A) : null;
    if (!(quantidade >= 1) || !Number.isInteger(quantidade)) {
      avisos.push(`${cfg.nome}, linha ${l.n}: QUANT. inválida ou vazia ("${l.A ?? ''}") em "${original}": entrou com 1 unidade`);
    }
    const n = quantidade >= 1 && Number.isInteger(quantidade) ? quantidade : 1;
    const bem = l.C && !/^s\/?n$/i.test(l.C) ? l.C : null;
    const obs = l.D && !/^aristocrata( ok)?$/i.test(l.D) ? corrigir(l.D) : null;
    const { categoria, controle } = classificar(nome);
    if (categoria === 'Outros') avisos.push(`${cfg.nome}, linha ${l.n}: "${original}" não casou com nenhuma categoria (foi para "Outros")`);
    if (bem && n > 1) avisos.push(`${cfg.nome}, linha ${l.n}: "${original}" tem Nº DE BEM e QUANT. ${n}: o patrimônio ficou só na primeira unidade`);
    const origem = `${cfg.origem}, inventário ${cfg.data}`;
    totalUnidades += n;

    if (controle === 'QUANTIDADE') {
      const ruins = defeituosas(obs, n);
      registros.push({
        aba: cfg.nome, linha: l.n, categoria, controle, nome, patrimonio: bem, quantidade: Math.max(1, n - ruins),
        localizacao: cfg.localizacao, ativo: true, observacoes: [obs, ruins ? `${ruins} fora de uso (não contado na quantidade)` : null, origem].filter(Boolean).join('. '),
        original,
      });
      continue;
    }
    const ruins = defeituosas(obs, n);
    for (let i = 1; i <= n; i++) {
      const fora = i > n - ruins;
      registros.push({
        aba: cfg.nome, linha: l.n, categoria, controle, patrimonio: i === 1 ? bem : null, quantidade: 1,
        nome: n > 1 ? `${singularizar(nome)} (${i}/${n})` : nome,
        localizacao: cfg.localizacao, ativo: !fora,
        observacoes: [fora ? `Fora de uso: ${obs}` : obs, origem].filter(Boolean).join('. '),
        original,
      });
    }
  }
  lidas.push(`${cfg.nome}: ${totalUnidades} unidades`);
}

// ---------------------------------------------------------------- conferências
const patrimonios = new Map();
for (const r of registros) {
  if (!r.patrimonio) continue;
  const chave = r.patrimonio.toLowerCase();
  if (patrimonios.has(chave)) avisos.push(`Patrimônio repetido "${r.patrimonio}": ${patrimonios.get(chave)} e ${r.aba}, linha ${r.linha}. A carga vai falhar: ajuste antes`);
  else patrimonios.set(chave, `${r.aba}, linha ${r.linha}`);
}

const norm = (s) => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/[^a-z0-9]+/g, '');
const porNome = new Map();
for (const r of registros) {
  const base = norm(r.original);
  if (!porNome.has(base)) porNome.set(base, { linhas: new Map(), semBem: false });
  const g = porNome.get(base);
  g.linhas.set(`${r.aba.startsWith('EQUIPAMENTOS ESTÚDIO') ? 'BB' : 'Aristocrata'} linha ${r.linha}`, r.original);
  if (!r.patrimonio) g.semBem = true;
}
const possiveisDuplicados = [...porNome.values()]
  .filter((g) => g.linhas.size > 1 && g.semBem)
  .map((g) => `${[...g.linhas.values()][0]}: ${[...g.linhas.keys()].join(' e ')}`);

const porCategoria = new Map();
for (const r of registros) {
  const c = porCategoria.get(r.categoria) ?? { controle: r.controle, equipamentos: 0, unidades: 0, inativos: 0, comPatrimonio: 0 };
  c.equipamentos++;
  c.unidades += r.quantidade;
  if (!r.ativo) c.inativos++;
  if (r.patrimonio) c.comPatrimonio++;
  porCategoria.set(r.categoria, c);
}

// ---------------------------------------------------------------- SQL
const saida = path.join(__dirname, 'saida-inventario');
fs.mkdirSync(saida, { recursive: true });

fs.writeFileSync(path.join(saida, '00-apagar-dados-de-teste.sql'), `-- USO ÚNICO. Apaga TODO o conteúdo do módulo de equipamentos, que até a carga real era só dado de teste.
-- Preserva: termo de responsabilidade (versões), papéis das pessoas e as pessoas.
begin;

select 'antes' as momento,
       (select count(*) from emprestimo) as emprestimos,
       (select count(*) from emprestimo_foto) as fotos,
       (select count(*) from equipamento) as equipamentos,
       (select count(*) from categoria_equipamento) as categorias;

delete from auditoria where tabela = 'emprestimo';
delete from emprestimo;               -- itens e fotos saem em cascata
delete from equipamento;
delete from categoria_equipamento;

select 'depois' as momento,
       (select count(*) from emprestimo) as emprestimos,
       (select count(*) from equipamento) as equipamentos,
       (select count(*) from categoria_equipamento) as categorias,
       (select count(*) from termo_equipamento) as versoes_do_termo;
commit;
`);

const linhasSql = [];
linhasSql.push('-- Carga do inventário real do estúdio. Rode DEPOIS de 00-apagar-dados-de-teste.sql.', 'begin;', '');
for (const [categoria, c] of porCategoria) {
  linhasSql.push(`insert into categoria_equipamento (nome, controle) values (${sql(categoria)}, ${sql(c.controle)});`);
}
linhasSql.push('');
for (const [categoria] of porCategoria) {
  const doGrupo = registros.filter((r) => r.categoria === categoria);
  linhasSql.push(`-- ${categoria}: ${doGrupo.length} equipamentos`);
  linhasSql.push('with c as (select id from categoria_equipamento where nome = ' + sql(categoria) + ')');
  linhasSql.push('insert into equipamento (categoria_id, nome, patrimonio, quantidade_total, localizacao, observacoes, ativo)');
  linhasSql.push('select c.id, v.nome, v.patrimonio, v.quantidade_total, v.localizacao, v.observacoes, v.ativo from c, (values');
  linhasSql.push(
    doGrupo
      .map((r) => `  (${sql(r.nome)}, ${sql(r.patrimonio)}, ${r.quantidade}, ${sql(r.localizacao)}, ${sql(r.observacoes)}, ${r.ativo})`)
      .join(',\n'),
  );
  linhasSql.push(') as v(nome, patrimonio, quantidade_total, localizacao, observacoes, ativo);', '');
}
linhasSql.push(`select (select count(*) from categoria_equipamento) as categorias, (select count(*) from equipamento) as equipamentos,
       (select count(*) from equipamento where ativo) as ativos, (select count(*) from equipamento where patrimonio is not null) as com_patrimonio,
       ${registros.length} as esperado_equipamentos;`);
linhasSql.push('commit;');
fs.writeFileSync(path.join(saida, '01-inventario.sql'), linhasSql.join('\n') + '\n');

// ---------------------------------------------------------------- relatório
const rel = [];
rel.push('# Inventário do estúdio: conferência antes de carregar', '');
rel.push('## O que foi lido', ...lidas.map((l) => `- ${l}`), '- Ficaram de fora: aba "EQUIPAMENTOS ARISTROCRATA" (um item solto) e "Notebooks-BB" (notebooks em posse de pessoas).', '');
rel.push('## Totais', `- Registros de equipamento a criar: **${registros.length}** (${registros.filter((r) => r.ativo).length} ativos, ${registros.filter((r) => !r.ativo).length} inativos por defeito)`);
rel.push(`- Com patrimônio (Nº DE BEM): ${registros.filter((r) => r.patrimonio).length}`);
rel.push(`- Categorias: ${porCategoria.size}`, '');
rel.push('| Categoria | Controle | Registros | Unidades | Inativos | Com patrimônio |', '|---|---|---|---|---|---|');
for (const [k, c] of [...porCategoria].sort((a, b) => a[0].localeCompare(b[0], 'pt'))) {
  rel.push(`| ${k} | ${c.controle} | ${c.equipamentos} | ${c.unidades} | ${c.inativos} | ${c.comPatrimonio} |`);
}
rel.push('', '## Unidades marcadas como fora de uso (confira qual é a defeituosa)');
const inativos = registros.filter((r) => !r.ativo);
rel.push(...(inativos.length ? inativos.map((r) => `- ${r.nome}: ${r.observacoes}`) : ['- nenhuma']));
rel.push('', '## Possíveis duplicados (mesmo nome em mais de uma linha, conferir se são itens diferentes)');
rel.push(...(possiveisDuplicados.length ? possiveisDuplicados.map((d) => `- ${d}`) : ['- nenhum']));
rel.push('', '## Avisos', ...(avisos.length ? avisos.map((a) => `- ${a}`) : ['- nenhum']), '');
fs.writeFileSync(path.join(saida, 'relatorio-conferencia.md'), rel.join('\n'));

console.log(`Gerado em ${saida}`);
console.log(`conferência: soma dos tamanhos dos nomes = ${registros.reduce((s, r) => s + [...r.nome].length, 0)}; soma das quantidades = ${registros.reduce((s, r) => s + r.quantidade, 0)}; com patrimônio = ${registros.filter((r) => r.patrimonio).length}; ativos = ${registros.filter((r) => r.ativo).length}`);
console.log(`${registros.length} equipamentos em ${porCategoria.size} categorias; ${avisos.length} aviso(s); ${possiveisDuplicados.length} possível(is) duplicado(s)`);
