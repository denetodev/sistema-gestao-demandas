import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { provideTesteApp } from '../../../testing/providers';
import { SugestoesPlanner } from './sugestoes-planner';
import type { SugestaoPlanner } from '../../core/planner/planner.service';

function sugestao(parcial: Partial<SugestaoPlanner> = {}): SugestaoPlanner {
  return {
    id: 's1', codigo: 'BC2610FN', titulo: 'BC2610FN_email_SS06_BBFriday', bucket: 'HTML para Aprovação', etiqueta: 'HTML - Carol',
    criadaNoPlanner: '2026-10-28T15:49:00Z', demandaExistenteId: null, demandaExistenteTitulo: null, ...parcial,
  };
}

describe('SugestoesPlanner', () => {
  let fixture: ComponentFixture<SugestoesPlanner>;
  let component: SugestoesPlanner;
  let http: HttpTestingController;

  async function criar(sugestoes: SugestaoPlanner[], perfil: 'PROFISSIONAL' | 'GESTOR' = 'PROFISSIONAL') {
    await TestBed.configureTestingModule({ imports: [SugestoesPlanner], providers: provideTesteApp({ perfil }) }).compileComponents();
    fixture = TestBed.createComponent(SugestoesPlanner);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne((r) => r.url.endsWith('/planner/sugestoes')).flush(sugestoes);
    await new Promise((r) => setTimeout(r, 0));
    fixture.detectChanges();
  }

  it('lista uma sugestão por tarefa, com o código separado do título', async () => {
    await criar([sugestao(), sugestao({ id: 's2', titulo: 'BC2610FO - Email', codigo: 'BC2610FO' })]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.sugestao').length).toBe(2);
    expect(el.querySelector('.codigo-chip')?.textContent).toContain('BC2610FN');
  });

  it('avisa quando aceitar só coloca a pessoa numa demanda que já existe', async () => {
    await criar([sugestao({ demandaExistenteId: 'd9', demandaExistenteTitulo: 'BC2610FN' })]);
    expect((fixture.nativeElement as HTMLElement).querySelector('.sugestao__efeito')?.textContent).toContain('já está no sistema');
  });

  it('sem sugestão mostra que não há nada esperando', async () => {
    await criar([]);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Nada esperando você');
  });

  it('aceitar chama a API e abre a demanda', async () => {
    await criar([sugestao()]);
    const router = TestBed.inject(Router);
    const ir = spyOn(router, 'navigate').and.resolveTo(true);

    const promessa = component.aceitar(sugestao());
    http.expectOne((r) => r.url.endsWith('/planner/sugestoes/s1/aceitar')).flush({ demandaId: 'd1' });
    await promessa;

    expect(ir).toHaveBeenCalledWith(['/demandas', 'd1']);
  });

  it('profissional comum não vê o botão Atualizar pelo Planner', async () => {
    await criar([], 'PROFISSIONAL');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Atualizar pelo Planner');
  });

  it('a Referência de equipe vê o botão, como a gestão', async () => {
    await TestBed.configureTestingModule({
      imports: [SugestoesPlanner],
      providers: provideTesteApp({ perfil: 'PROFISSIONAL', referenciaAreaId: 'a1' }),
    }).compileComponents();
    fixture = TestBed.createComponent(SugestoesPlanner);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne((r) => r.url.endsWith('/planner/sugestoes')).flush([]);
    await new Promise((r) => setTimeout(r, 0));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Atualizar pelo Planner');
  });

  it('a gestão envia o arquivo e vê as etiquetas sem pessoa e os nomes ambíguos', async () => {
    await criar([], 'GESTOR');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Atualizar pelo Planner');

    const arquivo = new File([new Uint8Array([1, 2, 3])], 'Construção HOUSE.xlsx');
    const campo = el.querySelector('input[type=file]') as HTMLInputElement;
    const promessa = component.atualizarPeloPlanner({ target: campo } as unknown as Event);
    // sem o arquivo escolhido no campo, nada é enviado
    await promessa;
    http.expectNone((r) => r.url.endsWith('/planner/importar'));

    const transferencia = new DataTransfer();
    transferencia.items.add(arquivo);
    campo.files = transferencia.files;
    const envio = component.atualizarPeloPlanner({ target: campo } as unknown as Event);
    const req = http.expectOne((r) => r.url.endsWith('/planner/importar'));
    expect(req.request.body instanceof FormData).toBeTrue();
    expect((req.request.body as FormData).get('arquivo')).toBeTruthy();
    req.flush({
      tarefasLidas: 911, tarefasNaJanela: 43, sugestoesNovas: 6,
      etiquetasSemCadastro: ['CRIATIVO - Fulano'], nomesAmbiguos: ['HTML - Lucas'],
    });
    await envio;
    fixture.detectChanges();
    await new Promise((r) => setTimeout(r, 0));
    http.match((r) => r.url.endsWith('/planner/sugestoes')).forEach((rq) => rq.flush([]));
    fixture.detectChanges();

    expect(el.textContent).toContain('CRIATIVO - Fulano');
    expect(el.textContent).toContain('HTML - Lucas');
    expect(el.textContent).toContain('6 sugestões novas');
  });
});
