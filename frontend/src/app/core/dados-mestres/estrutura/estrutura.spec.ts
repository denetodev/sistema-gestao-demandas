import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { provideTesteApp } from '../../../../testing/providers';
import type { Area } from '../area/area.service';
import type { Nucleo } from '../nucleo/nucleo.service';
import { Estrutura } from './estrutura';

const DIRETORIAS = [
  { id: 'd1', nome: 'CRM', sigla: 'CRM', ativo: true },
  { id: 'd2', nome: 'UGR', sigla: 'UGR', ativo: true },
  { id: 'd3', nome: 'URI', sigla: null, ativo: true },
];
const NUCLEOS = [
  { id: 'n1', nome: 'House-CRM', diretoriaId: 'd1', diretoriaNome: 'CRM', ativo: true },
  { id: 'n2', nome: 'Multimídia', diretoriaId: 'd2', diretoriaNome: 'UGR', ativo: true },
  { id: 'n3', nome: 'Antigo', diretoriaId: 'd2', diretoriaNome: 'UGR', ativo: false },
] as Nucleo[];
const AREAS = [
  { id: 'a1', nome: 'HTML', diretoriaId: 'd1', diretoriaNome: 'CRM', nucleoId: 'n1', nucleoNome: 'House-CRM', ativo: true },
  { id: 'a2', nome: 'Design', diretoriaId: 'd1', diretoriaNome: 'CRM', nucleoId: 'n1', nucleoNome: 'House-CRM', ativo: true },
  { id: 'a3', nome: 'Motion', diretoriaId: 'd2', diretoriaNome: 'UGR', nucleoId: 'n2', nucleoNome: 'Multimídia', ativo: true },
  { id: 'a4', nome: 'Solta', diretoriaId: 'd2', diretoriaNome: 'UGR', nucleoId: null, nucleoNome: null, ativo: true },
  { id: 'a5', nome: 'Velha', diretoriaId: 'd1', diretoriaNome: 'CRM', nucleoId: 'n1', nucleoNome: 'House-CRM', ativo: false },
] as Area[];

describe('Estrutura (diretoria, núcleo e equipe)', () => {
  let fixture: ComponentFixture<Estrutura>;
  let c: Estrutura;
  let http: HttpTestingController;

  /** Responde o que a tela pediu e deixa passar recargas que ela fizer depois de salvar. */
  function esvaziarRecargas() {
    http.match(() => true).forEach((r) => {
      if (!r.cancelled) r.flush([]);
    });
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Estrutura], providers: provideTesteApp({ perfil: 'ADMIN' }) }).compileComponents();
    fixture = TestBed.createComponent(Estrutura);
    c = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/diretorias')).flush(DIRETORIAS);
    http.expectOne((r) => r.url.endsWith('/nucleos')).flush(NUCLEOS);
    http.expectOne((r) => r.url.endsWith('/areas')).flush(AREAS);
    TestBed.tick();
  });

  it('monta a árvore só com o que está ativo e mostra primeiro as diretorias com núcleos', () => {
    const arvore = c.arvore();
    expect(arvore.map((n) => n.diretoria.nome)).toEqual(['CRM', 'UGR', 'URI']);
    expect(arvore[0].nucleos.map((n) => n.nucleo.nome)).toEqual(['House-CRM']);
    expect(arvore[0].nucleos[0].equipes.map((e) => e.nome)).toEqual(['Design', 'HTML']);
    expect(arvore[1].nucleos.map((n) => n.nucleo.nome)).toEqual(['Multimídia']); // o núcleo inativo some
    expect(arvore[1].totalEquipes).toBe(2); // Motion + a equipe solta da diretoria
  });

  it('o filtro padrão esconde diretoria vazia e "todas" mostra tudo; a busca acha equipe dentro do núcleo', () => {
    expect(c.visiveis().map((n) => n.diretoria.nome)).toEqual(['CRM', 'UGR']);
    c.filtro.set('todas');
    expect(c.visiveis().length).toBe(3);

    c.filtro.set('com');
    c.busca.set('motion');
    expect(c.visiveis().map((n) => n.diretoria.nome)).toEqual(['UGR']);
    expect(c.estaAberta('d2')).toBeTrue(); // com busca, a diretoria que bateu abre sozinha
  });

  it('lista as equipes sem núcleo para a pessoa escolher o núcleo de cada uma', () => {
    expect(c.equipesSemNucleo().map((e) => e.nome)).toEqual(['Solta']);
  });

  it('novo núcleo pelo botão da diretoria já vem com a diretoria escolhida e exige nome', () => {
    c.novoNucleo('d2');
    expect(c.janela()).toBe('nucleo');
    expect(c.formNucleo.controls.diretoriaId.value).toBe('d2');
    expect(c.formNucleo.invalid).toBeTrue();
    c.formNucleo.patchValue({ nome: 'Redes' });
    expect(c.formNucleo.valid).toBeTrue();
  });

  it('criar núcleo manda o nome aparado e abre a diretoria dele', async () => {
    c.novoNucleo('d3');
    c.formNucleo.patchValue({ nome: '  Redes  ' });

    const promessa = c.salvar();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/nucleos'));
    expect(req.request.body).toEqual({ nome: 'Redes', diretoriaId: 'd3' });
    req.flush({});
    await promessa;
    esvaziarRecargas();

    expect(c.janela()).toBeNull();
    expect(c.estaAberta('d3')).toBeTrue();
  });

  it('criar equipe usa a diretoria do núcleo escolhido', async () => {
    c.novaEquipe('n2');
    c.formEquipe.patchValue({ nome: 'Videomaker' });

    const promessa = c.salvar();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/areas'));
    expect(req.request.body).toEqual({ nome: 'Videomaker', diretoriaId: 'd2', nucleoId: 'n2' });
    req.flush({});
    await promessa;
    esvaziarRecargas();

    expect(c.janela()).toBeNull();
  });

  it('ao editar equipe, só oferece núcleos da mesma diretoria dela', () => {
    c.editarEquipe(AREAS[2]); // Motion, UGR
    const grupos = c.opcoesNucleo();
    expect(grupos.map((g) => g.rotulo)).toEqual(['UGR']);
    expect(grupos[0].itens.map((i) => i.rotulo)).toEqual(['Multimídia']);

    c.novaEquipe();
    expect(c.opcoesNucleo().map((g) => g.rotulo)).toEqual(['CRM', 'UGR']);
  });

  it('mostra o motivo que o servidor recusou e mantém a janela aberta', async () => {
    c.novoNucleo('d1');
    c.formNucleo.patchValue({ nome: 'House-CRM' });

    const promessa = c.salvar();
    http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/nucleos'))
      .flush({ mensagem: 'Já existe um núcleo com esse nome nessa diretoria' }, { status: 409, statusText: 'Conflict' });
    await promessa;

    expect(c.erro()).toBe('Já existe um núcleo com esse nome nessa diretoria');
    expect(c.janela()).toBe('nucleo');
    expect(c.salvando()).toBeFalse();
  });

  it('nova diretoria manda sigla em maiúsculas ou nula', async () => {
    c.novaDiretoria();
    c.formDiretoria.patchValue({ nome: 'Diretoria de Teste', sigla: ' dit ' });

    const promessa = c.salvar();
    const req = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/diretorias'));
    expect(req.request.body).toEqual({ nome: 'Diretoria de Teste', sigla: 'DIT' });
    req.flush({});
    await promessa;
    esvaziarRecargas();
  });
});
