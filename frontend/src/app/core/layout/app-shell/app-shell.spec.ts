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

  it('mostra no rodape do menu o nucleo, a equipe e o cargo da pessoa', () => {
    expect(component.hierarquia()).toEqual({ nucleo: 'COE/CRM', equipe: 'Design', cargo: '—' });
    const itens = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.app-trilho strong'))
      .map((e) => e.textContent?.trim());
    expect(itens).toEqual(['COE/CRM', 'Design', '—']);
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
