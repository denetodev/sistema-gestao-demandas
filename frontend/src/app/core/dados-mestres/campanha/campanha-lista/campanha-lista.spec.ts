import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { CampanhaLista } from './campanha-lista';

describe('CampanhaLista', () => {
  let component: CampanhaLista;
  let fixture: ComponentFixture<CampanhaLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampanhaLista],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampanhaLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
