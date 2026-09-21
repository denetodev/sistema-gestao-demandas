import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DemandaForm } from './demanda-form';

describe('DemandaForm', () => {
  let component: DemandaForm;
  let fixture: ComponentFixture<DemandaForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandaForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DemandaForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
