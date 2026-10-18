import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { CampanhaLista } from './campanha-lista';

describe('CampanhaLista', () => {
  let component: CampanhaLista;
  let fixture: ComponentFixture<CampanhaLista>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampanhaLista],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampanhaLista);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('janela Nova campanha', () => {
    it('só aceita salvar com nome, e nome só de espaços não vale', () => {
      component.abrirNovo();
      expect(component.form.invalid).toBeTrue();

      component.form.patchValue({ nome: '    ' });
      expect(component.form.invalid).toBeTrue();

      component.form.patchValue({ nome: 'Plano Safra' });
      expect(component.form.valid).toBeTrue();
    });

    it('barra data de fim antes da data de início', () => {
      component.abrirNovo();
      component.form.patchValue({
        nome: 'Campanha', dataInicio: new Date(2026, 9, 20), dataFim: new Date(2026, 9, 10),
      });
      expect(component.form.hasError('fimAntesDoInicio')).toBeTrue();
      expect(component.form.invalid).toBeTrue();

      component.form.patchValue({ dataFim: new Date(2026, 9, 25) });
      expect(component.form.valid).toBeTrue();
    });

    it('código passa de 40 caracteres? o formulário recusa', () => {
      component.abrirNovo();
      component.form.patchValue({ nome: 'Campanha', codigo: 'X'.repeat(41) });
      expect(component.form.controls.codigo.invalid).toBeTrue();
    });

    it('manda nome aparado e código em maiúsculas, sem código vira nulo', async () => {
      component.abrirNovo();
      component.form.patchValue({ nome: '  Outubro das MPEs  ', codigo: ' bc2609cr ' });

      const promessa = component.salvar();
      const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/campanhas'));
      expect(req.request.body.nome).toBe('Outubro das MPEs');
      expect(req.request.body.codigo).toBe('BC2609CR');
      req.flush({});
      await promessa;
      expect(component.dialogAberto()).toBeFalse();

      component.abrirNovo();
      component.form.patchValue({ nome: 'Sem código', codigo: '   ' });
      const segunda = component.salvar();
      const req2 = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/campanhas'));
      expect(req2.request.body.codigo).toBeNull();
      req2.flush({});
      await segunda;
    });

    it('mostra o motivo que o servidor devolveu quando não consegue salvar', async () => {
      component.abrirNovo();
      component.form.patchValue({ nome: 'Campanha' });

      const promessa = component.salvar();
      http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/campanhas'))
        .flush({ mensagem: 'Projeto não encontrado' }, { status: 404, statusText: 'Not Found' });
      await promessa;

      expect(component.erro()).toBe('Projeto não encontrado');
      expect(component.dialogAberto()).toBeTrue();
      expect(component.salvando()).toBeFalse();
    });

    it('o erro do nome só aparece depois que a pessoa mexe na caixa', () => {
      component.abrirNovo();
      expect(component.mostrarErro('nome')).toBeFalse();

      component.form.controls.nome.markAsTouched();
      expect(component.mostrarErro('nome')).toBeTrue();
    });
  });
});
