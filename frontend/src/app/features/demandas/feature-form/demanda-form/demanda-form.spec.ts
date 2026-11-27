import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { DemandaForm } from './demanda-form';

describe('DemandaForm', () => {
  let component: DemandaForm;
  let fixture: ComponentFixture<DemandaForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandaForm],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(DemandaForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('avisa quando o código digitado já está em uso e some ao apagar', fakeAsync(() => {
    const http = TestBed.inject(HttpTestingController);

    component.form.controls.codigo.setValue('BC2611EG');
    tick(400);
    const req = http.expectOne((r) => r.url.endsWith('/demandas/por-codigo') && r.params.get('codigo') === 'BC2611EG');
    req.flush([{ id: 'x1', codigo: 'BC2611EG', titulo: 'CUSTEIO_26_27', status: 'CONCLUIDA' }]);
    expect(component.repetidas().length).toBe(1);

    component.form.controls.codigo.setValue('');
    tick(400);
    expect(component.repetidas().length).toBe(0);
  }));

  it('não consulta com código curto demais', fakeAsync(() => {
    const http = TestBed.inject(HttpTestingController);

    component.form.controls.codigo.setValue('BC1');
    tick(400);

    http.expectNone((r) => r.url.endsWith('/demandas/por-codigo'));
    expect(component.repetidas().length).toBe(0);
  }));
});
