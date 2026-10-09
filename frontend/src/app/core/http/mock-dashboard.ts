import { HttpRequest } from '@angular/common/http';

// MOCK TEMPORÁRIO (remover junto com mock-demanda.ts). Espelha as regras do
// DashboardService do backend sobre o estado em memória do mock.

const INATIVAS = ['CONCLUIDA', 'CANCELADA'];

interface EstadoMock {
  participantes: any[];
  pecas: any[];
  atividades: any[];
}

interface ContextoMock {
  demandas: any[];
  pessoas: any[];
  pessoaAtual: any;
  dados: Record<string, any>;
}

function hash(texto: string): number {
  let h = 2166136261;
  for (let k = 0; k < texto.length; k++) h = Math.imul(h ^ texto.charCodeAt(k), 16777619);
  return (h >>> 0) / 4294967295;
}

export function dashboardMock(req: HttpRequest<unknown>, ctx: ContextoMock, e: EstadoMock) {
  const eu = ctx.pessoaAtual;
  const hojeIso = new Date().toISOString().slice(0, 10);
  const mes = req.params.get('mes') ?? hojeIso.slice(0, 7);
  const diretoriaParam = req.params.get('diretoriaId');
  const pessoaParam = req.params.get('pessoaId');
  const porPessoa = req.params.get('porPessoa') === 'true';

  const elegiveis = ctx.pessoas.filter(
    (p) => p.status === 'ATIVO' && p.aprovadoEm && p.perfil === 'PROFISSIONAL',
  );
  const ehGA = eu.perfil === 'ADMIN' || eu.perfil === 'GESTOR';
  const visao = ehGA || eu.perfil === 'VISUALIZADOR' ? 'DIRETORIA' : eu.referenciaAreaId ? 'EQUIPE' : 'MINHA';

  let alvo: any[];
  let titulo: string;
  let diretoriaId: string | null = null;
  if (visao === 'MINHA') {
    alvo = [eu];
    titulo = 'Meus números';
  } else if (visao === 'EQUIPE') {
    const equipe = elegiveis.filter((p) => p.areaId === eu.referenciaAreaId);
    const colega = pessoaParam ? equipe.find((p) => p.id === pessoaParam) : null;
    alvo = colega ? [colega] : equipe;
    titulo = colega
      ? `Individual: ${colega.nome}`
      : `Equipe ${eu.referenciaAreaNome} (agregado, ${equipe.length} pessoas)`;
  } else {
    diretoriaId = eu.perfil === 'VISUALIZADOR' ? eu.diretoriaId : diretoriaParam;
    alvo = elegiveis.filter((p) => !diretoriaId || p.diretoriaId === diretoriaId);
    const nome = (ctx.dados['/diretorias'] as any[]).find((d) => d.id === diretoriaId)?.nome;
    titulo = diretoriaId ? `Diretoria ${nome} (agregado)` : 'Todas as diretorias (agregado)';
  }
  const ids = new Set(alvo.map((p) => p.id));

  // Atividades: as reais do estado + ruído determinístico, para o calendário parecer de uso
  const atividadesNoDia = (pessoaId: string, iso: string, fimDeSemana: boolean) => {
    const r = hash(pessoaId + iso);
    const ruido = fimDeSemana ? (r < 0.05 ? 1 : 0) : Math.floor(r * r * 5);
    return ruido + e.atividades.filter((a) => a.pessoaId === pessoaId && a.dataRealizacao === iso).length;
  };
  const calendario: { data: string; quantidade: number }[] = [];
  const atvMesPorPessoa = new Map<string, number>();
  const hojeData = new Date(hojeIso + 'T12:00:00');
  for (let k = 0; k < 365; k++) {
    const dia = new Date(hojeData);
    dia.setDate(dia.getDate() - k);
    const iso = dia.toISOString().slice(0, 10);
    const fimDeSemana = dia.getDay() === 0 || dia.getDay() === 6;
    let qtd = 0;
    for (const p of alvo) {
      const n = atividadesNoDia(p.id, iso, fimDeSemana);
      qtd += n;
      if (iso.startsWith(mes)) atvMesPorPessoa.set(p.id, (atvMesPorPessoa.get(p.id) ?? 0) + n);
    }
    if (qtd) calendario.push({ data: iso, quantidade: qtd });
  }
  calendario.reverse();

  // Valor gerado: peças das pessoas, fora de demandas canceladas
  const canceladas = new Set(ctx.demandas.filter((d) => d.status === 'CANCELADA').map((d) => d.id));
  const ano = mes.slice(0, 4);
  const valorMesPorPessoa = new Map<string, number>();
  let valorMes = 0;
  let valorAno = 0;
  for (const p of e.pecas) {
    if (!ids.has(p.pessoaId) || canceladas.has(p.demandaId) || !p.dataEntrega?.startsWith(ano)) continue;
    valorAno += p.valorTotal;
    if (p.dataEntrega.startsWith(mes)) {
      valorMes += p.valorTotal;
      valorMesPorPessoa.set(p.pessoaId, (valorMesPorPessoa.get(p.pessoaId) ?? 0) + p.valorTotal);
    }
  }

  // Demandas: por participação (minha/equipe) ou por diretoria
  const demandasPorPessoa = new Map<string, Set<string>>();
  for (const part of e.participantes) {
    if (!ids.has(part.pessoaId)) continue;
    const set = demandasPorPessoa.get(part.pessoaId) ?? new Set<string>();
    set.add(part.demandaId);
    demandasPorPessoa.set(part.pessoaId, set);
  }
  const porId = new Map(ctx.demandas.map((d) => [d.id, d]));
  const resumoDemandas =
    visao === 'DIRETORIA'
      ? ctx.demandas.filter((d) => !diretoriaId || d.diretoriaId === diretoriaId)
      : [...new Set([...demandasPorPessoa.values()].flatMap((s) => [...s]))].map((id) => porId.get(id));
  const atrasada = (d: any) => d.dataPrazo && d.dataPrazo < hojeIso && !INATIVAS.includes(d.status);
  const ativa = (d: any) => !INATIVAS.includes(d.status);

  const demandasPorStatus: Record<string, number> = {
    NAO_INICIADA: 0, EM_ANDAMENTO: 0, EM_APROVACAO: 0, CONCLUIDA: 0, CANCELADA: 0,
  };
  resumoDemandas.forEach((d) => demandasPorStatus[d.status]++);

  const doPessoa = (id: string) => [...(demandasPorPessoa.get(id) ?? [])].map((x) => porId.get(x)).filter(Boolean);
  const somaMes = (ps: any[]) => ps.reduce((t, p) => t + (valorMesPorPessoa.get(p.id) ?? 0), 0);

  const porArea =
    visao === 'DIRETORIA'
      ? (ctx.dados['/areas'] as any[])
          .map((a) => {
            const ps = alvo.filter((p) => p.areaId === a.id);
            const ds = [...new Set(ps.flatMap((p) => doPessoa(p.id)))];
            return {
              areaId: a.id, area: a.nome, pessoas: ps.length,
              demandasAtivas: ds.filter(ativa).length,
              atividadesNoMes: ps.reduce((t, p) => t + (atvMesPorPessoa.get(p.id) ?? 0), 0),
              demandasAtrasadas: ds.filter(atrasada).length,
              valorNoMes: somaMes(ps),
            };
          })
          .filter((l) => l.pessoas > 0)
      : [];

  const linhasPessoa =
    porPessoa && ehGA
      ? alvo.map((p) => ({
          pessoaId: p.id, nome: p.nome, area: p.areaNome, diretoria: p.diretoriaNome,
          demandasAtivas: doPessoa(p.id).filter(ativa).length,
          atividadesNoMes: atvMesPorPessoa.get(p.id) ?? 0,
          valorNoMes: valorMesPorPessoa.get(p.id) ?? 0,
        }))
      : [];

  return {
    visao, titulo, mes,
    resumo: {
      pessoas: alvo.length,
      demandasPorStatus,
      demandasAtrasadas: resumoDemandas.filter(atrasada).length,
      atividadesNoMes: [...atvMesPorPessoa.values()].reduce((t, n) => t + n, 0),
      valorNoMes: valorMes,
      valorNoAno: valorAno,
    },
    calendario,
    porArea,
    porPessoa: linhasPessoa,
  };
}
