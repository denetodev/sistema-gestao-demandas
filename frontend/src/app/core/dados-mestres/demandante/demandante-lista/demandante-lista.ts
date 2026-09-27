import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { DemandanteService } from '../demandante.service';
import { DiretoriaService } from '../../diretoria/diretoria.service';
import { AuthService } from '../../../auth/auth.service';
import { BarraPagina } from '../../../layout/barra-pagina/barra-pagina';
import type { Demandante, TipoDemandante } from '../demandante.model';

@Component({
  selector: 'app-demandante-lista',
  imports: [
    ReactiveFormsModule,
    TableModule,
    Dialog,
    InputText,
    Textarea,
    Select,
    Button,
    Message,
    Tag,
    BarraPagina,
  ],
  templateUrl: './demandante-lista.html',
  styleUrl: './demandante-lista.scss',
})
export class DemandanteLista {
  #fb = inject(FormBuilder);
  #service = inject(DemandanteService);

  auth = inject(AuthService);
  demandantes = this.#service.listar;
  diretorias = inject(DiretoriaService).listar;

  opcoesTipo = [
    { label: 'Pessoa', value: 'PESSOA' },
    { label: 'Área', value: 'AREA' },
    { label: 'Unidade', value: 'UNIDADE' },
    { label: 'Externo', value: 'EXTERNO' },
  ];

  dialogAberto = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  form = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    tipo: this.#fb.control<TipoDemandante | null>(null, Validators.required),
    observacao: [''],
    diretoriaId: this.#fb.control<string | null>(null),
  });

  abrirNovo() {
    this.editandoId.set(null);
    this.erro.set(null);
    this.form.reset();
    this.dialogAberto.set(true);
  }

  abrirEdicao(demandante: Demandante) {
    this.editandoId.set(demandante.id);
    this.erro.set(null);
    this.form.setValue({
      nome: demandante.nome,
      tipo: demandante.tipo,
      observacao: demandante.observacao ?? '',
      diretoriaId: demandante.diretoriaId,
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
      tipo: v.tipo!,
      observacao: v.observacao || null,
      diretoriaId: v.diretoriaId,
    };
    const id = this.editandoId();
    try {
      await firstValueFrom(
        id ? this.#service.atualizar(id, payload) : this.#service.criar(payload),
      );
      this.dialogAberto.set(false);
      this.demandantes.reload();
    } catch {
      this.erro.set('Não foi possível salvar o demandante.');
    } finally {
      this.salvando.set(false);
    }
  }
}
