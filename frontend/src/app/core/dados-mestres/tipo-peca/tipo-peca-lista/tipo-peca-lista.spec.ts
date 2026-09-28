import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TipoPecaLista } from './tipo-peca-lista';

describe('TipoPecaLista', () => {
  let component: TipoPecaLista;
  let fixture: ComponentFixture<TipoPecaLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TipoPecaLista]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TipoPecaLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
