import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideTesteApp } from '../../../../testing/providers';
import { AgendaEquipamentos } from './agenda-equipamentos';
import type { Agenda, BlocoAgenda, LinhaAgenda } from '../equipamento.model';

function bloco(parcial: Partial<BlocoAgenda>): BlocoAgenda {
  const hoje = new Date();
  return {
    emprestimoId: 'e1', retiranteNome: 'João', status: 'RETIRADO', devolvido: false, atrasado: false, externo: false,
    localUso: null, quantidade: 1,
    inicio: new Date(hoje.getTime() - 3600_000).toISOString(), fim: new Date(hoje.getTime() + 86400_000).toISOString(),
    ...parcial,
  };
}

function linha(parcial: Partial<LinhaAgenda>): LinhaAgenda {
  return {
    equipamentoId: 'q1', nome: 'Câmera A7', patrimonio: 'PAT-1', categoriaId: 'c1', categoriaNome: 'Câmeras',
    controle: 'INDIVIDUAL', quantidadeTotal: 1, blocos: [], ...parcial,
  };
}

describe('AgendaEquipamentos (Gantt)', () => {
  let fixture: ComponentFixture<AgendaEquipamentos>;
  let component: AgendaEquipamentos;
  let http: HttpTestingController;

  async function criar(opcoes: Parameters<typeof provideTesteApp>[0], linhas: LinhaAgenda[]) {
    await TestBed.configureTestingModule({ imports: [AgendaEquipamentos], providers: provideTesteApp(opcoes) }).compileComponents();
    fixture = TestBed.createComponent(AgendaEquipamentos);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const agenda: Agenda = { de: '', ate: '', agora: new Date().toISOString(), equipamentos: linhas };
    http.expectOne((r) => r.url.endsWith('/equipamentos/agenda')).flush(agenda);
    await new Promise((r) => setTimeout(r, 0));
    fixture.detectChanges();
  }

  it('pede a janela de 14 dias começando dois dias antes de hoje', async () => {
    await TestBed.configureTestingModule({ imports: [AgendaEquipamentos], providers: provideTesteApp({ perfil: 'ADMIN' }) }).compileComponents();
    fixture = TestBed.createComponent(AgendaEquipamentos);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    const req = http.expectOne((r) => r.url.endsWith('/equipamentos/agenda'));
    const de = new Date(req.request.params.get('de')!);
    const ate = new Date(req.request.params.get('ate')!);
    expect(Math.round((ate.getTime() - de.getTime()) / 86400000)).toBe(14);
    const hoje = new Date();
    expect(Math.round((new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate()).getTime() - de.getTime()) / 86400000)).toBe(2);
  });

  it('mostra uma linha por equipamento, agrupadas por categoria, e uma barra por período', async () => {
    await criar({ perfil: 'ADMIN' }, [
      linha({ blocos: [bloco({})] }),
      linha({ equipamentoId: 'q2', nome: 'Tripé', patrimonio: null, categoriaId: 'c2', categoriaNome: 'Suportes' }),
    ]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.gantt__grupo').length).toBe(2);
    expect(el.querySelectorAll('.gantt__linha').length).toBe(2);
    expect(el.querySelectorAll('.gantt .barra').length).toBe(1);
    expect(el.querySelector('.gantt .barra')!.textContent).toContain('João');
  });

  it('clicar no nome mostra com quem está agora, as reservas e o que já voltou', async () => {
    const l = linha({
      blocos: [
        bloco({ emprestimoId: 'a', retiranteNome: 'Com a pessoa' }),
        bloco({ emprestimoId: 'b', retiranteNome: 'Reserva', status: 'AUTORIZADO' }),
        bloco({ emprestimoId: 'c', retiranteNome: 'Já voltou', devolvido: true }),
      ],
    });
    await criar({ perfil: 'ADMIN' }, [l]);

    component.abrirEquipamento(l);
    const usos = component.usosDoEquipamento();
    expect(usos.agora.map((b) => b.retiranteNome)).toEqual(['Com a pessoa']);
    expect(usos.reservas.map((b) => b.retiranteNome)).toEqual(['Reserva']);
    expect(usos.devolvidos.map((b) => b.retiranteNome)).toEqual(['Já voltou']);
  });

  it('o filtro por busca e por categoria esconde o que não bate', async () => {
    await criar({ perfil: 'ADMIN' }, [
      linha({}),
      linha({ equipamentoId: 'q2', nome: 'Tripé', patrimonio: null, categoriaId: 'c2', categoriaNome: 'Suportes' }),
    ]);
    component.busca.set('tripe');
    expect(component.grupos().flatMap((g) => g.linhas.map((l) => l.linha.nome))).toEqual(['Tripé']);

    component.busca.set('');
    component.categoriaId.set('c1');
    expect(component.grupos().flatMap((g) => g.linhas.map((l) => l.linha.nome))).toEqual(['Câmera A7']);
    expect(component.filtrosAtivos()).toBeTrue();
    component.limparFiltros();
    expect(component.grupos().length).toBe(2);
  });

  it('navegar anda meia janela e Hoje volta ao começo', async () => {
    await criar({ perfil: 'ADMIN' }, [linha({})]);
    const inicial = component.inicio().getTime();

    component.mover(1);
    expect(Math.round((component.inicio().getTime() - inicial) / 86400000)).toBe(7);
    component.mover(-1);
    expect(component.inicio().getTime()).toBe(inicial);

    component.mover(1);
    component.hoje();
    expect(component.inicio().getTime()).toBe(inicial);
  });

  it('mudar o tamanho da janela refaz o pedido', async () => {
    await criar({ perfil: 'ADMIN' }, [linha({})]);
    component.mudarDias('30');
    fixture.detectChanges();
    await new Promise((r) => setTimeout(r, 0));
    const req = http.expectOne((r) => r.url.endsWith('/equipamentos/agenda'));
    const de = new Date(req.request.params.get('de')!);
    const ate = new Date(req.request.params.get('ate')!);
    expect(Math.round((ate.getTime() - de.getTime()) / 86400000)).toBe(30);
    req.flush({ de: '', ate: '', agora: new Date().toISOString(), equipamentos: [] });
  });
});
