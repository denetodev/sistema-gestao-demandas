import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { erroHttpInterceptor } from './erro-http.interceptor';

describe('erroHttpInterceptor', () => {
  let http: HttpClient;
  let controle: HttpTestingController;
  let aviso: jasmine.Spy;

  beforeEach(() => {
    aviso = jasmine.createSpy('add');
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([erroHttpInterceptor])),
        provideHttpClientTesting(),
        { provide: MessageService, useValue: { add: aviso } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controle = TestBed.inject(HttpTestingController);
  });

  function falhar(url: string, status: number, corpo: object | null) {
    http.get(url).subscribe({ error: () => undefined });
    controle.expectOne(url).flush(corpo, { status, statusText: 'x' });
  }

  it('mostra o motivo que a API devolve em "mensagem"', () => {
    falhar('/areas', 409, { mensagem: 'Já existe uma equipe com esse nome nessa diretoria' });

    expect(aviso).toHaveBeenCalledWith(jasmine.objectContaining({
      summary: 'Conflito',
      detail: 'Já existe uma equipe com esse nome nessa diretoria',
    }));
  });

  it('sem motivo no corpo, usa o texto padrão de cada status', () => {
    falhar('/a', 409, null);
    falhar('/b', 400, {});
    falhar('/c', 500, null);

    const textos = aviso.calls.allArgs().map(([m]) => m.detail);
    expect(textos).toEqual([
      'Este registro está em uso e não pode ser alterado.',
      'Verifique os campos preenchidos.',
      'Tente novamente em instantes.',
    ]);
  });

  it('403 sempre diz que não tem permissão e /pessoas/me não vira aviso', () => {
    falhar('/x', 403, { mensagem: 'detalhe interno' });
    falhar('/pessoas/me', 500, null);

    expect(aviso).toHaveBeenCalledTimes(1);
    expect(aviso.calls.mostRecent().args[0].detail).toBe('Você não tem permissão para esta ação.');
  });
});
