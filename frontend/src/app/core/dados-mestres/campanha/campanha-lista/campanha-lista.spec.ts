import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampanhaLista } from './campanha-lista';

describe('CampanhaLista', () => {
  let component: CampanhaLista;
  let fixture: ComponentFixture<CampanhaLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampanhaLista]
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
