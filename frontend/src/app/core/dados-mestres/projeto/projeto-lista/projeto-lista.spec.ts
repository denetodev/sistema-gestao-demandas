import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjetoLista } from './projeto-lista';

describe('ProjetoLista', () => {
  let component: ProjetoLista;
  let fixture: ComponentFixture<ProjetoLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjetoLista]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProjetoLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
