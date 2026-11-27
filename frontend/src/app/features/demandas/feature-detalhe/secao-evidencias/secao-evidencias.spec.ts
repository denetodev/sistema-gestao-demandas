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

  function escolher(...arquivos: File[]) {
    const evento = { target: { files: arquivos } } as unknown as Event;
    componente.escolherArquivo(evento);
  }

  const png = (nome: string, conteudo = 'x') => new File([conteudo], nome, { type: 'image/png', lastModified: 1 });

  it('começa em modo imagem e sem permitir salvar', () => {
    expect(componente.modo()).toBe('IMAGEM');
    expect(componente.podeSalvar()).toBe(false);
  });

  it('aceita PNG e JPEG pequenos', () => {
    escolher(png('a.png'));
    expect(componente.arquivos()[0].erro).toBeNull();
    expect(componente.podeSalvar()).toBe(true);

    escolher(new File(['y'], 'b.jpg', { type: 'image/jpeg', lastModified: 2 }));
    expect(componente.validos().length).toBe(2);
  });

  it('escolhe várias imagens de uma vez e mostra no botão quantas serão enviadas', () => {
    escolher(png('1.png', 'a'), png('2.png', 'bb'), png('3.png', 'ccc'));

    expect(componente.arquivos().length).toBe(3);
    expect(componente.rotuloAdicionar()).toBe('Adicionar 3 evidências');
  });

  it('juntar de duas escolhas soma, e a mesma imagem escolhida duas vezes não duplica', () => {
    escolher(png('1.png'), png('2.png', 'bb'));
    escolher(png('1.png'), png('3.png', 'ccc'));

    expect(componente.arquivos().map((a) => a.arquivo.name)).toEqual(['1.png', '2.png', '3.png']);
  });

  it('remover tira da lista', () => {
    escolher(png('1.png'), png('2.png', 'bb'));
    componente.removerArquivo(componente.arquivos()[0].id);

    expect(componente.arquivos().map((a) => a.arquivo.name)).toEqual(['2.png']);
  });

  it('recusa GIF, PDF e imagens acima de 5 MB, mas mantém as boas', () => {
    escolher(
      new File(['x'], 'a.gif', { type: 'image/gif', lastModified: 1 }),
      new File(['x'], 'a.pdf', { type: 'application/pdf', lastModified: 2 }),
      new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.png', { type: 'image/png', lastModified: 3 }),
    );
    const [gif, pdf, grande] = componente.arquivos();
    expect(gif.erro).toContain('JPEG ou PNG');
    expect(pdf.erro).toContain('JPEG ou PNG');
    expect(grande.erro).toContain('5 MB');
    expect(componente.podeSalvar()).toBe(false);

    escolher(png('boa.png', 'zz'));
    expect(componente.validos().length).toBe(1);
    expect(componente.podeSalvar()).toBe(true);
    expect(componente.rotuloAdicionar()).toBe('Adicionar evidência');
  });

  it('abrir de novo começa sem as imagens da vez anterior', () => {
    escolher(png('1.png'));
    componente.abrir();

    expect(componente.arquivos().length).toBe(0);
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
