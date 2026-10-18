import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { ProjetoLista } from './projeto-lista';

describe('ProjetoLista', () => {
  let component: ProjetoLista;
  let fixture: ComponentFixture<ProjetoLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjetoLista],
      providers: provideTesteApp(),
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProjetoLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('ProjetoLista (quem vê o botão Novo projeto)', () => {
  async function texto(opcoes: Parameters<typeof provideTesteApp>[0]) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({ imports: [ProjetoLista], providers: provideTesteApp(opcoes) }).compileComponents();
    const fixture = TestBed.createComponent(ProjetoLista);
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('Profissional comum não vê Novo projeto; Referência de equipe, Gestor e Admin veem', async () => {
    expect(await texto({ perfil: 'PROFISSIONAL' })).not.toContain('Novo projeto');
    expect(await texto({ perfil: 'PROFISSIONAL', referenciaAreaId: 'a1' })).toContain('Novo projeto');
    expect(await texto({ perfil: 'GESTOR' })).toContain('Novo projeto');
    expect(await texto({ perfil: 'ADMIN' })).toContain('Novo projeto');
  });
});
