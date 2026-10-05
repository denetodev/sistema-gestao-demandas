import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SecaoEvidencias } from './secao-evidencias';
import { provideTesteApp } from '../../../../../testing/providers';

describe('SecaoEvidencias (upload e link)', () => {
  let fixture: ComponentFixture<SecaoEvidencias>;
  let componente: SecaoEvidencias;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [SecaoEvidencias], providers: provideTesteApp() });
    fixture = TestBed.createComponent(SecaoEvidencias);
    fixture.componentRef.setInput('todas', []);
    fixture.componentRef.setInput('atividades', []);
    fixture.componentRef.setInput('pecas', []);
    fixture.componentRef.setInput('editavel', true);
    fixture.detectChanges();
    componente = fixture.componentInstance;
    componente.abrir();
  });

  function escolher(arquivo: File | null) {
    const evento = { target: { files: arquivo ? [arquivo] : [] } } as unknown as Event;
    componente.escolherArquivo(evento);
  }

  it('começa em modo imagem e sem permitir salvar', () => {
    expect(componente.modo()).toBe('IMAGEM');
    expect(componente.podeSalvar()).toBe(false);
  });

  it('aceita PNG e JPEG pequenos', () => {
    escolher(new File(['x'], 'a.png', { type: 'image/png' }));
    expect(componente.erroArquivo()).toBeNull();
    expect(componente.podeSalvar()).toBe(true);

    escolher(new File(['x'], 'a.jpg', { type: 'image/jpeg' }));
    expect(componente.podeSalvar()).toBe(true);
  });

  it('recusa GIF, PDF e imagens acima de 5 MB', () => {
    escolher(new File(['x'], 'a.gif', { type: 'image/gif' }));
    expect(componente.erroArquivo()).toContain('JPEG ou PNG');
    expect(componente.podeSalvar()).toBe(false);

    escolher(new File(['x'], 'a.pdf', { type: 'application/pdf' }));
    expect(componente.podeSalvar()).toBe(false);

    const grande = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.png', { type: 'image/png' });
    escolher(grande);
    expect(componente.erroArquivo()).toContain('5 MB');
    expect(componente.podeSalvar()).toBe(false);
  });

  it('no modo link só libera URL http ou https', () => {
    componente.modo.set('LINK');

    componente.form.controls.link.setValue('javascript:alert(1)');
    expect(componente.podeSalvar()).toBe(false);

    componente.form.controls.link.setValue('exemplo.com/pagina');
    expect(componente.podeSalvar()).toBe(false);

    componente.form.controls.link.setValue(' https://exemplo.com/tarefa/1 ');
    expect(componente.podeSalvar()).toBe(true);
  });

  it('formata o tamanho do arquivo', () => {
    expect(componente.tamanho(null)).toBe('');
    expect(componente.tamanho(100)).toBe('1 KB');
    expect(componente.tamanho(2048)).toBe('2 KB');
    expect(componente.tamanho(3 * 1024 * 1024)).toBe('3.0 MB');
  });
});
