import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { CampanhaDetalheComponent } from './campanha-detalhe';

describe('CampanhaDetalheComponent', () => {
  let fixture: ComponentFixture<CampanhaDetalheComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampanhaDetalheComponent],
      providers: provideTesteApp(),
    }).compileComponents();

    fixture = TestBed.createComponent(CampanhaDetalheComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('mostra so os status que existem na campanha, na ordem do fluxo', () => {
    const c = fixture.componentInstance;
    c.detalhe.set({
      resumo: {
        id: 'c1', nome: 'BBCast Agro', codigo: null, demandas: 5, demandasAtivas: 4, pessoas: 2, valor: 100,
        pecas: 3, atividades: 4,
        porStatus: { NAO_INICIADA: 0, EM_ANDAMENTO: 1, EM_APROVACAO: 0, CONCLUIDA: 3, CANCELADA: 1 },
        primeiraDemanda: null, ultimaDemanda: null,
      },
      pessoas: [],
      demandas: [],
    });

    expect(c.situacao().map((s) => [s.status, s.quantidade])).toEqual([
      ['EM_ANDAMENTO', 1],
      ['CONCLUIDA', 3],
      ['CANCELADA', 1],
    ]);
  });
});
