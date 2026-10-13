import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PessoaService } from './pessoa.service';

describe('PessoaService (vínculo sugerido pelo CPF)', () => {
  let service: PessoaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PessoaService);
    http = TestBed.inject(HttpTestingController);
  });

  it('busca o resumo do cadastro importado sugerido para a conta pendente', () => {
    service.vinculoSugerido('p1').subscribe((r) => expect(r.demandas).toBe(7));
    const req = http.expectOne((r) => r.url.endsWith('/pessoas/p1/vinculo-sugerido'));
    expect(req.request.method).toBe('GET');
    req.flush({ pessoaId: 'x', nome: 'Klebson', demandas: 7, atividades: 1, pecas: 2, jaTemConta: false, impedimento: null });
  });

  it('confirma o vínculo com POST sem corpo de dados', () => {
    service.confirmarVinculoSugerido('p1').subscribe();
    const req = http.expectOne((r) => r.url.endsWith('/pessoas/p1/confirmar-vinculo'));
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
