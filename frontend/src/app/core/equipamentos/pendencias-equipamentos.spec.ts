import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideTesteApp } from '../../../testing/providers';
import { PendenciasEquipamentos } from './pendencias-equipamentos';

describe('PendenciasEquipamentos (selo do menu)', () => {
  function montar(opcoes: Parameters<typeof provideTesteApp>[0]) {
    TestBed.configureTestingModule({ providers: provideTesteApp(opcoes) });
    const servico = TestBed.inject(PendenciasEquipamentos);
    const http = TestBed.inject(HttpTestingController);
    TestBed.tick();
    return { servico, http };
  }

  it('quem gerencia soma os pedidos esperando e os atrasados; externo esperando deixa urgente', async () => {
    const { servico, http } = montar({ perfil: 'ADMIN' });
    http.expectOne((r) => r.url.endsWith('/emprestimos/resumo')).flush({ aguardandoAutorizacao: 2, aguardandoExterno: 1, atrasados: 1 });
    await TestBed.inject(ApplicationRef).whenStable();
    expect(servico.total()).toBe(3);
    expect(servico.urgente()).toBeTrue();
  });

  it('o Operador de estúdio conta só os atrasados (autorizar não é com ele)', async () => {
    const { servico, http } = montar({ perfil: 'PROFISSIONAL', papelEquipamentos: 'OPERADOR_DE_ESTUDIO' });
    http.expectOne((r) => r.url.endsWith('/emprestimos/resumo')).flush({ aguardandoAutorizacao: 0, aguardandoExterno: 0, atrasados: 2 });
    await TestBed.inject(ApplicationRef).whenStable();
    expect(servico.total()).toBe(2);
    expect(servico.urgente()).toBeTrue();
  });

  it('sem pendência o selo some e não fica urgente', async () => {
    const { servico, http } = montar({ perfil: 'ADMIN' });
    http.expectOne((r) => r.url.endsWith('/emprestimos/resumo')).flush({ aguardandoAutorizacao: 0, aguardandoExterno: 0, atrasados: 0 });
    await TestBed.inject(ApplicationRef).whenStable();
    expect(servico.total()).toBe(0);
    expect(servico.urgente()).toBeFalse();
  });

  it('Profissional sem papel não tem acesso e nem chama o backend', () => {
    const { http } = montar({ perfil: 'PROFISSIONAL' });
    http.expectNone((r) => r.url.endsWith('/emprestimos/resumo'));
  });
});
