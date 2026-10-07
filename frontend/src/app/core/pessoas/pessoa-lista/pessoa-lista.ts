import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { SelectButton } from 'primeng/selectbutton';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';
import { PessoaService } from '../../../core/pessoas/pessoa.service';
import { AreaService } from '../../../core/dados-mestres/area/area.service';
import { CargoService } from '../../../core/dados-mestres/cargo/cargo.service';
import { AuthService } from '../../../core/auth/auth.service';
import { BarraPagina } from '../../../core/layout/barra-pagina/barra-pagina';
import { cpfValidator, formatarCpf } from '../../../core/util/cpf';
import type { Pessoa, StatusPessoa } from '../../../core/pessoas/pessoa.model';
import type { Perfil } from '../../../core/auth/auth.model';

@Component({
  selector: 'app-pessoa-lista',
  imports: [
    ReactiveFormsModule, TableModule, Dialog, SelectButton, InputText, Textarea,
    Select, Button, Message, Tag, Tooltip, BarraPagina, DatePipe, FormsModule
  ],
  templateUrl: './pessoa-lista.html',
  styleUrl: './pessoa-lista.scss',
})
export class PessoaLista {
  #fb = inject(FormBuilder);
  #service = inject(PessoaService);
  #confirmationService = inject(ConfirmationService);
  #messageService = inject(MessageService);
  #cargoService = inject(CargoService);

  auth = inject(AuthService);
  pessoas = this.#service.listar;
  areas = inject(AreaService).listar;
  cargos = computed(() => (this.#cargoService.listar.value() ?? []).filter(c => c.ativo));
  dialogAprovacao = signal(false);
  dialogEdicao = signal(false);

  apenasPendentes = this.#service.apenasPendentes;

  abas = [
    { label: 'Todas', value: false },
    { label: 'Aguardando aprovação', value: true },
  ];

  opcoesPerfil = [
    { label: 'Visualizador', value: 'VISUALIZADOR' },
    { label: 'Profissional', value: 'PROFISSIONAL' },
    { label: 'Gestor', value: 'GESTOR' },
    { label: 'Admin', value: 'ADMIN' },
  ];

  /** Gestor não concede ADMIN nem mexe em quem já é ADMIN (regra do backend). */
  perfisDisponiveis = computed(() =>
    this.auth.temPerfil('ADMIN')
      ? this.opcoesPerfil
      : this.opcoesPerfil.filter(o => o.value !== 'ADMIN'),
  );

  podeGerenciar(pessoa: Pessoa): boolean {
    if (this.auth.temPerfil('ADMIN')) return true;
    return this.auth.temPerfil('GESTOR') && pessoa.perfil !== 'ADMIN';
  }

  pessoaSelecionada = signal<Pessoa | null>(null);
  dialogAberto = signal(false);
  salvando = signal(false);
  erro = signal<string | null>(null);

  pendente = computed(() => this.pessoaSelecionada()?.aprovadoEm === null);

  form = this.#fb.nonNullable.group({
    perfil: this.#fb.control<Perfil | null>(null, Validators.required),
    areaId: this.#fb.control<string | null>(null),
    cargoId: this.#fb.control<string | null>(null),
    referenciaAreaId: this.#fb.control<string | null>(null),
  });

  abrir(pessoa: Pessoa) {
    this.pessoaSelecionada.set(pessoa);
    this.erro.set(null);
    if (pessoa.aprovadoEm === null) {
      this.form.setValue({
        perfil: 'PROFISSIONAL',
        areaId: pessoa.areaId,
        cargoId: pessoa.cargoId,
        referenciaAreaId: pessoa.referenciaAreaId,
      });
      this.dialogAprovacao.set(true);
    } else {
      this.formEdicao.setValue({
        nome: pessoa.nome,
        email: pessoa.email ?? '',
        cpf: '',
        areaId: pessoa.areaId,
        cargoId: pessoa.cargoId,
        status: pessoa.status,
        perfil: pessoa.perfil,
        referenciaAreaId: pessoa.referenciaAreaId,
      });
      this.dialogEdicao.set(true);
    }
  }

