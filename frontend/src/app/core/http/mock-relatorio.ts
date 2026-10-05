import { HttpRequest } from '@angular/common/http';

// MOCK TEMPORÁRIO (remover junto com mock-demanda.ts). Espelha as regras do
// RelatorioMensalService do backend sobre o estado em memória do mock.

interface EstadoMock {
  pecas: any[];
  atividades: any[];
}

interface ContextoMock {
  demandas: any[];
  pessoas: any[];
  pessoaAtual: any;
}

interface Guardado {
  status: 'ABERTO' | 'APROVADO';
  observacoes: string | null;
  excluidas: string[];
  aprovadoEm: string | null;
  congelado: any | null;
}

const guardados = new Map<string, Guardado>();
const chave = (pessoaId: string, mes: string) => `${pessoaId}|${mes}`;

export interface RespostaMock {
  status: number;
  body: unknown;
}

const erro = (status: number, message: string): RespostaMock => ({ status, body: { message } });

/** `partes` = caminho depois de /relatorios, ex.: ['2026-03', 'aprovar'] ou ['resumo']. */
export function relatorioMock(
  req: HttpRequest<unknown>, ctx: ContextoMock, e: EstadoMock, partes: string[],
): RespostaMock {
  const eu = ctx.pessoaAtual;
  const ehGA = eu.perfil === 'ADMIN' || eu.perfil === 'GESTOR';
  const [primeiro, acao] = partes;
  const m = req.method;
  const mesAtual = new Date().toISOString().slice(0, 7);

  if (eu.perfil === 'VISUALIZADOR') return erro(403, 'Visualizador não tem relatório mensal');

  if (primeiro === 'resumo' && m === 'GET') {
    if (!ehGA) return erro(403, 'Resumo de relatórios restrito a Gestor/Admin');
    const mes = req.params.get('mes') ?? mesAtual;
    const lista = ctx.pessoas
      .filter((p) => p.status === 'ATIVO' && p.aprovadoEm && p.perfil === 'PROFISSIONAL')
      .map((p) => {
        const g = guardados.get(chave(p.id, mes));
        return {
          pessoaId: p.id, nome: p.nome, area: p.areaNome,
          situacao: g ? g.status : 'NAO_INICIADO',
          atividadesNoMes: e.atividades.filter((a) => a.pessoaId === p.id && a.dataRealizacao?.startsWith(mes)).length,
          aprovadoEm: g?.aprovadoEm ?? null,
        };
      });
    return { status: 200, body: lista };
  }

  const mes = primeiro;
  if (!/^\d{4}-\d{2}$/.test(mes)) return erro(400, 'Mês inválido');
  if (mes > mesAtual) return erro(400, `Não é possível gerar relatório de mês futuro: ${mes}`);

  const pessoaParam = req.params.get('pessoaId');
  const donoId = pessoaParam && pessoaParam !== eu.id ? pessoaParam : eu.id;
  if (donoId !== eu.id && !ehGA) return erro(403, 'Só Gestor/Admin consultam relatórios de outras pessoas');
  const dono = ctx.pessoas.find((p) => p.id === donoId) ?? eu;
  const k = chave(dono.id, mes);

  const montar = (): any => {
    const g = guardados.get(k);
    const propria = dono.id === eu.id;
    if (g?.status === 'APROVADO') {
      return { ...g.congelado, podeEditar: false, podeReabrir: propria || ehGA };
    }
    return { ...ao_vivo(dono, mes, g, ctx, e), podeEditar: propria, podeReabrir: false };
  };

  if (!acao && m === 'GET') return { status: 200, body: montar() };

  if (!acao && m === 'PUT') {
    if (dono.id !== eu.id) return erro(403, 'Só a própria pessoa edita o relatório');
    const g = guardados.get(k);
    if (g?.status === 'APROVADO') return erro(409, 'Relatório aprovado: reabra para editar');
    const b: any = req.body ?? {};
    guardados.set(k, {
      status: 'ABERTO', observacoes: b.observacoes ?? null, excluidas: b.atividadesExcluidas ?? [],
      aprovadoEm: null, congelado: null,
    });
    return { status: 200, body: montar() };
  }

  if (acao === 'aprovar' && m === 'POST') {
    if (dono.id !== eu.id) return erro(403, 'Só a própria pessoa aprova o relatório');
    const g = guardados.get(k);
    if (g?.status === 'APROVADO') return erro(409, 'Relatório aprovado: reabra para editar');
    const vivo = ao_vivo(dono, mes, g, ctx, e);
    const aprovadoEm = new Date().toISOString();
    const congelado = {
      ...vivo, status: 'APROVADO', aprovadoEm, atividades: vivo.atividades.filter((a: any) => a.incluida),
    };
    guardados.set(k, {
      status: 'APROVADO', observacoes: g?.observacoes ?? null, excluidas: g?.excluidas ?? [], aprovadoEm, congelado,
    });
    return { status: 200, body: { ...congelado, podeEditar: false, podeReabrir: true } };
  }

  if (acao === 'reabrir' && m === 'POST') {
    const g = guardados.get(k);
    if (!g) return erro(404, `Relatório não encontrado para ${mes}`);
    if (g.status !== 'APROVADO') return erro(409, `O relatório de ${mes} não está aprovado`);
    guardados.set(k, { ...g, status: 'ABERTO', aprovadoEm: null, congelado: null });
    return { status: 200, body: montar() };
  }

  return erro(404, 'Rota de relatório não simulada');
}

function ao_vivo(dono: any, mes: string, g: Guardado | undefined, ctx: ContextoMock, e: EstadoMock) {
  const excluidas = new Set(g?.excluidas ?? []);
  const atividades = e.atividades
    .filter((a) => a.pessoaId === dono.id && a.dataRealizacao?.startsWith(mes))
    .sort((a, b) => a.dataRealizacao.localeCompare(b.dataRealizacao))
    .map((a) => ({
      id: a.id, data: a.dataRealizacao, demandaId: a.demandaId ?? null, demandaTitulo: a.demandaTitulo ?? null,
      tipo: a.tipoAtividadeNome, descricao: a.descricao, incluida: !excluidas.has(a.id),
    }));
  const canceladas = new Set(ctx.demandas.filter((d) => d.status === 'CANCELADA').map((d) => d.id));
  const pecas = e.pecas
    .filter((p) => p.pessoaId === dono.id && p.dataEntrega?.startsWith(mes) && !canceladas.has(p.demandaId))
    .map((p) => ({
      id: p.id, demandaTitulo: p.demandaTitulo, nome: p.nome, tipo: p.tipoPecaNome,
      quantidade: p.quantidade, valorUnitario: p.valorUnitario, valorTotal: p.valorTotal,
    }));
  return {
    pessoaId: dono.id, pessoaNome: dono.nome, diretoria: dono.diretoriaNome, area: dono.areaNome,
    cargo: dono.cargoNome ?? null, mes, status: 'ABERTO', aprovadoEm: null,
    observacoes: g?.observacoes ?? null, atividades, pecas,
    valorReferencia: pecas.reduce((t, p) => t + p.valorTotal, 0),
  };
}
