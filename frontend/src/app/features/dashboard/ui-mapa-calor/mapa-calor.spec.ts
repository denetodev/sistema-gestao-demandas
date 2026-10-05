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
});
