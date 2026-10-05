import { HttpEvent, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, delay, of } from 'rxjs';

// MOCK TEMPORÁRIO (remover junto com mock.interceptor.ts): rede do BB bloqueia o Supabase.
// Simula, em memória, o detalhe da demanda e as mutações (criar/editar/cancelar demanda,
// status, participantes, peças, atividades e evidências). Estado vale até recarregar a página.

export interface MockCtx {
  demandas: any[];
  pessoas: any[];
  pessoaAtual: any;
  dados: Record<string, any>;
}

const PAPEIS = ['RESPONSAVEL_PRINCIPAL', 'PARTICIPANTE', 'APOIO', 'REVISOR'];
const TIPOS_ATIVIDADE = [
  { id: 'ta1', nome: 'Briefing', descricao: null, ativo: true },
  { id: 'ta2', nome: 'Produção', descricao: null, ativo: true },
  { id: 'ta3', nome: 'Revisão', descricao: null, ativo: true },
  { id: 'ta4', nome: 'Aprovação com demandante', descricao: null, ativo: true },
  { id: 'ta5', nome: 'Reunião', descricao: null, ativo: false },
];

let contador = 0;
const novoId = (prefixo: string) => `${prefixo}-${++contador}`;
const hoje = () => new Date().toISOString().slice(0, 10);

export function tiposAtividadeMock() {
  return TIPOS_ATIVIDADE;
}

interface Estado {
  participantes: any[];
  pecas: any[];
  atividades: any[];
  evidencias: any[];
}
let estado: Estado | null = null;

function ativas(ctx: MockCtx) {
  return ctx.pessoas.filter((p) => p.status === 'ATIVO' && p.aprovadoEm);
}

function semear(ctx: MockCtx): Estado {
  const pessoas = ativas(ctx);
  const tipos = (ctx.dados['/tipos-peca'] as any[]).filter((t) => t.ativo);
  const e: Estado = { participantes: [], pecas: [], atividades: [], evidencias: [] };

  ctx.demandas.forEach((d, i) => {
    const part = (n: number) => pessoas[(i + n) % pessoas.length];
    [0, 1, 2].slice(0, 1 + (i % 3)).forEach((n) => {
      const p = part(n);
      e.participantes.push({
        id: novoId('part'), demandaId: d.id, pessoaId: p.id, pessoaNome: p.nome,
        papel: PAPEIS[n === 0 ? 0 : 1 + ((i + n) % 3)],
        dataEntrada: d.dataCriacao, dataSaida: null, observacao: null,
      });
    });

    if (i % 3 !== 2) {
      for (let n = 0; n < 1 + (i % 3); n++) {
        const t = tipos[(i + n) % tipos.length];
        const p = part(n);
        const quantidade = 1 + ((i + n) % 3);
        const peca = {
          id: novoId('peca'), demandaId: d.id, demandaTitulo: d.titulo,
          tipoPecaId: t.id, tipoPecaNome: t.nome,
          nome: `${t.nome} v${n + 1}`, descricao: null,
          quantidade, valorUnitario: t.valorReferencia, valorTotal: quantidade * t.valorReferencia,
          pessoaId: p.id, pessoaNome: p.nome, dataEntrega: d.dataPrazo,
        };
        e.pecas.push(peca);
        if (n === 0) {
          e.evidencias.push({
            id: novoId('evid'), atividadeId: null, pecaId: peca.id, tipo: 'MOCKUP',
            conteudo: `mockup_${d.id}.png`, descricao: 'Mockup aprovado', createdAt: '',
          });
        }
      }
    }

    if (i % 4 !== 3) {
      for (let n = 0; n < 2 + (i % 3); n++) {
        const ta = TIPOS_ATIVIDADE[n % 4];
        const p = part(n);
        const atv = {
          id: novoId('atv'), demandaId: d.id, demandaTitulo: d.titulo,
          tipoAtividadeId: ta.id, tipoAtividadeNome: ta.nome,
          pessoaId: p.id, pessoaNome: p.nome,
          descricao: n === 0 ? 'Alinhamento inicial com o demandante' : null,
          dataRealizacao: d.dataCriacao,
        };
        e.atividades.push(atv);
        if (n === 1) {
          e.evidencias.push({
            id: novoId('evid'), atividadeId: atv.id, pecaId: null, tipo: 'LINK',
            conteudo: 'https://exemplo.com/planner/tarefa', descricao: 'Tarefa no Planner', createdAt: '',
          });
        }
      }
    }
  });
  ctx.demandas.forEach((d) => {
    const pecas = e.pecas.filter((p) => p.demandaId === d.id);
    d.valorCalculado = pecas.length ? pecas.reduce((t, p) => t + p.valorTotal, 0) : null;
  });
  return e;
}