  async salvarEdicao() {
    const pessoa = this.pessoaSelecionada();
    if (!pessoa || this.formEdicao.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.formEdicao.getRawValue();
    try {
      await firstValueFrom(this.#service.atualizar(pessoa.id, {
        nome: v.nome,
        email: v.email || null,
        cpf: v.cpf || null,
        areaId: v.areaId!,
        cargoId: v.cargoId,
        status: v.status,
        perfil: v.perfil,
        referenciaAreaId: v.referenciaAreaId,
      }));
      this.dialogEdicao.set(false);
      this.pessoas.reload();
      this.#messageService.add({ severity: 'success', summary: 'Pessoa atualizada', life: 4000 });
    } catch {
      this.erro.set('Não foi possível salvar as alterações.');
    } finally {
      this.salvando.set(false);
    }
  }

  /** Marca o que o gestor alterou em relação ao que a pessoa informou. */
  ajustado(campo: 'areaId' | 'cargoId'): boolean {
    const original = this.pessoaSelecionada()?.[campo] ?? null;
    return this.form.controls[campo].value !== original;
  }

  async aprovar() {
    const pessoa = this.pessoaSelecionada();
    if (!pessoa || this.form.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    try {
      await firstValueFrom(this.#service.aprovar(pessoa.id, {
        perfil: v.perfil!,
        areaId: v.areaId,
        cargoId: v.cargoId,
        referenciaAreaId: v.referenciaAreaId,
      }));
      this.dialogAprovacao.set(false);
      this.pessoas.reload();
      this.#messageService.add({ severity: 'success', summary: 'Cadastro aprovado', life: 4000 });
    } catch {
      this.erro.set('Não foi possível aprovar o cadastro.');
    } finally {
      this.salvando.set(false);
    }
  }

  confirmarRejeicao() {
    const pessoa = this.pessoaSelecionada();
    if (!pessoa) return;
    this.#confirmationService.confirm({
      header: 'Rejeitar cadastro',
      message: `Rejeitar o cadastro de ${pessoa.nome}? Essa ação não pode ser desfeita: a pessoa não poderá ser aprovada depois.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Rejeitar',
      rejectLabel: 'Voltar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.#rejeitar(pessoa),
    });
  }

  async #rejeitar(pessoa: Pessoa) {
    this.salvando.set(true);
    try {
      await firstValueFrom(this.#service.rejeitar(pessoa.id, { motivo: null }));
      this.dialogAprovacao.set(false);
      this.pessoas.reload();
    } catch {
      this.erro.set('Não foi possível rejeitar o cadastro.');
    } finally {
      this.salvando.set(false);
    }
  }

  severityStatus(status: StatusPessoa) {
    switch (status) {
      case 'ATIVO': return 'success';
      case 'AFASTADO': return 'warn';
      case 'REJEITADO': return 'danger';
      default: return 'secondary';
    }
  }

  formEdicao = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    email: [''],
    cpf: ['', cpfValidator],
    areaId: this.#fb.control<string | null>(null, Validators.required),
    cargoId: this.#fb.control<string | null>(null),
    status: this.#fb.control<StatusPessoa | null>(null),
    perfil: this.#fb.control<Perfil | null>(null),
    referenciaAreaId: this.#fb.control<string | null>(null),
  });

  mascararCpf() {
    const c = this.formEdicao.controls.cpf;
    const formatado = formatarCpf(c.value);
    if (formatado !== c.value) c.setValue(formatado);
  }

  opcoesStatus = [
    { label: 'Ativo', value: 'ATIVO' },
    { label: 'Inativo', value: 'INATIVO' },
    { label: 'Afastado', value: 'AFASTADO' },
  ];
}