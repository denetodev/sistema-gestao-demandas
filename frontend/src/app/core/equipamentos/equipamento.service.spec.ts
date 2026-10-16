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

  it('corrige o pedido com PUT e prorroga com PATCH levando o motivo', () => {
    service.editar('e1', {
      retiranteNome: 'João', retiranteChaveC: null, retiranteContato: null, finalidade: null, externo: true,
      localUso: null, retiradaPrevista: null, previstaDevolucao: '2026-10-20T15:00:00.000Z', demandaId: null,
    }).subscribe();
    const put = http.expectOne((r) => r.method === 'PUT' && r.url.endsWith('/emprestimos/e1'));
    expect(put.request.body.externo).toBeTrue();
    expect(put.request.body.itens).toBeUndefined();
    put.flush({});

    service.prorrogar('e1', '2026-10-25T15:00:00.000Z', 'gravação estendida').subscribe();
    const patch = http.expectOne((r) => r.url.endsWith('/emprestimos/e1/prorrogar'));
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ novaPrevista: '2026-10-25T15:00:00.000Z', motivo: 'gravação estendida' });
    patch.flush({});
  });

  it('pede a agenda da janela e a disponibilidade do período (com o pedido em correção ignorado)', () => {
    service.agenda('2026-10-18T03:00:00.000Z', '2026-11-01T03:00:00.000Z').subscribe();
    const agenda = http.expectOne((r) => r.url.endsWith('/equipamentos/agenda'));
    expect(agenda.request.params.get('de')).toBe('2026-10-18T03:00:00.000Z');
    expect(agenda.request.params.get('ate')).toBe('2026-11-01T03:00:00.000Z');
    agenda.flush({});

    service.disponibilidade('2026-10-20T12:00:00.000Z', '2026-10-22T12:00:00.000Z', 'em1').subscribe();
    const disp = http.expectOne((r) => r.url.endsWith('/equipamentos/disponibilidade'));
    expect(disp.request.params.get('inicio')).toBe('2026-10-20T12:00:00.000Z');
    expect(disp.request.params.get('ignorarEmprestimoId')).toBe('em1');
    disp.flush([]);

    service.disponibilidade('2026-10-20T12:00:00.000Z', '2026-10-22T12:00:00.000Z').subscribe();
    expect(http.expectOne((r) => r.url.endsWith('/equipamentos/disponibilidade')).request.params.has('ignorarEmprestimoId')).toBeFalse();
  });

  it('define o papel da pessoa no endpoint certo (nulo remove)', () => {
    service.definirPapel('p1', 'OPERADOR_DE_ESTUDIO').subscribe();
    const papel = http.expectOne((r) => r.url.endsWith('/pessoas/p1/papel-equipamentos'));
    expect(papel.request.method).toBe('PATCH');
    expect(papel.request.body).toEqual({ papel: 'OPERADOR_DE_ESTUDIO' });
    papel.flush({});

    service.definirPapel('p1', null).subscribe();
    expect(http.expectOne((r) => r.url.endsWith('/pessoas/p1/papel-equipamentos')).request.body).toEqual({ papel: null });
  });
});
