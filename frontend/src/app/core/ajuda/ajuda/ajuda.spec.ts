import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AJUDA, ChaveAjuda } from '../ajuda-conteudo';
import { Ajuda, esquecerAjudasVistas } from './ajuda';

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
    esquecerAjudasVistas();
    await TestBed.configureTestingModule({ imports: [Ajuda] }).compileComponents();
  });

  it('mostra o título e todas as perguntas e respostas da chave', () => {
    const el = criar('campanhas');
    expect(el.querySelector('summary')?.textContent).toContain(AJUDA.campanhas.titulo);
    expect(el.querySelectorAll('dt').length).toBe(AJUDA.campanhas.itens.length);
    expect(el.querySelector('dd')?.textContent).toContain(AJUDA.campanhas.itens[0].resposta);
  });

  it('na primeira visita abre e fica destacada em laranja', () => {
    const el = criar('demandas');
    expect(el.querySelector('details')?.open).toBeTrue();
    expect(el.querySelector('details')?.classList).toContain('ajuda--destaque');
    expect(el.querySelector('.ajuda__selo')).not.toBeNull();
  });

  it('nas visitas seguintes nasce recolhida e sem destaque', () => {
    criar('demandas'); // a primeira visita
    const el = criar('demandas'); // entrou na tela de novo
    expect(el.querySelector('details')?.open).toBeFalse();
    expect(el.querySelector('details')?.classList).not.toContain('ajuda--destaque');
  });

  it('abrir a ajuda não é lembrado: ao voltar à tela ela recomeça recolhida', () => {
    criar('demandas');
    const aberta = criar('demandas');
    const detalhes = aberta.querySelector('details')!;
    detalhes.open = true;
    detalhes.dispatchEvent(new Event('toggle'));
    expect(fixture.componentInstance.aberta()).toBeTrue();
    expect(detalhes.classList).not.toContain('ajuda--destaque'); // aberta à mão não ganha o destaque

    const outra = criar('demandas');
    expect(outra.querySelector('details')?.open).toBeFalse();
  });

  it('cada tela tem a sua primeira vez', () => {
    criar('demandas');
    const el = criar('campanhas');
    expect(el.querySelector('details')?.open).toBeTrue();
  });

  it('quem já tinha uma escolha gravada na versão antiga não vê o destaque de novo', () => {
    localStorage.setItem('sgd.ajuda.demandas', 'fechada');
    const el = criar('demandas');
    expect(el.querySelector('details')?.open).toBeFalse();
  });

  it('se o localStorage lançar erro, a primeira vez aparece e a segunda recolhe (memória da sessão)', () => {
    spyOn(Storage.prototype, 'getItem').and.throwError('bloqueado');
    spyOn(Storage.prototype, 'setItem').and.throwError('bloqueado');
    expect(criar('relatorio').querySelector('details')?.open).toBeTrue();
    expect(criar('relatorio').querySelector('details')?.open).toBeFalse();
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
