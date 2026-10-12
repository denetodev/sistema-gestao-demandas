import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { CampoBusca } from './campo-busca';

describe('CampoBusca', () => {
  let fixture: ComponentFixture<CampoBusca>;
  let el: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CampoBusca] }).compileComponents();
    fixture = TestBed.createComponent(CampoBusca);
    fixture.detectChanges();
    el = fixture.nativeElement;
  });

  const caixa = () => el.querySelector('input') as HTMLInputElement;

  function digitar(texto: string) {
    caixa().value = texto;
    caixa().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('so avisa a tela depois que a pessoa para de digitar', fakeAsync(() => {
    digitar('cap');
    tick(299);
    expect(fixture.componentInstance.valor()).toBe('');

    tick(1);
    expect(fixture.componentInstance.valor()).toBe('cap');
  }));

  it('digitar de novo reinicia a espera e vale so o ultimo texto', fakeAsync(() => {
    digitar('c');
    tick(200);
    digitar('captacao');
    tick(200);
    expect(fixture.componentInstance.valor()).toBe('');

    tick(100);
    expect(fixture.componentInstance.valor()).toBe('captacao');
  }));

  it('Enter busca na hora e tira os espacos das pontas', fakeAsync(() => {
    digitar('  agro ');
    caixa().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    fixture.detectChanges();

    expect(fixture.componentInstance.valor()).toBe('agro');
    tick(500); // o timer antigo foi cancelado
    expect(fixture.componentInstance.valor()).toBe('agro');
  }));

  it('o X limpa a caixa e a busca, e some quando nao ha texto', fakeAsync(() => {
    expect(el.querySelector('.campo-busca__limpar')).toBeNull();
    digitar('agro');
    expect(el.querySelector('.campo-busca__limpar')).not.toBeNull();

    (el.querySelector('.campo-busca__limpar') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.componentInstance.valor()).toBe('');
    expect(caixa().value).toBe('');
    expect(el.querySelector('.campo-busca__limpar')).toBeNull();
  }));

  it('um valor vindo de fora (a URL) aparece na caixa', () => {
    fixture.componentRef.setInput('valor', 'plano safra');
    fixture.detectChanges();

    expect(caixa().value).toBe('plano safra');
  });
});
