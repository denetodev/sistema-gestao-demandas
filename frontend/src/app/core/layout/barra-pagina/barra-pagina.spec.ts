import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../testing/providers';

import { BarraPagina } from './barra-pagina';

describe('BarraPagina', () => {
  let component: BarraPagina;
  let fixture: ComponentFixture<BarraPagina>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BarraPagina],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(BarraPagina);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
