import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideTesteApp } from '../../../../testing/providers';

import { AppShell } from './app-shell';

describe('AppShell', () => {
  let component: AppShell;
  let fixture: ComponentFixture<AppShell>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AppShell],
      providers: provideTesteApp({ perfil: 'PROFISSIONAL' }),
    })
    .compileComponents();

    fixture = TestBed.createComponent(AppShell);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('mostra no rodape do menu a diretoria, o nucleo, a equipe e o cargo da pessoa', () => {
    expect(component.hierarquia()).toEqual({ diretoria: 'COE/CRM', nucleo: 'House-CRM', equipe: 'Design', cargo: '—' });
    const itens = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.app-trilho strong'))
      .map((e) => e.textContent?.trim());
    expect(itens).toEqual(['COE/CRM', 'House-CRM', 'Design', '—']);
  });

  it('mostra o perfil por extenso, nao em maiusculas de sistema', () => {
    expect(component.perfilRotulo()).toBe('Profissional');
  });

  it('no celular o botao do topo abre e fecha a gaveta, sem mexer no modo recolhido', () => {
    spyOnProperty(window, 'innerWidth').and.returnValue(500);

    component.alternarMenu();
    expect(component.menuMovelAberto()).toBeTrue();
    expect(component.sidebarRecolhida()).toBeFalse();

    component.fecharMenuMovel();
    expect(component.menuMovelAberto()).toBeFalse();
  });

  it('no computador o botao do topo recolhe o menu e lembra a escolha', () => {
    spyOnProperty(window, 'innerWidth').and.returnValue(1280);
    expect(component.sidebarRecolhida()).toBeFalse();
    component.alternarMenu();
    expect(component.sidebarRecolhida()).toBeTrue();
    expect(localStorage.getItem('sgd-sidebar-recolhida')).toBe('true');
    component.alternarMenu();
    expect(component.sidebarRecolhida()).toBeFalse();
  });

  it('o botao de recolher fica dentro do menu e a marca expande quando recolhido', () => {
    spyOnProperty(window, 'innerWidth').and.returnValue(1280);
    const el = fixture.nativeElement as HTMLElement;
    el.querySelector<HTMLButtonElement>('.app-marca__recolher')!.click();
    fixture.detectChanges();
    expect(component.sidebarRecolhida()).toBeTrue();

    el.querySelector<HTMLButtonElement>('.app-marca__logo')!.click();
    fixture.detectChanges();
    expect(component.sidebarRecolhida()).toBeFalse();
  });

  it('o menu da conta reune perfil, tema e sair', () => {
    expect(component.itensUsuario().map((i) => i.label).filter(Boolean)).toEqual(['Meu perfil', 'Tema escuro', 'Sair']);
  });

  it('o nome no topo vem seguido da matrícula', () => {
    expect(component.nomeComMatricula()).toBe('Pessoa de Teste - C1349864');
    expect((fixture.nativeElement as HTMLElement).querySelector('.app-usuario__nome')?.textContent).toContain('C1349864');

  });

  it('a busca do topo leva para as demandas com o texto como filtro', () => {
    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    component.busca.set('  captação agro ');

    component.buscar(new Event('submit'));

    expect(navegar).toHaveBeenCalledWith(['/demandas'], { queryParams: { busca: 'captação agro' } });
  });

  it('busca vazia abre as demandas sem filtro', () => {
    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    component.busca.set('   ');

    component.buscar(new Event('submit'));

    expect(navegar).toHaveBeenCalledWith(['/demandas'], { queryParams: {} });
  });
});

describe('AppShell (menu por perfil)', () => {
  async function menuDe(perfil: 'PROFISSIONAL' | 'GESTOR' | 'ADMIN' | 'VISUALIZADOR') {
    localStorage.clear();
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({ imports: [AppShell], providers: provideTesteApp({ perfil }) }).compileComponents();
    const fixture = TestBed.createComponent(AppShell);
    fixture.detectChanges();
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.app-nav__item'))
      .map((a) => a.getAttribute('title'));
  }

  it('Profissional não vê Demandantes, Pessoas, Núcleos e equipes nem Cargos, mas vê Projetos e Campanhas', async () => {
    const itens = await menuDe('PROFISSIONAL');
    for (const fora of ['Demandantes', 'Pessoas', 'Núcleos e equipes', 'Cargos']) {
      expect(itens).not.toContain(fora);
    }
    expect(itens).toContain('Projetos');
    expect(itens).toContain('Campanhas');
  });

  it('Visualizador também não vê as telas de gestão', async () => {
    const itens = await menuDe('VISUALIZADOR');
    expect(itens).not.toContain('Demandantes');
    expect(itens).not.toContain('Pessoas');
  });

  it('Gestor e Admin veem Demandantes e Pessoas', async () => {
    for (const perfil of ['GESTOR', 'ADMIN'] as const) {
      const itens = await menuDe(perfil);
      expect(itens).toContain('Demandantes');
      expect(itens).toContain('Pessoas');
    }
  });
});
