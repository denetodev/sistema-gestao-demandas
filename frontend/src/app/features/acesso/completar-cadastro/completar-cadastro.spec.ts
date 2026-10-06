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

  it('só libera o envio com nome, CPF válido e área', () => {
    expect(component.form.invalid).toBeTrue();
    component.form.patchValue({ nome: 'Ana Caroliny Santos de Sousa', cpf: '529.982.247-26', areaId: 'a1' });
    expect(component.form.controls.cpf.errors?.['cpfInvalido']).toBeTrue();
    component.form.patchValue({ cpf: '529.982.247-25' });
    expect(component.form.valid).toBeTrue();
  });

  it('formata o CPF enquanto digita', () => {
    component.form.controls.cpf.setValue('52998224725');
    component.mascararCpf();
    expect(component.form.controls.cpf.value).toBe('529.982.247-25');
  });
});
