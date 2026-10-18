import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { provideTesteApp } from '../../../../testing/providers';
import { Notificacao, NotificacoesResposta, Sino } from './sino';

function aviso(parcial: Partial<Notificacao>): Notificacao {
  return {
    id: 'x', tipo: 'ADICIONADO_EM_DEMANDA', titulo: 'Você entrou numa demanda', texto: 'Ana te adicionou em X.',
    link: '/demandas/1', quantidade: 1, lida: false, em: '2026-10-18T10:00:00Z', ...parcial,
  };
}

describe('Sino (avisos do topo)', () => {
  let fixture: ComponentFixture<Sino>;
  let sino: Sino;
  let http: HttpTestingController;

  async function abrirComAvisos(resposta: NotificacoesResposta) {
    await TestBed.configureTestingModule({ imports: [Sino], providers: provideTesteApp() }).compileComponents();
    fixture = TestBed.createComponent(Sino);
    sino = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/notificacoes')).flush(resposta);
    // o resource aplica a resposta num microtask; whenStable não serve aqui porque o sino tem um intervalo ativo
    await new Promise((r) => setTimeout(r));
    TestBed.tick();
    fixture.detectChanges();
  }

  it('sem avisos não mostra selo e diz que está tudo em dia', async () => {
    await abrirComAvisos({ itens: [], naoLidas: 0 });

    expect(sino.naoLidas()).toBe(0);
    expect((fixture.nativeElement as HTMLElement).querySelector('.sino__selo')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('button')?.getAttribute('aria-label')).toBe('Avisos');
  });

  it('o selo mostra a quantidade sem ler e vira 9+ acima de nove', async () => {
    await abrirComAvisos({ itens: [aviso({})], naoLidas: 4 });
    expect((fixture.nativeElement as HTMLElement).querySelector('.sino__selo')?.textContent?.trim()).toBe('4');
    expect(sino.selo()).toBe('4');
  });

  it('acima de 9 o selo diz 9+', async () => {
    await abrirComAvisos({ itens: [aviso({})], naoLidas: 14 });
    expect(sino.selo()).toBe('9+');
  });

  it('ao abrir, avisa o servidor só se há "te adicionaram" ainda não visto', async () => {
    await abrirComAvisos({ itens: [aviso({ id: 'a' })], naoLidas: 1 });

    const promessa = sino.aoAbrir();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/notificacoes/vistas'));
    req.flush(null);
    await promessa;
  });

  it('só pendências (cadastros, empréstimos) ou tudo já lido: abrir não chama o servidor', async () => {
    await abrirComAvisos({
      itens: [aviso({ tipo: 'CADASTROS_PENDENTES', id: 'c' }), aviso({ id: 'd', lida: true })],
      naoLidas: 1,
    });

    await sino.aoAbrir();

    http.expectNone((r) => r.method === 'POST');
  });

  it('clicar num aviso leva para a tela dele e fecha o painel', async () => {
    await abrirComAvisos({ itens: [aviso({ link: '/demandas/dem-9' })], naoLidas: 1 });
    const navegar = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    const esconder = spyOn(sino.popover(), 'hide');

    sino.abrir(sino.itens()[0]);

    expect(navegar).toHaveBeenCalledWith('/demandas/dem-9');
    expect(esconder).toHaveBeenCalled();
  });

  it('cada tipo de aviso tem o seu ícone', async () => {
    await abrirComAvisos({ itens: [], naoLidas: 0 });
    expect(sino.icone('CADASTROS_PENDENTES')).toBe('pi-user-plus');
    expect(sino.icone('EMPRESTIMOS_AGUARDANDO')).toBe('pi-inbox');
    expect(sino.icone('EMPRESTIMOS_ATRASADOS')).toBe('pi-exclamation-triangle');
    expect(sino.icone('ADICIONADO_EM_DEMANDA')).toBe('pi-briefcase');
  });
});
