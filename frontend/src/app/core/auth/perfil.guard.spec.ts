import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { gestaoOuReferenciaGuard, naoVisualizadorGuard, perfilGuard } from './perfil.guard';
import { provideTesteApp } from '../../../testing/providers';
import type { Perfil } from './auth.model';

function executar(guard: ReturnType<typeof perfilGuard>, url: string) {
  const estado = { url } as RouterStateSnapshot;
  return TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, estado)) as Promise<boolean | UrlTree>;
}

function configurar(perfil: Perfil) {
  TestBed.configureTestingModule({ providers: provideTesteApp({ perfil }) });
}

describe('guards de perfil', () => {
  it('perfilGuard libera o perfil permitido', async () => {
    configurar('GESTOR');
    expect(await executar(perfilGuard('ADMIN', 'GESTOR'), '/pessoas')).toBe(true);
  });

  it('perfilGuard manda para /acesso-negado guardando o endereço barrado', async () => {
    configurar('PROFISSIONAL');
    const resultado = await executar(perfilGuard('ADMIN', 'GESTOR'), '/pessoas');

    expect(resultado instanceof UrlTree).toBe(true);
    const router = TestBed.inject(Router);
    expect(router.serializeUrl(resultado as UrlTree)).toBe('/acesso-negado?de=%2Fpessoas');
  });

  it('naoVisualizadorGuard barra o Visualizador e libera os demais', async () => {
    configurar('VISUALIZADOR');
    const barrado = await executar(naoVisualizadorGuard, '/relatorio');
    expect(barrado instanceof UrlTree).toBe(true);

    TestBed.resetTestingModule();
    configurar('PROFISSIONAL');
    expect(await executar(naoVisualizadorGuard, '/relatorio')).toBe(true);
  });

  it('gestaoOuReferenciaGuard libera Admin, Gestor e Referência de equipe, e barra Profissional comum e Visualizador', async () => {
    const guard = gestaoOuReferenciaGuard as ReturnType<typeof perfilGuard>;
    for (const [perfil, referencia, libera] of [
      ['ADMIN', null, true],
      ['GESTOR', null, true],
      ['PROFISSIONAL', 'a1', true],
      ['PROFISSIONAL', null, false],
      ['VISUALIZADOR', null, false],
    ] as const) {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ providers: provideTesteApp({ perfil, referenciaAreaId: referencia }) });
      const resultado = await executar(guard, '/demandantes');
      expect(resultado === true).withContext(perfil + ' ' + referencia).toBe(libera);
    }
  });
});
