import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTesteApp } from '../../../../testing/providers';
import { CompletarCadastro } from './completar-cadastro';

describe('CompletarCadastro', () => {
  let component: CompletarCadastro;
  let fixture: ComponentFixture<CompletarCadastro>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompletarCadastro],
      providers: provideTesteApp(),
    }).compileComponents();

    fixture = TestBed.createComponent(CompletarCadastro);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('só libera o envio com nome, chave C válida e área', () => {
    expect(component.form.invalid).toBeTrue();
    component.form.patchValue({ nome: 'Ana Caroliny Santos de Sousa', chaveC: 'X1349864', areaId: 'a1' });
    expect(component.form.controls.chaveC.errors?.['chaveCInvalida']).toBeTrue();
    component.form.patchValue({ chaveC: 'C1349864' });
    expect(component.form.valid).toBeTrue();
  });

  it('põe a chave C em maiúscula enquanto digita', () => {
    component.form.controls.chaveC.setValue('c1349864');
    component.mascararChaveC();
    expect(component.form.controls.chaveC.value).toBe('C1349864');
  });
});
