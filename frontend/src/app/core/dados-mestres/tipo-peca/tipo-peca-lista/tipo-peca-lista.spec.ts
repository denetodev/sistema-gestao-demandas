import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { TipoPecaLista } from './tipo-peca-lista';

describe('TipoPecaLista', () => {
  let component: TipoPecaLista;
  let fixture: ComponentFixture<TipoPecaLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TipoPecaLista],
      providers: provideTesteApp(),
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
