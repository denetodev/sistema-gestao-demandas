import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MapaCalor } from './mapa-calor';

function iso(diasAtras: number): string {
  const d = new Date();
  d.setDate(d.getDate() - diasAtras);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

describe('MapaCalor', () => {
  let fixture: ComponentFixture<MapaCalor>;

  function abrir(dias: { data: string; quantidade: number }[]) {
    fixture = TestBed.createComponent(MapaCalor);
    fixture.componentRef.setInput('dias', dias);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [MapaCalor] }));

  it('cobre 12 meses alinhados no domingo, terminando hoje', () => {
    const grade = abrir([]).grade();
    expect(grade.celulas.length).toBeGreaterThanOrEqual(365);
    expect(grade.celulas.length).toBeLessThanOrEqual(371);
    expect(grade.celulas[0].linha).toBe(1); // primeira célula é domingo
    expect(grade.celulas[grade.celulas.length - 1].data).toBe(iso(0));
  });

  it('o dia mais cheio fica no nível 4 e o vazio no nível 0', () => {
    const grade = abrir([
      { data: iso(1), quantidade: 8 },
      { data: iso(2), quantidade: 1 },
    ]).grade();
    const porData = new Map(grade.celulas.map((c) => [c.data, c]));

    expect(porData.get(iso(1))!.nivel).toBe(4);
    expect(porData.get(iso(2))!.nivel).toBe(1);
    expect(porData.get(iso(3))!.nivel).toBe(0);
    expect(grade.total).toBe(9);
  });

  describe('insights', () => {
    it('sem atividade, tudo zerado e sem dia mais cheio', () => {
      const n = abrir([]).insights();
      expect(n.total).toBe(0);
      expect(n.diasAtivos).toBe(0);
      expect(n.melhor).toBeNull();
      expect(n.mesTop).toBeNull();
      expect(n.maiorSequencia).toBe(0);
      expect(n.mediaPorDiaAtivo).toBe(0);
    });

    it('acha o dia mais cheio, a média por dia ativo e os dias com atividade', () => {
      const n = abrir([
        { data: iso(10), quantidade: 6 },
        { data: iso(20), quantidade: 2 },
        { data: iso(30), quantidade: 4 },
      ]).insights();
      expect(n.total).toBe(12);
      expect(n.diasAtivos).toBe(3);
      expect(n.mediaPorDiaAtivo).toBe(4);
      expect(n.melhor?.quantidade).toBe(6);
    });

    it('a maior sequência conta dias úteis e o fim de semana vazio não a quebra', () => {
      const comp = abrir([]);
      // monta atividade em todo dia útil dos últimos 14 dias
      const dias = comp
        .grade()
        .celulas.slice(-14)
        .filter((c) => c.diaSemana !== 0 && c.diaSemana !== 6)
        .map((c) => ({ data: c.data, quantidade: 1 }));
      fixture.componentRef.setInput('dias', dias);
      fixture.detectChanges();
      expect(comp.insights().maiorSequencia).toBe(dias.length);
    });

    it('o ritmo da semana começa na segunda e marca o dia mais movimentado', () => {
      const comp = abrir([]);
      const segunda = comp.grade().celulas.filter((c) => c.diaSemana === 1).slice(-1)[0];
      fixture.componentRef.setInput('dias', [{ data: segunda.data, quantidade: 5 }]);
      fixture.detectChanges();
      const semana = comp.insights().semana;
      expect(semana.map((d) => d.rotulo)).toEqual(['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom']);
      expect(semana[0].topo).toBeTrue();
      expect(semana[0].percentual).toBe(100);
    });
  });

  it('a leitura mostra o dia sob o mouse e volta ao convite quando o mouse sai', () => {
    const comp = abrir([{ data: iso(1), quantidade: 3 }]);
    expect(comp.leitura()).toContain('Passe o mouse');
    comp.ativa.set(comp.grade().celulas.find((c) => c.data === iso(1))!);
    expect(comp.leitura()).toContain('3 atividades');
    comp.ativa.set(null);
    expect(comp.leitura()).toContain('Passe o mouse');
  });
});
