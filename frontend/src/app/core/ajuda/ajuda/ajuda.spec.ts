import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AJUDA, ChaveAjuda } from '../ajuda-conteudo';
import { Ajuda } from './ajuda';

describe('Ajuda', () => {
  let fixture: ComponentFixture<Ajuda>;

  function criar(chave: ChaveAjuda) {
    fixture = TestBed.createComponent(Ajuda);
    fixture.componentRef.setInput('chave', chave);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({ imports: [Ajuda] }).compileComponents();
  });

  it('mostra o título e todas as perguntas e respostas da chave', () => {
    const el = criar('campanhas');
    expect(el.querySelector('summary')?.textContent).toContain(AJUDA.campanhas.titulo);
    expect(el.querySelectorAll('dt').length).toBe(AJUDA.campanhas.itens.length);
    expect(el.querySelector('dd')?.textContent).toContain(AJUDA.campanhas.itens[0].resposta);
  });

  it('abre na primeira visita', () => {
    const el = criar('demandas');
    expect(el.querySelector('details')?.open).toBeTrue();
  });

  it('lembra que a pessoa recolheu', () => {
    localStorage.setItem('sgd.ajuda.demandas', 'fechada');
    const el = criar('demandas');
    expect(el.querySelector('details')?.open).toBeFalse();
  });

  it('funciona mesmo se o localStorage lançar erro', () => {
    spyOn(Storage.prototype, 'getItem').and.throwError('bloqueado');
    const el = criar('relatorio');
    expect(el.querySelector('details')?.open).toBeTrue();
  });

  it('todas as chaves têm título e ao menos um item preenchido', () => {
    for (const ajuda of Object.values(AJUDA)) {
      expect(ajuda.titulo.trim()).not.toBe('');
      expect(ajuda.itens.length).toBeGreaterThan(0);
      for (const item of ajuda.itens) {
        expect(item.pergunta.trim()).not.toBe('');
        expect(item.resposta.trim()).not.toBe('');
      }
    }
  });
});
