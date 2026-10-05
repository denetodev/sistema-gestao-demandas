import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { DemandanteLista } from './demandante-lista';

describe('DemandanteLista', () => {
  let component: DemandanteLista;
  let fixture: ComponentFixture<DemandanteLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandanteLista],
      providers: provideTesteApp(),
    }).compileComponents();

    fixture = TestBed.createComponent(DemandanteLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
