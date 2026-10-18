import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideTesteApp } from '../../../../testing/providers';
import { EmprestimoLista } from './emprestimo-lista';
import type { Equipamento } from '../equipamento.model';

const CAMERA = {
  id: 'eq1', categoriaId: 'c1', categoriaNome: 'Câmeras', controle: 'INDIVIDUAL', nome: 'Câmera A7', patrimonio: null,
  numeroSerie: null, quantidadeTotal: 1, quantidadeDisponivel: 1, localizacao: null, observacoes: null, ativo: true, emUso: [],
} as Equipamento;

describe('EmprestimoLista (novo empréstimo e termo)', () => {
  let fixture: ComponentFixture<EmprestimoLista>;
  let component: EmprestimoLista;
  let http: HttpTestingController;

  async function criar(opcoes: Parameters<typeof provideTesteApp>[0]) {
    await TestBed.configureTestingModule({ imports: [EmprestimoLista], providers: provideTesteApp(opcoes) }).compileComponents();
    fixture = TestBed.createComponent(EmprestimoLista);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  function preencher() {
    component.form.patchValue({ retiranteNome: 'João da Silva', retiranteChaveC: 'f1234567', previstaDevolucao: new Date(Date.now() + 86400000) });
    component.selecionados.set([CAMERA]);
  }

  it('só segue para o termo com nome, equipamento e data; sem o OK nada é registrado', async () => {
    await criar({ perfil: 'ADMIN' });
    expect(component.podeContinuar()).toBeFalse();

    preencher();
    expect(component.podeContinuar()).toBeTrue();
    component.continuar();

    expect(component.etapa()).toBe('termo');
    http.expectNone((r) => r.method === 'POST' && r.url.endsWith('/emprestimos'));
  });

  it('o OK do termo registra com termoAceito e a chave C normalizada', async () => {
    await criar({ perfil: 'ADMIN' });
    preencher();
    component.continuar();

    const promessa = component.confirmarTermoERegistrar();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/emprestimos'));
    expect(req.request.body.termoAceito).toBeTrue();
    expect(req.request.body.retiranteChaveC).toBe('F1234567');
    expect(req.request.body.itens).toEqual([{ equipamentoId: 'eq1', quantidade: null }]);
    req.flush({});
    await promessa;
    expect(component.dialogNovo()).toBeFalse();
  });

  it('chave C fora do formato barra o avanço', async () => {
    await criar({ perfil: 'ADMIN' });
    preencher();
    component.form.patchValue({ retiranteChaveC: 'X123' });
    expect(component.chaveOk()).toBeFalse();
    expect(component.podeContinuar()).toBeFalse();
  });

  it('o OK do termo leva a retirada prevista (reserva) e nulo quando é para agora', async () => {
    await criar({ perfil: 'ADMIN' });
    preencher();
    const futura = new Date(Date.now() + 2 * 86400000);
    component.form.patchValue({ retiradaPrevista: futura, previstaDevolucao: new Date(futura.getTime() + 86400000) });
    component.continuar();

    const promessa = component.confirmarTermoERegistrar();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/emprestimos'));
    expect(req.request.body.retiradaPrevista).toBe(futura.toISOString());
    req.flush({});
    await promessa;

    // sem retirada informada: agora
    component.abrirNovo();
    preencher();
    component.continuar();
    const outra = component.confirmarTermoERegistrar();
    const req2 = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/emprestimos'));
    expect(req2.request.body.retiradaPrevista).toBeNull();
    req2.flush({});
    await outra;
  });

  it('a devolução precisa ser depois da retirada, senão não avança', async () => {
    await criar({ perfil: 'ADMIN' });
    preencher();
    const base = Date.now() + 86400000;
    component.form.patchValue({ retiradaPrevista: new Date(base + 3600_000), previstaDevolucao: new Date(base) });
    expect(component.periodoValido()).toBeFalse();
    expect(component.podeContinuar()).toBeFalse();

    component.form.patchValue({ previstaDevolucao: new Date(base + 7200_000) });
    expect(component.periodoValido()).toBeTrue();
    expect(component.podeContinuar()).toBeTrue();
  });

  it('com o período definido só entram os equipamentos livres nele; sem período, todos', async () => {
    await criar({ perfil: 'ADMIN' });
    http.match((r) => r.url.endsWith('/equipamentos')).forEach((r) => r.flush([
      { id: 'q1', nome: 'Câmera', categoriaNome: 'Câmeras', controle: 'INDIVIDUAL', quantidadeDisponivel: 0, quantidadeTotal: 1, emUso: [] },
      { id: 'q2', nome: 'Tripé', categoriaNome: 'Suportes', controle: 'INDIVIDUAL', quantidadeDisponivel: 1, quantidadeTotal: 1, emUso: [] },
    ]));
    await new Promise((r) => setTimeout(r, 0));
    // sem período: tudo, mesmo o que está ocupado agora (pode estar livre na semana que vem)
    expect(component.opcoesEquipamentos().map((e) => e.id)).toEqual(['q1', 'q2']);

    const ini = new Date(Date.now() + 5 * 86400000);
    component.form.patchValue({ retiradaPrevista: ini, previstaDevolucao: new Date(ini.getTime() + 86400000) });
    fixture.detectChanges();
    await new Promise((r) => setTimeout(r, 400)); // espera o debounce da consulta
    const req = http.expectOne((r) => r.url.endsWith('/equipamentos/disponibilidade'));
    expect(req.request.params.get('inicio')).toBe(ini.toISOString());
    req.flush([{ equipamentoId: 'q1', quantidadeDisponivel: 1 }, { equipamentoId: 'q2', quantidadeDisponivel: 0 }]);
    await new Promise((r) => setTimeout(r, 0));

    expect(component.periodoDefinido()).toBeTrue();
    expect(component.opcoesEquipamentos().map((e) => e.id)).toEqual(['q1']); // a câmera volta a ficar livre
  });

  it('o que estava escolhido e deixou de estar livre ao mudar as datas trava o avanço', async () => {
    await criar({ perfil: 'ADMIN' });
    http.match((r) => r.url.endsWith('/equipamentos')).forEach((r) => r.flush([
      { id: 'q1', nome: 'Câmera', categoriaNome: 'Câmeras', controle: 'INDIVIDUAL', quantidadeDisponivel: 1, quantidadeTotal: 1, emUso: [] },
    ]));
    await new Promise((r) => setTimeout(r, 0));
    preencher(); // escolhe a câmera q... (eq1 do teste)
    component.selecionados.set([{ id: 'q1', nome: 'Câmera', controle: 'INDIVIDUAL' } as never]);
    const ini = new Date(Date.now() + 5 * 86400000);
    component.form.patchValue({ retiradaPrevista: ini, previstaDevolucao: new Date(ini.getTime() + 86400000) });
    fixture.detectChanges();
    await new Promise((r) => setTimeout(r, 400));
    http.expectOne((r) => r.url.endsWith('/equipamentos/disponibilidade')).flush([{ equipamentoId: 'q1', quantidadeDisponivel: 0 }]);
    await new Promise((r) => setTimeout(r, 0));

    expect(component.indisponiveisSelecionados().map((e) => e.id)).toEqual(['q1']);
    expect(component.podeContinuar()).toBeFalse();
  });

  it('o OK do termo leva a demanda escolhida', async () => {
    await criar({ perfil: 'ADMIN' });
    preencher();
    component.demandaEscolhida.set({ id: 'dm1', titulo: 'Newsletter' } as never);
    component.continuar();

    const promessa = component.confirmarTermoERegistrar();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/emprestimos'));
    expect(req.request.body.demandaId).toBe('dm1');
    req.flush({});
    await promessa;
  });

  it('foto de saída só depois de autorizado; a de devolução, depois que saiu; quem consulta nunca', async () => {
    await criar({ perfil: 'PROFISSIONAL', papelEquipamentos: 'OPERADOR_DE_ESTUDIO' });
    expect(component.podeFotografar({ status: 'AGUARDANDO_AUTORIZACAO' } as never, 'SAIDA')).toBeFalse();
    expect(component.podeFotografar({ status: 'AUTORIZADO' } as never, 'SAIDA')).toBeTrue();
    expect(component.podeFotografar({ status: 'AUTORIZADO' } as never, 'DEVOLUCAO')).toBeFalse();
    expect(component.podeFotografar({ status: 'RETIRADO' } as never, 'DEVOLUCAO')).toBeTrue();
  });

  it('corrigir abre o diálogo preenchido e salva direto, sem passar pelo termo', async () => {
    await criar({ perfil: 'ADMIN' });
    const e = {
      id: 'em1', status: 'AGUARDANDO_AUTORIZACAO', retiranteNome: 'João', retiranteChaveC: 'F1234567', retiranteContato: null,
      finalidade: null, externo: false, localUso: null, retiradaPrevista: new Date(Date.now() - 3600_000).toISOString(),
      previstaDevolucao: new Date(Date.now() + 86400000).toISOString(),
      demandaId: null, demandaTitulo: null, itens: [],
    } as never;
    component.abrirEdicao(e);
    expect(component.form.controls.retiranteNome.value).toBe('João');
    expect(component.etapa()).toBe('dados');

    component.form.patchValue({ externo: true, retiranteChaveC: 'c7654321' });
    const promessa = component.salvarEdicao();
    const req = http.expectOne((r) => r.method === 'PUT' && r.url.endsWith('/emprestimos/em1'));
    expect(req.request.body.externo).toBeTrue();
    expect(req.request.body.retiranteChaveC).toBe('C7654321');
    expect(req.request.body.itens).toBeUndefined();
    req.flush({});
    await promessa;
    expect(component.dialogNovo()).toBeFalse();
  });

  it('só quem opera corrige pedido aguardando e prorroga o que está com a pessoa', async () => {
    await criar({ perfil: 'PROFISSIONAL', papelEquipamentos: 'OPERADOR_DE_ESTUDIO' });
    expect(component.podeEditar({ status: 'AGUARDANDO_AUTORIZACAO' } as never)).toBeTrue();
    expect(component.podeEditar({ status: 'AUTORIZADO' } as never)).toBeFalse();
    expect(component.podeProrrogar({ status: 'RETIRADO' } as never)).toBeTrue();
    expect(component.podeProrrogar({ status: 'DEVOLVIDO' } as never)).toBeFalse();
  });

  it('Profissional sem papel não corrige nem prorroga', async () => {
    await criar({ perfil: 'PROFISSIONAL' });
    expect(component.podeEditar({ status: 'AGUARDANDO_AUTORIZACAO' } as never)).toBeFalse();
    expect(component.podeProrrogar({ status: 'RETIRADO' } as never)).toBeFalse();
  });

  it('foto que nao sobe mostra o motivo na janela, em vez de falhar calado', async () => {
    await criar({ perfil: 'ADMIN' });
    const arquivo = new File([new Uint8Array([1, 2, 3])], 'foto.png', { type: 'image/png' });
    const input = { files: [arquivo], value: 'x' } as unknown as HTMLInputElement;

    const promessa = component.escolherFoto({ id: 'em1' } as never, 'SAIDA', { target: input } as unknown as Event);
    http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/emprestimos/em1/fotos'))
      .flush({ mensagem: 'Não foi possível guardar o arquivo no Storage' }, { status: 503, statusText: 'Service Unavailable' });
    await promessa;

    expect(component.erroFoto()).toBe('Não foi possível guardar o arquivo no Storage');
    expect(component.enviandoFoto()).toBeFalse();
  });

  it('prorrogar manda a nova data e o motivo', async () => {
    await criar({ perfil: 'ADMIN' });
    component.abrirProrrogacao({ id: 'em1', previstaDevolucao: new Date().toISOString() } as never);
    component.novaPrevista.set(new Date('2026-12-01T12:00:00Z'));
    component.motivoProrrogacao.set('  mais um dia  ');

    const promessa = component.prorrogar();
    const req = http.expectOne((r) => r.method === 'PATCH' && r.url.endsWith('/emprestimos/em1/prorrogar'));
    expect(req.request.body).toEqual({ novaPrevista: '2026-12-01T12:00:00.000Z', motivo: 'mais um dia' });
    req.flush({});
    await promessa;
    expect(component.prorrogando()).toBeNull();
  });

  it('o filtro da tela esconde o que não bate e limpar volta tudo', async () => {
    await criar({ perfil: 'ADMIN' });
    http.expectOne((r) => r.url.includes('/emprestimos?grupo=')).flush([
      { id: '1', status: 'RETIRADO', atrasado: false, externo: false, retiranteNome: 'João', itens: [], previstaDevolucao: '2026-10-20T15:00:00Z' },
      { id: '2', status: 'RETIRADO', atrasado: false, externo: true, retiranteNome: 'Maria', itens: [], previstaDevolucao: '2026-10-20T15:00:00Z' },
    ]);
    await new Promise((r) => setTimeout(r, 0)); // as outras chamadas da tela seguem pendentes: não dá para esperar a aplicação estabilizar
    fixture.detectChanges();
    expect(component.filtrados().length).toBe(2);

    component.ajustarFiltro({ busca: 'maria' });
    expect(component.filtrados().map((e) => e.id)).toEqual(['2']);
    expect(component.filtrosAtivos()).toBeTrue();

    component.limparFiltros();
    expect(component.filtrados().length).toBe(2);
  });

  it('Profissional sem papel não fotografa', async () => {
    await criar({ perfil: 'PROFISSIONAL' });
    expect(component.podeFotografar({ status: 'RETIRADO' } as never, 'DEVOLUCAO')).toBeFalse();
  });

  it('Profissional sem papel não tem acesso ao módulo nem aos botões de operar e autorizar', async () => {
    await criar({ perfil: 'PROFISSIONAL' });
    expect(component.auth.acessaEquipamentos()).toBeFalse();
    expect(component.auth.operaEquipamentos()).toBeFalse();
    const e = { status: 'AGUARDANDO_AUTORIZACAO' } as never;
    expect(component.podeAutorizar(e)).toBeFalse();
    expect(component.podeEntregar({ status: 'AUTORIZADO' } as never)).toBeFalse();
  });

  it('o gestor de equipamentos autoriza e recusa; o operador entrega e recebe', async () => {
    await criar({ perfil: 'PROFISSIONAL', papelEquipamentos: 'GESTOR_DE_EQUIPAMENTOS' });
    expect(component.podeAutorizar({ status: 'AGUARDANDO_AUTORIZACAO' } as never)).toBeTrue();
    expect(component.podeReceber({ status: 'RETIRADO' } as never)).toBeTrue();
    expect(component.podeAutorizar({ status: 'RETIRADO' } as never)).toBeFalse();
  });
});
