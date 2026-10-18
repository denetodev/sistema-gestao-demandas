import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { DemandaLista } from './demanda-lista';

describe('DemandaLista', () => {
  let component: DemandaLista;
  let fixture: ComponentFixture<DemandaLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandaLista],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(DemandaLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('grupo de fotos do time', () => {
    const time = (n: number) => ({
      participantes: Array.from({ length: n }, (_, k) => ({ pessoaId: 'p' + k, nome: 'Pessoa ' + k + ' Sobrenome', fotoUrl: null })),
    }) as never;

    it('mostra até 4 rostos e o resto vira +N', () => {
      const lista = fixture.componentInstance;
      expect(lista.rostos(time(3)).length).toBe(3);
      expect(lista.restantes(time(3))).toBe(0);
      expect(lista.rostos(time(7)).length).toBe(4);
      expect(lista.restantes(time(7))).toBe(3);
    });

    it('iniciais: primeiro e último nome, e um nome só vira uma letra', () => {
      const lista = fixture.componentInstance;
      expect(lista.iniciaisDe('Deusdete Dias Torres Neto')).toBe('DN');
      expect(lista.iniciaisDe('Carol')).toBe('C');
    });

    it('o texto do mouse traz todos os nomes, um por linha, mesmo os que viraram +N', () => {
      expect(fixture.componentInstance.nomesDoTime(time(6)).split('\n').length).toBe(6);
    });

    it('demanda sem a lista de participantes não quebra', () => {
      expect(fixture.componentInstance.rostos({} as never)).toEqual([]);
      expect(fixture.componentInstance.restantes({} as never)).toBe(0);
    });
  });
});
