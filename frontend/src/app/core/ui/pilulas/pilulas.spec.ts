import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Pilulas } from './pilulas';

describe('Pilulas', () => {
  let fixture: ComponentFixture<Pilulas>;
  let el: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Pilulas] }).compileComponents();
    fixture = TestBed.createComponent(Pilulas);
    fixture.componentRef.setInput('opcoes', [
      { valor: 'MINHAS', rotulo: 'Minhas' },
      { valor: 'EQUIPE', rotulo: 'Minha equipe', detalhe: 'HTML' },
      { valor: 'TODAS', rotulo: 'Todas' },
    ]);
    fixture.componentRef.setInput('valor', 'MINHAS');
    fixture.detectChanges();
    el = fixture.nativeElement;
  });

  const botoes = () => Array.from(el.querySelectorAll<HTMLButtonElement>('button.pilula'));

  it('mostra uma pilula por opcao, com o detalhe ao lado do rotulo', () => {
    expect(botoes().length).toBe(3);
    expect(botoes()[1].textContent).toContain('Minha equipe');
    expect(botoes()[1].querySelector('.pilula__detalhe')?.textContent).toContain('HTML');
    expect(botoes()[0].querySelector('.pilula__detalhe')).toBeNull();
  });

  it('marca so a escolhida, tambem para leitores de tela', () => {
    expect(botoes().map((b) => b.getAttribute('aria-checked'))).toEqual(['true', 'false', 'false']);
    expect(botoes()[0].classList).toContain('pilula--ativa');
  });

  it('clicar escolhe a opcao', () => {
    botoes()[2].click();
    fixture.detectChanges();

    expect(fixture.componentInstance.valor()).toBe('TODAS');
    expect(botoes().map((b) => b.getAttribute('aria-checked'))).toEqual(['false', 'false', 'true']);
  });

  it('o grupo e uma selecao unica com nome acessivel', () => {
    fixture.componentRef.setInput('descricao', 'Escopo da lista');
    fixture.detectChanges();
    const grupo = el.querySelector('[role="radiogroup"]')!;
    expect(grupo.getAttribute('aria-label')).toBe('Escopo da lista');
  });
});
