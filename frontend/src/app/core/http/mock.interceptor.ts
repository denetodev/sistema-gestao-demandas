import { HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { mockDemanda } from './mock-demanda';

// MOCK TEMPORÁRIO (remover): rede do BB bloqueia o Supabase.
// Intercepta as respostas da API para permitir trabalho visual offline.

// ==========================================================================
// Identidade simulada — troca PESSOA_ATUAL pra testar cada cenário.
// ==========================================================================
const ADMIN = {
  id: 'p1',
  nome: 'Deusdete Neto',
  email: 'neto@exemplo.com',
  fotoUrl: null,
  diretoriaId: 'd1',
  diretoriaNome: 'COE/CRM',
  nucleoId: 'n1',
  nucleoNome: 'House-CRM',
  areaId: 'a3',
  areaNome: 'HTML',
  cargoId: null,
  cargoNome: null,
  status: 'ATIVO',
  perfil: 'ADMIN',
  referenciaAreaId: null,
  referenciaAreaNome: null,
  authUserId: 'a1',
  aprovadoPorId: null,
  aprovadoPorNome: null,
  aprovadoEm: '2026-01-01T00:00:00Z',
  createdAt: '',
  updatedAt: '',
};

// Referência de Equipe da área Design — descomenta pra testar podeGerenciar
// por registro na tela de Tipos de peça (edita tp3/tp6, não vê botão nos
// outros, área travada e pré-preenchida no dialog).
const REFERENCIA_DESIGN = {
  ...ADMIN,
  id: 'p2',
  nome: 'Ana Ferreira',
  email: 'ana@exemplo.com',
  areaId: 'a1',
  areaNome: 'Design',
  status: 'ATIVO',
  perfil: 'PROFISSIONAL',
  referenciaAreaId: 'a1',
  referenciaAreaNome: 'Design',
  authUserId: 'a2',
};

// Contas que ainda não entram no app (telas de Aguardando aprovação e Cadastro não aprovado)
const PENDENTE = { ...ADMIN, id: 'p20', nome: 'Nova Pessoa', perfil: 'PROFISSIONAL', aprovadoEm: null };
const REJEITADA = { ...PENDENTE, status: 'REJEITADO' };
// Profissional puro: testa a tela de Acesso negado em /pessoas, /demandantes e /relatorio (Visualizador)
const PROFISSIONAL = { ...ADMIN, id: 'p21', nome: 'Bruno Alves', perfil: 'PROFISSIONAL' };
const VISUALIZADOR = { ...ADMIN, id: 'p22', nome: 'Vera Souza', perfil: 'VISUALIZADOR' };

const PESSOA_ATUAL = ADMIN;
// const PESSOA_ATUAL = PENDENTE;
// const PESSOA_ATUAL = REJEITADA;
// const PESSOA_ATUAL = PROFISSIONAL;
// const PESSOA_ATUAL = VISUALIZADOR;
// const PESSOA_ATUAL = REFERENCIA_DESIGN;

// ==========================================================================
// Dados mestres
// ==========================================================================
const TITULOS = [
  'Campanha Plano Safra',
  'E-mail Varejo',
  'Banner Mobile Ourocard',
  'Vídeo Institucional',
  'Adaptação KV Consórcio',
  'Régua de boas-vindas PF',
  'Disparo MPE Week',
  'Card Dia do Médico',
  'Peça Sustentabilidade',
  'Revisão Segmentação Seguros',
  'Landing Centauro Day',
  'Motion Abertura Evento',
  'Cobertura House Talks',
  'Adaptação Falecom',
  'HTML Hórus APF',
  'Captação Agência Centro',
  'E-mail Estilo',
  'Banner Desktop Crédito',
  'Edição Podcast Interno',
  'Copy Régua Consignado',
];

const SEGMENTOS = ['Varejo', 'Estilo', 'Private', 'PJ', 'Agro', 'Gov'];
const STATUS = ['NAO_INICIADA', 'EM_ANDAMENTO', 'EM_APROVACAO', 'CONCLUIDA', 'CANCELADA'];
const PRIORIDADES = ['BAIXA', 'NORMAL', 'ALTA', 'URGENTE'];
const DIRETORIAS = [
  { id: 'd1', nome: 'COE/CRM' },
  { id: 'd2', nome: 'UGR' },
  { id: 'dx0', nome: 'URI' },
  { id: 'dx1', nome: 'DINED' },
  { id: 'dx2', nome: 'COMIN' },
  { id: 'dx3', nome: 'DIGOV' },
  { id: 'dx4', nome: 'DIMAC' },
  { id: 'dx5', nome: 'DIOPE' },
  { id: 'dx6', nome: 'DIREC' },
  { id: 'dx7', nome: 'UCI' },
  { id: 'dx8', nome: 'DITEC' },
  { id: 'dx9', nome: 'DIREO' },
  { id: 'dx10', nome: 'DIPES' },
  { id: 'dx11', nome: 'DIRAG' },
  { id: 'dx12', nome: 'DIRIS' },
  { id: 'dx13', nome: 'UAN' },
  { id: 'dx14', nome: 'UEG DIGOV' },
  { id: 'dx15', nome: 'Super ADM' },
  { id: 'dx16', nome: 'Presi' },
  { id: 'dx17', nome: 'Aceleração Digital' },
  { id: 'dx18', nome: 'TESOU/GEASE/MERCADO' },
];
const NUCLEOS = [
  { id: 'n1', nome: 'House-CRM', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', ativo: true, createdAt: '', updatedAt: '' },
  { id: 'n2', nome: 'Multimídia', diretoriaId: 'd2', diretoriaNome: 'UGR', nucleoId: 'n2', nucleoNome: 'Multimídia', ativo: true, createdAt: '', updatedAt: '' },
];
const AREAS = [
  { id: 'a1', nome: 'Design', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', nucleoId: 'n1', nucleoNome: 'House-CRM', ativo: true, createdAt: '', updatedAt: '' },
  { id: 'a2', nome: 'Motion', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', nucleoId: 'n1', nucleoNome: 'House-CRM', ativo: true, createdAt: '', updatedAt: '' },
  { id: 'a3', nome: 'HTML', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', nucleoId: 'n1', nucleoNome: 'House-CRM', ativo: true, createdAt: '', updatedAt: '' },
  { id: 'a4', nome: 'Audiovisual', diretoriaId: 'd2', diretoriaNome: 'UGR', ativo: true, createdAt: '', updatedAt: '' },
];
const DEMANDANTES_REF = [
  { id: 'c1', nome: 'DIMAC' },
  { id: 'c2', nome: 'Agência Centro BSB' },
  { id: 'c3', nome: 'Flávia Nogueira' },
  { id: null, nome: null },
];
const PROJETOS_REF = [
  { id: 'pr1', nome: 'Plano Safra 2026' },
  { id: 'pr2', nome: 'Consórcio BB' },
  { id: 'pr3', nome: 'Institucional 60 anos' },
  { id: null, nome: null },
];

function data(diasAtras: number): string {
  const d = new Date(2026, 8, 24);
  d.setDate(d.getDate() - diasAtras);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Pessoas de exemplo para o grupo de fotos da lista (algumas com foto, as outras só com iniciais). */
const AVATAR_AZUL =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="#0ea5e9"/><circle cx="50" cy="38" r="18" fill="#fff"/><path d="M14 100c4-26 20-38 36-38s32 12 36 38z" fill="#fff"/></svg>');
const AVATAR_VERDE =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" fill="#16a34a"/><circle cx="50" cy="38" r="18" fill="#fff"/><path d="M14 100c4-26 20-38 36-38s32 12 36 38z" fill="#fff"/></svg>');
const TIME_DE_EXEMPLO: [string, string | null][] = [
  ['Deusdete Neto', AVATAR_AZUL],
  ['Lucas Corsino', null],
  ['Thaís Trindade', AVATAR_VERDE],
  ['Cícero Londerly', null],
  ['Ana Beatriz', null],
  ['Rafael Lima', AVATAR_AZUL],
  ['Camila Duarte', null],
];

function gerarDemandas(qtd: number) {
  return Array.from({ length: qtd }, (_, i) => {
    const status = STATUS[(i * 3) % STATUS.length];
    const concluida = status === 'CONCLUIDA';
    const diretoria = DIRETORIAS[i % DIRETORIAS.length];
    const demandante = DEMANDANTES_REF[(i * 2) % DEMANDANTES_REF.length];
    const projeto = PROJETOS_REF[(i * 5) % PROJETOS_REF.length];
    const criacao = 2 + i;
    const mes = String(9 - Math.floor(i / 30)).padStart(2, '0');
    const valor = i % 5 === 0 ? null : 200 + ((i * 137) % 4800);

    return {
      id: `dem-${i + 1}`,
      titulo: `${TITULOS[i % TITULOS.length]} ${SEGMENTOS[(i * 7) % SEGMENTOS.length]}`,
      descricao: null,
      codigo:
        i % 3 === 0
          ? `BC26${mes}${String.fromCharCode(65 + (i % 26))}${String.fromCharCode(65 + ((i * 3) % 26))}`
          : null,
      diretoriaId: diretoria.id,
      diretoriaNome: diretoria.nome,
      demandanteId: demandante.id,
      demandanteNome: demandante.nome,
      projetoId: projeto.id,
      projetoNome: projeto.nome,
      campanhaId: null,
      campanhaNome: null,
      prioridade: PRIORIDADES[(i * 5) % PRIORIDADES.length],
      status,
      dataCriacao: data(criacao),
      dataPrazo: i % 4 === 0 ? null : data(criacao - 12),
      dataEntregaReal: concluida ? data(criacao - 10) : null,
      valor,
      valorCalculado: valor === null ? null : Math.round(valor * 1.15),
      podeAlterar: true,
      participantes: TIME_DE_EXEMPLO.slice(0, 1 + (i % 6)).map(([nome, fotoUrl], k) => ({ pessoaId: `p-ex-${k}`, nome, fotoUrl })),
      observacoes: null,
      createdAt: '',
      updatedAt: '',
    };
  });
}

const TODAS_DEMANDAS = gerarDemandas(150);

// ==========================================================================
// Paginação + escopo de /demandas
// ==========================================================================
function compararPorCampo(a: any, b: any, campo: string): number {
  const va = a[campo];
  const vb = b[campo];
  if (va == null && vb == null) return 0;
  if (va == null) return -1;
  if (vb == null) return 1;
  if (typeof va === 'number' && typeof vb === 'number') return va - vb;
  return String(va).localeCompare(String(vb), 'pt-BR');
}

function responderDemandas(req: HttpRequest<unknown>) {
  const escopo = req.params.get('escopo') ?? 'MINHAS';
  const page = Number(req.params.get('page') ?? 0);
  const size = Number(req.params.get('size') ?? 10);
  const sort = req.params.get('sort');

  let filtradas = TODAS_DEMANDAS;
  if (escopo === 'MINHAS') {
    filtradas = TODAS_DEMANDAS.filter((_, i) => i % 7 === 0);
  } else if (escopo === 'EQUIPE') {
    filtradas = TODAS_DEMANDAS.filter((_, i) => i % 3 === 0);
  } else if (escopo === 'DIRETORIA' || escopo === 'NUCLEO') {
    filtradas = TODAS_DEMANDAS.filter((d) => d.diretoriaId === PESSOA_ATUAL.diretoriaId);
  }
  // TODAS: sem filtro

  // busca, situação, diretoria e equipe só estreitam o escopo (como na API)
  const semAcento = (t: string) => t.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
  const busca = req.params.get('busca');
  if (busca) {
    const q = semAcento(busca);
    filtradas = filtradas.filter((d: any) => semAcento(d.titulo + ' ' + (d.codigo ?? '')).includes(q));
  }
  const status = req.params.get('status');
  if (status) filtradas = filtradas.filter((d: any) => d.status === status);
  const diretoria = req.params.get('diretoriaId');
  if (diretoria) filtradas = filtradas.filter((d: any) => d.diretoriaId === diretoria);

  const [campoOrdenacao, direcao] = (sort ?? 'dataCriacao,desc').split(',');
  filtradas = [...filtradas].sort((a, b) =>
    direcao === 'asc'
      ? compararPorCampo(a, b, campoOrdenacao)
      : compararPorCampo(b, a, campoOrdenacao),
  );

  const inicio = page * size;
  const content = filtradas.slice(inicio, inicio + size);
  const totalElements = filtradas.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));

  return {
    content,
    totalElements,
    totalPages,
    number: page,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
  };
}

// ==========================================================================
// Demais rotas — resposta fixa
// ==========================================================================
const dados: Record<string, unknown> = {
  '/pessoas/me': { vinculado: !!PESSOA_ATUAL.aprovadoEm, pessoa: PESSOA_ATUAL },
  '/diretorias': DIRETORIAS,
  '/nucleos': NUCLEOS,
  '/areas': AREAS,
  '/demandantes': [
    { id: 'c1', nome: 'DIMAC', tipo: 'AREA', observacao: null, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'c2', nome: 'Agência Centro BSB', tipo: 'UNIDADE', observacao: 'Piloto regional', ativo: true, createdAt: '', updatedAt: '' },
    { id: 'c3', nome: 'Flávia Nogueira', tipo: 'PESSOA', observacao: null, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'c4', nome: 'Fornecedor XYZ', tipo: 'EXTERNO', observacao: 'Contrato encerrado', ativo: false, createdAt: '', updatedAt: '' },
  ],
  '/projetos': [
    { id: 'pr1', nome: 'Plano Safra 2026', demandanteId: 'c1', demandanteNome: 'DIMAC', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', descricao: null, status: 'ATIVO', createdAt: '', updatedAt: '' },
    { id: 'pr2', nome: 'Consórcio BB', demandanteId: 'c2', demandanteNome: 'Agência Centro BSB', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', descricao: null, status: 'PAUSADO', createdAt: '', updatedAt: '' },
    { id: 'pr3', nome: 'Institucional 60 anos', demandanteId: null, demandanteNome: null, diretoriaId: 'd2', diretoriaNome: 'UGR', descricao: null, status: 'ENCERRADO', createdAt: '', updatedAt: '' },
  ],
  '/campanhas': [
    { id: 'cp1', nome: 'Outubro das MPEs', projetoId: 'pr1', projetoNome: 'Plano Safra 2026', codigo: 'BC2609EV', dataInicio: '2026-10-01', dataFim: '2026-10-31', createdAt: '', updatedAt: '' },
    { id: 'cp2', nome: 'Centauro Day', projetoId: 'pr2', projetoNome: 'Consórcio BB', codigo: 'BC2509DY', dataInicio: '2026-09-22', dataFim: null, createdAt: '', updatedAt: '' },
  ],
  '/campanhas/resumo': [
    { id: 'cp1', nome: 'Outubro das MPEs', codigo: 'BC2609EV', demandas: 8, demandasAtivas: 5, pessoas: 4, valor: 18400, pecas: 22, atividades: 41, porStatus: { NAO_INICIADA: 1, EM_ANDAMENTO: 3, EM_APROVACAO: 1, CONCLUIDA: 3, CANCELADA: 0 }, primeiraDemanda: '2026-09-22', ultimaDemanda: '2026-10-09' },
    { id: 'cp2', nome: 'Centauro Day', codigo: 'BC2509DY', demandas: 3, demandasAtivas: 0, pessoas: 2, valor: 5200, pecas: 7, atividades: 12, porStatus: { NAO_INICIADA: 0, EM_ANDAMENTO: 0, EM_APROVACAO: 0, CONCLUIDA: 3, CANCELADA: 0 }, primeiraDemanda: '2026-08-12', ultimaDemanda: '2026-09-02' },
  ],
  '/tipos-peca': [
    { id: 'tp1', nome: 'Banner estático', descricao: null, areaId: 'a2', areaNome: 'Motion', valorReferencia: 800, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'tp2', nome: 'Ajustes HTML e-mail marketing', descricao: null, areaId: 'a3', areaNome: 'HTML', valorReferencia: 6000, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'tp3', nome: 'KV', descricao: 'Key visual de campanha', areaId: 'a1', areaNome: 'Design', valorReferencia: 1500, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'tp4', nome: 'Vinheta 15s', descricao: null, areaId: 'a2', areaNome: 'Motion', valorReferencia: 2200, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'tp5', nome: 'Captação externa', descricao: null, areaId: 'a4', areaNome: 'Audiovisual', valorReferencia: 3000, ativo: false, createdAt: '', updatedAt: '' },
    { id: 'tp6', nome: 'Banner mobile', descricao: null, areaId: 'a1', areaNome: 'Design', valorReferencia: 700, ativo: true, createdAt: '', updatedAt: '' },
  ],
  '/cargos': [
    { id: 'cg1', nome: 'Especialista HTML', descricao: null, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'cg2', nome: 'Designer', descricao: null, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'cg3', nome: 'Motion Designer', descricao: null, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'cg4', nome: 'Videomaker', descricao: null, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'cg5', nome: 'Redator', descricao: null, ativo: true, createdAt: '', updatedAt: '' },
    { id: 'cg6', nome: 'Estagiário', descricao: 'Cargo descontinuado', ativo: false, createdAt: '', updatedAt: '' },
  ],
  '/pessoas': [
    {
      id: 'p1', nome: 'Deusdete Neto', email: 'neto@exemplo.com',
      diretoriaId: 'd1', diretoriaNome: 'COE/CRM', areaId: 'a1', areaNome: 'HTML',
      cargoId: 'cg1', cargoNome: 'Especialista HTML',
      referenciaAreaId: null, referenciaAreaNome: null, fotoUrl: null,
      status: 'ATIVO', perfil: 'ADMIN', authUserId: 'auth-1',
      aprovadoPorId: null, aprovadoPorNome: null, aprovadoEm: '2026-09-16T14:17:48Z',
      createdAt: '2026-09-16T14:17:48Z', updatedAt: '2026-09-26T13:46:38Z'
    },

    {
      id: 'p2', nome: 'Carolina Ribeiro', email: 'carol@exemplo.com',
      diretoriaId: 'd1', diretoriaNome: 'COE/CRM', areaId: 'a2', areaNome: 'Design',
      cargoId: 'cg2', cargoNome: 'Designer',
      referenciaAreaId: 'a2', referenciaAreaNome: 'Design', fotoUrl: null,
      status: 'ATIVO', perfil: 'PROFISSIONAL', authUserId: 'auth-2',
      aprovadoPorId: 'p1', aprovadoPorNome: 'Deusdete Neto', aprovadoEm: '2026-09-18T10:00:00Z',
      createdAt: '2026-09-17T09:00:00Z', updatedAt: '2026-09-18T10:00:00Z'
    },

    {
      id: 'p3', nome: 'Marcos Tavares', email: 'marcos@exemplo.com',
      diretoriaId: 'd2', diretoriaNome: 'UGR', areaId: 'a3', areaNome: 'Motion',
      cargoId: 'cg3', cargoNome: 'Motion Designer',
      referenciaAreaId: null, referenciaAreaNome: null, fotoUrl: null,
      status: 'ATIVO', perfil: 'GESTOR', authUserId: 'auth-3',
      aprovadoPorId: 'p1', aprovadoPorNome: 'Deusdete Neto', aprovadoEm: '2026-09-19T11:00:00Z',
      createdAt: '2026-09-19T08:00:00Z', updatedAt: '2026-09-19T11:00:00Z'
    },

    {
      id: 'p4', nome: 'Juliana Prado', email: 'juliana@exemplo.com',
      diretoriaId: 'd2', diretoriaNome: 'UGR', areaId: 'a4', areaNome: 'Audiovisual',
      cargoId: 'cg4', cargoNome: 'Videomaker',
      referenciaAreaId: null, referenciaAreaNome: null, fotoUrl: null,
      status: 'ATIVO', perfil: 'PROFISSIONAL', authUserId: 'auth-4',
      aprovadoPorId: null, aprovadoPorNome: null, aprovadoEm: null,
      createdAt: '2026-09-26T16:30:00Z', updatedAt: '2026-09-26T16:30:00Z'
    },

    {
      id: 'p5', nome: 'Rafael Nunes', email: 'rafael@exemplo.com',
      diretoriaId: 'd1', diretoriaNome: 'COE/CRM', areaId: 'a1', areaNome: 'HTML',
      cargoId: null, cargoNome: null,
      referenciaAreaId: null, referenciaAreaNome: null, fotoUrl: null,
      status: 'ATIVO', perfil: 'PROFISSIONAL', authUserId: 'auth-5',
      aprovadoPorId: null, aprovadoPorNome: null, aprovadoEm: null,
      createdAt: '2026-09-27T09:15:00Z', updatedAt: '2026-09-27T09:15:00Z'
    },

    // Profissionais aprovados para o dashboard ter equipe e diretoria com mais de uma pessoa
    ...[
      ['p7', 'Bruno Alves', 'd1', 'COE/CRM', 'a1', 'Design'],
      ['p8', 'Diego Martins', 'd1', 'COE/CRM', 'a1', 'Design'],
      ['p9', 'Elisa Campos', 'd1', 'COE/CRM', 'a1', 'Design'],
      ['p10', 'Fabio Lima', 'd1', 'COE/CRM', 'a3', 'HTML'],
      ['p11', 'Gabriela Reis', 'd2', 'UGR', 'a4', 'Audiovisual'],
    ].map(([id, nome, diretoriaId, diretoriaNome, areaId, areaNome]) => ({
      id, nome, email: `${id}@exemplo.com`, diretoriaId, diretoriaNome, areaId, areaNome,
      nucleoId: diretoriaId === 'd1' ? 'n1' : 'n2', nucleoNome: diretoriaId === 'd1' ? 'House-CRM' : 'Multimídia',
      cargoId: null, cargoNome: null,
      referenciaAreaId: null, referenciaAreaNome: null, fotoUrl: null,
      status: 'ATIVO', perfil: 'PROFISSIONAL', authUserId: `auth-${id}`,
      aprovadoPorId: 'p1', aprovadoPorNome: 'Deusdete Neto', aprovadoEm: '2026-09-20T10:00:00Z',
      createdAt: '2026-09-19T09:00:00Z', updatedAt: '2026-09-20T10:00:00Z',
    })),

    {
      id: 'p6', nome: 'Tentativa Indevida', email: 'estranho@exemplo.com',
      diretoriaId: null, diretoriaNome: null, nucleoId: null, nucleoNome: null, areaId: 'a1', areaNome: 'HTML',
      cargoId: null, cargoNome: null,
      referenciaAreaId: null, referenciaAreaNome: null, fotoUrl: null,
      status: 'REJEITADO', perfil: 'PROFISSIONAL', authUserId: 'auth-6',
      aprovadoPorId: null, aprovadoPorNome: null, aprovadoEm: null,
      createdAt: '2026-09-20T22:00:00Z', updatedAt: '2026-09-21T08:00:00Z'
    },
  ],
};


export const mockInterceptor: HttpInterceptorFn = (req, next) => {
  const detalhe = mockDemanda(req, {
    demandas: TODAS_DEMANDAS,
    pessoas: dados['/pessoas'] as any[],
    pessoaAtual: PESSOA_ATUAL,
    dados,
  });
  if (detalhe) return detalhe;

  if (req.method !== 'GET') {
    return next(req);
  }

  const caminho = req.url.split('?')[0];

if (caminho.endsWith('/pessoas')) {
  const pendente = req.url.includes('pendente=true');
  const todas = dados['/pessoas'] as any[];
  const lista = pendente
    ? todas.filter((p) => p.aprovadoEm === null && p.status !== 'REJEITADO')
    : todas;
  return of(new HttpResponse({ status: 200, body: lista }));
}

  if (caminho.endsWith('/demandas')) {
    return of(new HttpResponse({ status: 200, body: responderDemandas(req) }));
  }

  const rota = Object.keys(dados).find((r) => caminho.endsWith(r));
  if (rota) {
    return of(new HttpResponse({ status: 200, body: dados[rota] }));
  }

  return next(req);
};