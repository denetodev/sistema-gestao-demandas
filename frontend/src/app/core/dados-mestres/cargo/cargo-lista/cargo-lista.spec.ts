import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { ConfirmationService } from 'primeng/api';
import { provideTesteApp } from '../../../../../testing/providers';
import type { Cargo } from '../cargo.service';
import { CargoLista } from './cargo-lista';

const CARGOS = [
  { id: 'c1', nome: 'Operador', descricao: 'Opera o estúdio', ativo: true },
  { id: 'c2', nome: 'Designer', descricao: null, ativo: true },
  { id: 'c3', nome: 'Antigo', descricao: null, ativo: false },
] as Cargo[];

const PESSOAS = [
  { id: 'p1', cargoId: 'c1', status: 'ATIVO' },
  { id: 'p2', cargoId: 'c1', status: 'ATIVO' },
  { id: 'p3', cargoId: 'c1', status: 'INATIVO' },
  { id: 'p4', cargoId: null, status: 'ATIVO' },
];

describe('CargoLista', () => {
  let fixture: ComponentFixture<CargoLista>;
  let c: CargoLista;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CargoLista], providers: provideTesteApp({ perfil: 'ADMIN' }) }).compileComponents();
    fixture = TestBed.createComponent(CargoLista);
    c = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/cargos')).flush(CARGOS);
    http.match((r) => r.url.endsWith('/pessoas')).forEach((r) => r.flush(PESSOAS));
    TestBed.tick();
  });

  it('lista só os cargos ativos, em ordem alfabética, com quantas pessoas ativas ocupam cada um', () => {
    expect(c.linhas().map((l) => [l.cargo.nome, l.pessoas])).toEqual([['Designer', 0], ['Operador', 2]]);
  });

  it('a busca olha o nome e a descrição, sem diferenciar acento', () => {
    c.busca.set('estudio');
    expect(c.filtradas().map((l) => l.cargo.nome)).toEqual(['Operador']);
  });

  it('exige nome, e nome só com espaços não vale', () => {
    c.novo();
    expect(c.form.invalid).toBeTrue();
    c.form.patchValue({ nome: '   ' });
    expect(c.form.invalid).toBeTrue();
    c.form.patchValue({ nome: 'Analista' });
    expect(c.form.valid).toBeTrue();
  });

  it('criar manda nome aparado e descrição vazia como nula', async () => {
    c.novo();
    c.form.patchValue({ nome: '  Analista de mídia ', descricao: '  ' });

    const promessa = c.salvar();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/cargos'));
    expect(req.request.body).toEqual({ nome: 'Analista de mídia', descricao: null });
    req.flush({});
    await promessa;
    http.match(() => true).forEach((r) => r.flush([]));

    expect(c.aberta()).toBeFalse();
  });

  it('editar abre preenchido e salva com PUT', async () => {
    c.editar(CARGOS[0]);
    expect(c.form.getRawValue()).toEqual({ nome: 'Operador', descricao: 'Opera o estúdio' });

    c.form.patchValue({ descricao: 'Opera o estúdio e as câmeras' });
    const promessa = c.salvar();
    const req = http.expectOne((r) => r.method === 'PUT' && r.url.endsWith('/cargos/c1'));
    expect(req.request.body.descricao).toBe('Opera o estúdio e as câmeras');
    req.flush({});
    await promessa;
    http.match(() => true).forEach((r) => r.flush([]));
  });

  it('mostra o motivo que o servidor recusou e mantém a janela aberta', async () => {
    c.novo();
    c.form.patchValue({ nome: 'Designer' });

    const promessa = c.salvar();
    http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/cargos'))
      .flush({ mensagem: 'Já existe um cargo com esse nome' }, { status: 409, statusText: 'Conflict' });
    await promessa;

    expect(c.erro()).toBe('Já existe um cargo com esse nome');
    expect(c.aberta()).toBeTrue();
  });

  it('cargo com pessoas ativas só avisa; sem pessoas pede confirmação antes de desativar', () => {
    const confirmar = spyOn(TestBed.inject(ConfirmationService), 'confirm');

    c.desativar(c.linhas().find((l) => l.cargo.nome === 'Operador')!);
    expect(confirmar.calls.mostRecent().args[0].message).toContain('2 pessoa(s) ativa(s)');
    expect(confirmar.calls.mostRecent().args[0].accept).toBeUndefined();

    c.desativar(c.linhas().find((l) => l.cargo.nome === 'Designer')!);
    expect(confirmar.calls.mostRecent().args[0].accept).toBeDefined();
  });
});
