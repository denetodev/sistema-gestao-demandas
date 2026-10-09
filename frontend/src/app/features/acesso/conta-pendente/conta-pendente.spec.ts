import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ContaPendente, SituacaoConta } from './conta-pendente';
import { provideTesteApp } from '../../../../testing/providers';

describe('ContaPendente', () => {
  let fixture: ComponentFixture<ContaPendente>;

  function abrir(situacao: SituacaoConta) {
    fixture = TestBed.createComponent(ContaPendente);
    fixture.componentRef.setInput('situacao', situacao);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => TestBed.configureTestingModule({ imports: [ContaPendente], providers: provideTesteApp() }));

  it('pendente: avisa que aguarda aprovação e oferece verificar de novo', () => {
    const el = abrir('pendente');
    expect(el.textContent).toContain('Conta aguardando aprovação');
    expect(el.textContent).toContain('Verificar novamente');
    expect(el.textContent).toContain('Sair');
  });

  it('rejeitada: não oferece verificar de novo', () => {
    const el = abrir('rejeitada');
    expect(el.textContent).toContain('Cadastro não aprovado');
    expect(el.textContent).not.toContain('Verificar novamente');
  });

  it('erro: pede para checar a API', () => {
    const el = abrir('erro');
    expect(el.textContent).toContain('Não foi possível conectar ao servidor');
    expect(el.textContent).toContain('Verificar novamente');
  });
});
