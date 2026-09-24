import { Component, computed, effect, inject, input, model, output, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { DemandaService } from '../../data-access/demanda.service';
import type { Prioridade } from '../../data-access/demanda.model';
import { DiretoriaService } from '../../../../core/dados-mestres/diretoria/diretoria.service';
import { formatarData, paraData } from '../../../../core/util/data';

@Component({
  selector: 'app-demanda-form',
  imports: [ReactiveFormsModule, Dialog, InputText, Select, DatePicker, InputNumber, Textarea, Button, Message],
  templateUrl: './demanda-form.html',
  styleUrl: './demanda-form.scss',
})
export class DemandaForm {
  visivel = model(false);
  demandaId = input<string | null>(null);
  salvo = output<void>();

  #fb = inject(FormBuilder);
  #demandaService = inject(DemandaService);
  diretorias = inject(DiretoriaService).listar;

  edicao = computed(() => this.demandaId() !== null);
  carregando = signal(false);
  carregandoDemanda = signal(false);
  erro = signal<string | null>(null);

  opcoesPrioridade = [
    { label: 'Baixa', value: 'BAIXA' },
    { label: 'Normal', value: 'NORMAL' },
    { label: 'Alta', value: 'ALTA' },
    { label: 'Urgente', value: 'URGENTE' },
  ];

  form = this.#fb.nonNullable.group({
    titulo: ['', Validators.required],
    diretoriaId: ['', Validators.required],
    prioridade: ['NORMAL' as Prioridade, Validators.required],
    dataPrazo: this.#fb.control<Date | null>(null),
    valor: this.#fb.control<number | null>(null),
    observacoes: [''],
  });

  constructor() {
    effect(() => {
      if (!this.visivel()) return;
      const id = this.demandaId();
      untracked(() => {
        this.erro.set(null);
        if (id) {
          this.#carregar(id);
        } else {
          this.form.reset({ prioridade: 'NORMAL' });
        }
      });
    });
  }

  async #carregar(id: string) {
    this.carregandoDemanda.set(true);
    try {
      const d = await firstValueFrom(this.#demandaService.buscarPorId(id));
      this.form.reset({
        titulo: d.titulo,
        diretoriaId: d.diretoriaId,
        prioridade: d.prioridade,
        dataPrazo: paraData(d.dataPrazo),
        valor: d.valor,
        observacoes: d.observacoes ?? '',
      });
    } catch {
      this.erro.set('Não foi possível carregar a demanda.');
    } finally {
      this.carregandoDemanda.set(false);
    }
  }

  async salvar() {
    if (this.form.invalid) return;
    this.carregando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    const id = this.demandaId();
    const payload = {
      titulo: v.titulo,
      diretoriaId: v.diretoriaId,
      prioridade: v.prioridade,
      dataPrazo: formatarData(v.dataPrazo),
      valor: v.valor,
      observacoes: v.observacoes || null,
    };
    try {
      await firstValueFrom(
        id ? this.#demandaService.atualizar(id, payload) : this.#demandaService.criar(payload),
      );
      this.visivel.set(false);
      this.salvo.emit();
    } catch {
      this.erro.set(id ? 'Não foi possível salvar a demanda.' : 'Não foi possível criar a demanda.');
    } finally {
      this.carregando.set(false);
    }
  }
}