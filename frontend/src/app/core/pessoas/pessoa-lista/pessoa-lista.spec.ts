import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../testing/providers';

import { PessoaLista } from './pessoa-lista';

describe('PessoaLista', () => {
  let component: PessoaLista;
  let fixture: ComponentFixture<PessoaLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PessoaLista],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(PessoaLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
