import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../testing/providers';
import { Estrutura } from './estrutura';

describe('Estrutura', () => {
  let fixture: ComponentFixture<Estrutura>;
  let componente: Estrutura;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Estrutura], providers: provideTesteApp() }).compileComponents();
    fixture = TestBed.createComponent(Estrutura);
    componente = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('abre sem nenhuma equipe nem núcleo carregados', () => {
    expect(componente).toBeTruthy();
    expect(componente.equipesSemNucleo()).toBe(0);
    expect(componente.qtdEquipes('qualquer')).toBe(0);
  });

  it('novo núcleo abre o formulário vazio e exige nome e diretoria', () => {
    componente.novoNucleo();
    expect(componente.nucleoAberto()).toBeTrue();
    expect(componente.nucleoEditando()).toBeNull();
    expect(componente.formNucleo.invalid).toBeTrue();
  });
});
