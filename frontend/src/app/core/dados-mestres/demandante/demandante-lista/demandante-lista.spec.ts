import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../../testing/providers';

import { DemandanteLista } from './demandante-lista';

describe('DemandanteLista', () => {
  let component: DemandanteLista;
  let fixture: ComponentFixture<DemandanteLista>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemandanteLista],
      providers: provideTesteApp(),
    }).compileComponents();

    fixture = TestBed.createComponent(DemandanteLista);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('DemandanteLista (quem vê o botão Novo demandante)', () => {
  async function texto(opcoes: Parameters<typeof provideTesteApp>[0]) {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({ imports: [DemandanteLista], providers: provideTesteApp(opcoes) }).compileComponents();
    const fixture = TestBed.createComponent(DemandanteLista);
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('Referência de equipe, Gestor e Admin veem; Profissional comum não', async () => {
    expect(await texto({ perfil: 'PROFISSIONAL' })).not.toContain('Novo demandante');
    expect(await texto({ perfil: 'PROFISSIONAL', referenciaAreaId: 'a1' })).toContain('Novo demandante');
    expect(await texto({ perfil: 'GESTOR' })).toContain('Novo demandante');
    expect(await texto({ perfil: 'ADMIN' })).toContain('Novo demandante');
  });
});
