import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { ConfirmationService } from 'primeng/api';
import { ProjetoService } from '../../projeto/projeto.service';
import { formatarData, paraData } from '../../../../core/util/data';
import type { Campanha } from '../campanha.model';
import { CampanhaService } from '../campanha.service';
import { AuthService } from '../../../auth/auth.service';

@Component({
  selector: 'app-campanha-lista',
  imports: [ReactiveFormsModule, TableModule, Dialog, InputText, DatePicker, Select, Button, Message, DatePipe],
  templateUrl: './campanha-lista.html',
  styleUrl: './campanha-lista.scss',
})
export class CampanhaLista {
  #fb = inject(FormBuilder);
  #service = inject(CampanhaService);
  #confirmationService = inject(ConfirmationService);

  campanhas = this.#service.listar;
  projetos = inject(ProjetoService).listar;

  dialogAberto = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  form = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    projetoId: this.#fb.control<string | null>(null),
    codigo: [''],
    dataInicio: this.#fb.control<Date | null>(null),
    dataFim: this.#fb.control<Date | null>(null),
  });
  auth = inject(AuthService);

  abrirNovo() {
    this.editandoId.set(null);
    this.erro.set(null);
    this.form.reset();
    this.dialogAberto.set(true);
  }

  abrirEdicao(campanha: Campanha) {
    this.editandoId.set(campanha.id);
    this.erro.set(null);
    this.form.setValue({
      nome: campanha.nome,
      projetoId: campanha.projetoId,
      codigo: campanha.codigo ?? '',
      dataInicio: paraData(campanha.dataInicio),
      dataFim: paraData(campanha.dataFim),
    });
    this.dialogAberto.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    const payload = {
      nome: v.nome,
      projetoId: v.projetoId,
      codigo: v.codigo || null,
      dataInicio: formatarData(v.dataInicio),
      dataFim: formatarData(v.dataFim),
    };
    const id = this.editandoId();
    try {
      await firstValueFrom(id ? this.#service.atualizar(id, payload) : this.#service.criar(payload));
      this.dialogAberto.set(false);
      this.campanhas.reload();
    } catch {
      this.erro.set('Não foi possível salvar a campanha.');
    } finally {
      this.salvando.set(false);
    }
  }

  confirmarExclusao(campanha: Campanha) {
    this.#confirmationService.confirm({
      header: 'Remover campanha',
      message: `Remover "${campanha.nome}"? Esta ação não pode ser desfeita.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Remover',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.#excluir(campanha),
    });
  }

  async #excluir(campanha: Campanha) {
    try {
      await firstValueFrom(this.#service.excluir(campanha.id));
      this.campanhas.reload();
    } catch {
      // o toast do interceptor já informou o motivo (409 = em uso)
    }
  }
}