import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DemandaLista } from './demanda-lista';

describe('DemandaLista', () => {
  let component: DemandaLista;
  let fixture: ComponentFixture<DemandaLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandaLista]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DemandaLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
