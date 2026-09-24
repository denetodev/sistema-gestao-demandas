import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';

// MOCK TEMPORÁRIO (remover): rede do BB bloqueia o Supabase.
// Intercepta as respostas da API para permitir trabalho visual offline.
const TITULOS = [
    'Campanha Plano Safra', 'E-mail Varejo', 'Banner Mobile Ourocard', 'Vídeo Institucional',
    'Adaptação KV Consórcio', 'Régua de boas-vindas PF', 'Disparo MPE Week', 'Card Dia do Médico',
    'Peça Sustentabilidade', 'Revisão Segmentação Seguros', 'Landing Centauro Day', 'Motion Abertura Evento',
    'Cobertura House Talks', 'Adaptação Falecom', 'HTML Hórus APF', 'Captação Agência Centro',
    'E-mail Estilo', 'Banner Desktop Crédito', 'Edição Podcast Interno', 'Copy Régua Consignado',
];
const SEGMENTOS = ['Varejo', 'Estilo', 'Private', 'PJ', 'Agro', 'Gov'];
const STATUS = ['NAO_INICIADA', 'EM_ANDAMENTO', 'EM_APROVACAO', 'CONCLUIDA', 'CANCELADA'];
const PRIORIDADES = ['BAIXA', 'NORMAL', 'ALTA', 'URGENTE'];
const DIRETORIAS = [
    { id: 'd1', nome: 'COE/CRM' },
    { id: 'd2', nome: 'UGR' },
];
const CLIENTES_REF = [
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

function gerarDemandas(qtd: number) {
    return Array.from({ length: qtd }, (_, i) => {
        const status = STATUS[(i * 3) % STATUS.length];
        const concluida = status === 'CONCLUIDA';
        const diretoria = DIRETORIAS[i % DIRETORIAS.length];
        const cliente = CLIENTES_REF[(i * 2) % CLIENTES_REF.length];
        const projeto = PROJETOS_REF[(i * 5) % PROJETOS_REF.length];
        const criacao = 2 + i;
        const mes = String(9 - Math.floor(i / 30)).padStart(2, '0');

        return {
            id: `dem-${i + 1}`,
            titulo: `${TITULOS[i % TITULOS.length]} ${SEGMENTOS[(i * 7) % SEGMENTOS.length]}`,
            descricao: null,
            codigo: i % 3 === 0 ? `BC26${mes}${String.fromCharCode(65 + (i % 26))}${String.fromCharCode(65 + ((i * 3) % 26))}` : null,
            diretoriaId: diretoria.id,
            diretoriaNome: diretoria.nome,
            clienteId: cliente.id,
            clienteNome: cliente.nome,
            projetoId: projeto.id,
            projetoNome: projeto.nome,
            campanhaId: null,
            campanhaNome: null,
            prioridade: PRIORIDADES[(i * 5) % PRIORIDADES.length],
            status,
            dataCriacao: data(criacao),
            dataPrazo: i % 4 === 0 ? null : data(criacao - 12),
            dataEntregaReal: concluida ? data(criacao - 10) : null,
            valor: i % 5 === 0 ? null : 200 + ((i * 137) % 4800),
            observacoes: null,
            createdAt: '',
            updatedAt: '',
        };
    });
}

const dados: Record<string, unknown> = {
    '/pessoas/me': {
        vinculado: true,
        pessoa: {
            id: 'p1', nome: 'Deusdete Neto', email: 'neto@exemplo.com', perfil: 'ADMIN',
            areaId: null, areaNome: null, diretoriaId: 'd1', diretoriaNome: 'COE/CRM',
            status: 'ATIVO', authUserId: 'a1',
        },
    },
    '/diretorias': [
        { id: 'd1', nome: 'COE/CRM' },
        { id: 'd2', nome: 'UGR' },
    ],
    '/clientes': [
        { id: 'c1', nome: 'DIMAC', tipo: 'AREA', observacao: null, ativo: true, createdAt: '', updatedAt: '' },
        { id: 'c2', nome: 'Agência Centro BSB', tipo: 'UNIDADE', observacao: 'Piloto regional', ativo: true, createdAt: '', updatedAt: '' },
        { id: 'c3', nome: 'Flávia Nogueira', tipo: 'PESSOA', observacao: null, ativo: true, createdAt: '', updatedAt: '' },
        { id: 'c4', nome: 'Fornecedor XYZ', tipo: 'EXTERNO', observacao: 'Contrato encerrado', ativo: false, createdAt: '', updatedAt: '' },
    ],
    '/projetos': [
        { id: 'pr1', nome: 'Plano Safra 2026', clienteId: 'c1', clienteNome: 'DIMAC', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', descricao: null, status: 'ATIVO', createdAt: '', updatedAt: '' },
        { id: 'pr2', nome: 'Consórcio BB', clienteId: 'c2', clienteNome: 'Agência Centro BSB', diretoriaId: 'd1', diretoriaNome: 'COE/CRM', descricao: null, status: 'PAUSADO', createdAt: '', updatedAt: '' },
        { id: 'pr3', nome: 'Institucional 60 anos', clienteId: null, clienteNome: null, diretoriaId: 'd2', diretoriaNome: 'UGR', descricao: null, status: 'ENCERRADO', createdAt: '', updatedAt: '' },
    ],
    '/campanhas': [
        { id: 'cp1', nome: 'Outubro das MPEs', projetoId: 'pr1', projetoNome: 'Plano Safra 2026', codigo: 'BC2609EV', dataInicio: '2026-10-01', dataFim: '2026-10-31', createdAt: '', updatedAt: '' },
        { id: 'cp2', nome: 'Centauro Day', projetoId: 'pr2', projetoNome: 'Consórcio BB', codigo: 'BC2509DY', dataInicio: '2026-09-22', dataFim: null, createdAt: '', updatedAt: '' },
    ],
    '/demandas': gerarDemandas(150),
};

export const mockInterceptor: HttpInterceptorFn = (req, next) => {
    const rota = Object.keys(dados).find(r => req.url.endsWith(r));
    if (rota && req.method === 'GET') {
        return of(new HttpResponse({ status: 200, body: dados[rota] }));
    }
    return next(req);
};