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

  it('quem só consulta não vê os botões de operar nem de autorizar', async () => {
    await criar({ perfil: 'PROFISSIONAL', acessaEquipamentos: true });
    expect(component.auth.operaEquipamentos()).toBeFalse();
    const e = { status: 'AGUARDANDO_AUTORIZACAO' } as never;
    expect(component.podeAutorizar(e)).toBeFalse();
    expect(component.podeEntregar({ status: 'AUTORIZADO' } as never)).toBeFalse();
    expect(component.titulo()).toBe('Quem está com quê');
  });

  it('o gestor de equipamentos autoriza e recusa; o operador entrega e recebe', async () => {
    await criar({ perfil: 'PROFISSIONAL', papelEquipamentos: 'GESTOR_DE_EQUIPAMENTOS' });
    expect(component.podeAutorizar({ status: 'AGUARDANDO_AUTORIZACAO' } as never)).toBeTrue();
    expect(component.podeReceber({ status: 'RETIRADO' } as never)).toBeTrue();
    expect(component.podeAutorizar({ status: 'RETIRADO' } as never)).toBeFalse();
  });
});