function recalcularValor(ctx: MockCtx, demandaId: string) {
  const d = ctx.demandas.find((x) => x.id === demandaId);
  if (!d) return;
  const pecas = estado!.pecas.filter((p) => p.demandaId === demandaId);
  if (pecas.length) d.valorCalculado = pecas.reduce((s, p) => s + p.valorTotal, 0);
}

const nomeDe = (lista: any[], id: string | null) => lista.find((x) => x.id === id)?.nome ?? null;

function aplicarPayloadDemanda(ctx: MockCtx, d: any, b: any) {
  Object.assign(d, {
    titulo: b.titulo,
    descricao: b.descricao ?? null,
    codigo: b.codigo ?? d.codigo ?? null,
    diretoriaId: b.diretoriaId,
    diretoriaNome: nomeDe(ctx.dados['/diretorias'], b.diretoriaId),
    demandanteId: b.demandanteId ?? null,
    demandanteNome: nomeDe(ctx.dados['/demandantes'], b.demandanteId),
    projetoId: b.projetoId ?? null,
    projetoNome: nomeDe(ctx.dados['/projetos'], b.projetoId),
    campanhaId: b.campanhaId ?? null,
    campanhaNome: nomeDe(ctx.dados['/campanhas'], b.campanhaId),
    prioridade: b.prioridade ?? 'NORMAL',
    dataPrazo: b.dataPrazo ?? null,
    valor: b.valor ?? null,
    observacoes: b.observacoes ?? null,
  });
}

function resp(status: number, body: unknown): Observable<HttpEvent<unknown>> {
  return of(new HttpResponse({ status, body })).pipe(delay(120));
}

