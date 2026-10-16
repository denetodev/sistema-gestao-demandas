import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EquipamentoService } from './equipamento.service';

describe('EquipamentoService', () => {
  let service: EquipamentoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(EquipamentoService);
    http = TestBed.inject(HttpTestingController);
  });

  it('autoriza com PATCH sem corpo de dados', () => {
    service.autorizar('e1').subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/emprestimos/e1/autorizar'));
    expect(req.request.method).toBe('PATCH');
    req.flush({});
  });

  it('recusa levando o motivo', () => {
    service.recusar('e1', 'sem agenda').subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/emprestimos/e1/recusar'));
    expect(req.request.body).toEqual({ motivo: 'sem agenda' });
    req.flush({});
  });

  it('devolução total manda itemIds nulo; parcial manda só os escolhidos', () => {
    service.devolver('e1', null, null).subscribe();
    expect(http.expectOne((r) => r.url.endsWith('/emprestimos/e1/devolver')).request.body).toEqual({ itemIds: null, observacoes: null });

    service.devolver('e2', ['i1'], 'riscado').subscribe();
    expect(http.expectOne((r) => r.url.endsWith('/emprestimos/e2/devolver')).request.body).toEqual({ itemIds: ['i1'], observacoes: 'riscado' });
  });

  it('envia a foto como multipart com o momento na query e lista as fotos', () => {
    const arquivo = new File([new Uint8Array([0xff, 0xd8, 0xff])], 'foto.jpg', { type: 'image/jpeg' });
    service.enviarFoto('e1', 'SAIDA', arquivo).subscribe();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/emprestimos/e1/fotos'));
    expect(req.request.params.get('momento')).toBe('SAIDA');
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('arquivo')).toBeTruthy();
    req.flush({});

    service.fotos('e1').subscribe();
    expect(http.expectOne((r) => r.method === 'GET' && r.url.endsWith('/emprestimos/e1/fotos')).request.method).toBe('GET');
  });

  it('busca demandas pela diretoria da pessoa, com o texto digitado', () => {
    service.buscarDemandas('BC26').subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/demandas'));
    expect(req.request.params.get('escopo')).toBe('DIRETORIA');
    expect(req.request.params.get('busca')).toBe('BC26');
    req.flush({ content: [] });
  });

  it('liga o acesso da equipe e define o papel da pessoa nos endpoints certos', () => {
    service.definirAcessoEquipe('a1', true).subscribe();
    const equipe = http.expectOne((r) => r.url.endsWith('/areas/a1/acesso-equipamentos'));
    expect(equipe.request.method).toBe('PATCH');
    expect(equipe.request.body).toEqual({ acessa: true });

    service.definirPapel('p1', null).subscribe();
    const papel = http.expectOne((r) => r.url.endsWith('/pessoas/p1/papel-equipamentos'));
    expect(papel.request.body).toEqual({ papel: null });
  });
});