/** Devolve a resposta simulada ou null se a rota não é tratada aqui. */
export function mockDemanda(req: HttpRequest<unknown>, ctx: MockCtx): Observable<HttpEvent<unknown>> | null {
  const caminho = req.url.split('?')[0];
  const i = caminho.search(/\/(demandas|pecas|atividades|evidencias|tipos-atividade)(\/|$)/);
  if (i < 0) return null;
  const partes = caminho.slice(i + 1).split('/'); // ex.: ['demandas', ':id', 'participantes']
  const [recurso, id, sub, subId] = partes;
  const m = req.method;
  const b: any = req.body ?? {};
  estado ??= semear(ctx);
  const e = estado;

  if (recurso === 'tipos-atividade' && m === 'GET') return resp(200, TIPOS_ATIVIDADE);

  if (recurso === 'demandas') {
    // a listagem paginada continua no interceptor principal
    if (!id) return m === 'POST' ? criarDemanda(ctx, b) : null;
    const d = ctx.demandas.find((x) => x.id === id);
    if (!d) return resp(404, { message: 'Demanda não encontrada' });

    if (!sub) {
      if (m === 'GET') return resp(200, d);
      if (m === 'PUT') { aplicarPayloadDemanda(ctx, d, b); return resp(200, d); }
      if (m === 'DELETE') { d.status = 'CANCELADA'; return resp(204, null); }
    }
    if (sub === 'status' && m === 'PATCH') {
      d.status = b.status;
      d.dataEntregaReal = b.status === 'CONCLUIDA' ? hoje() : null;
      return resp(200, d);
    }
    if (sub === 'participantes') {
      if (m === 'GET') return resp(200, e.participantes.filter((p) => p.demandaId === id));
      if (m === 'POST') {
        const pessoa = ctx.pessoas.find((p) => p.id === b.pessoaId);
        const novo = {
          id: novoId('part'), demandaId: id, pessoaId: b.pessoaId, pessoaNome: pessoa?.nome ?? '?',
          papel: b.papel ?? 'PARTICIPANTE', dataEntrada: hoje(), dataSaida: null, observacao: null,
        };
        e.participantes.push(novo);
        return resp(201, novo);
      }
      if (m === 'DELETE') {
        e.participantes = e.participantes.filter((p) => p.id !== subId);
        return resp(204, null);
      }
    }
    return null;
  }

  if (recurso === 'pecas') {
    if (m === 'GET' && !id) {
      const demandaId = req.params.get('demandaId');
      return resp(200, demandaId ? e.pecas.filter((p) => p.demandaId === demandaId) : e.pecas);
    }
    if (m === 'POST') {
      const tipo = (ctx.dados['/tipos-peca'] as any[]).find((t) => t.id === b.tipoPecaId);
      const pessoa = ctx.pessoas.find((p) => p.id === b.pessoaId);
      const quantidade = b.quantidade ?? 1;
      const d = ctx.demandas.find((x) => x.id === b.demandaId);
      const nova = {
        id: novoId('peca'), demandaId: b.demandaId, demandaTitulo: d?.titulo ?? '',
        tipoPecaId: b.tipoPecaId, tipoPecaNome: tipo?.nome ?? '?', nome: b.nome, descricao: b.descricao ?? null,
        quantidade, valorUnitario: tipo?.valorReferencia ?? 0, valorTotal: quantidade * (tipo?.valorReferencia ?? 0),
        pessoaId: b.pessoaId ?? null, pessoaNome: pessoa?.nome ?? null, dataEntrega: b.dataEntrega ?? null,
      };
      e.pecas.push(nova);
      recalcularValor(ctx, b.demandaId);
      return resp(201, nova);
    }
    if (m === 'DELETE' && id) {
      const p = e.pecas.find((x) => x.id === id);
      e.pecas = e.pecas.filter((x) => x.id !== id);
      e.evidencias = e.evidencias.filter((ev) => ev.pecaId !== id);
      if (p) {
        const d = ctx.demandas.find((x) => x.id === p.demandaId);
        if (d && !e.pecas.some((x) => x.demandaId === d.id)) d.valorCalculado = null;
        else recalcularValor(ctx, p.demandaId);
      }
      return resp(204, null);
    }
  }

  if (recurso === 'atividades') {
    if (m === 'GET' && !id) {
      const demandaId = req.params.get('demandaId');
      const content = demandaId ? e.atividades.filter((a) => a.demandaId === demandaId) : e.atividades;
      return resp(200, { content, totalElements: content.length, totalPages: 1, number: 0, size: content.length });
    }
    if (m === 'POST') {
      const ta = TIPOS_ATIVIDADE.find((t) => t.id === b.tipoAtividadeId);
      const pessoa = ctx.pessoas.find((p) => p.id === b.pessoaId);
      const d = ctx.demandas.find((x) => x.id === b.demandaId);
      const nova = {
        id: novoId('atv'), demandaId: b.demandaId, demandaTitulo: d?.titulo ?? '',
        tipoAtividadeId: b.tipoAtividadeId, tipoAtividadeNome: ta?.nome ?? '?',
        pessoaId: b.pessoaId ?? null, pessoaNome: pessoa?.nome ?? null,
        descricao: b.descricao ?? null, dataRealizacao: b.dataRealizacao ?? hoje(),
      };
      e.atividades.push(nova);
      return resp(201, nova);
    }
    if (m === 'DELETE' && id) {
      e.atividades = e.atividades.filter((a) => a.id !== id);
      e.evidencias = e.evidencias.filter((ev) => ev.atividadeId !== id);
      return resp(204, null);
    }
  }

  if (recurso === 'evidencias') {
    if (m === 'GET' && !id) return resp(200, e.evidencias);
    if (m === 'POST') {
      const nova = {
        id: novoId('evid'), atividadeId: b.atividadeId ?? null, pecaId: b.pecaId ?? null, tipo: b.tipo,
        conteudo: b.conteudo ?? null, descricao: b.descricao ?? null, createdAt: new Date().toISOString(),
      };
      e.evidencias.push(nova);
      return resp(201, nova);
    }
    if (m === 'DELETE' && id) {
      e.evidencias = e.evidencias.filter((ev) => ev.id !== id);
      return resp(204, null);
    }
  }

  return null;
}

function criarDemanda(ctx: MockCtx, b: any) {
  const d: any = {
    id: novoId('dem-novo'), status: 'NAO_INICIADA', dataCriacao: hoje(), dataEntregaReal: null,
    valorCalculado: null, createdAt: '', updatedAt: '',
  };
  aplicarPayloadDemanda(ctx, d, b);
  ctx.demandas.unshift(d);
  const p = ctx.pessoaAtual;
  estado!.participantes.push({
    id: novoId('part'), demandaId: d.id, pessoaId: p.id, pessoaNome: p.nome,
    papel: 'RESPONSAVEL_PRINCIPAL', dataEntrada: d.dataCriacao, dataSaida: null, observacao: null,
  });
  return resp(201, d);
}
