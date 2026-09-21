import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { DemandaService } from '../../data-access/demanda.service';
import type { Prioridade } from '../../data-access/demanda.model';
import { DiretoriaService } from '../../../../core/dados-mestres/diretoria.service';

@Component({
  selector: 'app-demanda-form',
  imports: [
    ReactiveFormsModule,
    InputText,
    Select,
    DatePicker,
    InputNumber,
    Textarea,
    Button,
    Message,
  ],
  templateUrl: './demanda-form.html',
  styleUrl: './demanda-form.scss',
})
export class DemandaForm {
  #fb = inject(FormBuilder);
  #demandaService = inject(DemandaService);
  #router = inject(Router);
  diretorias = inject(DiretoriaService).listar;

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

  carregando = signal(false);
  erro = signal<string | null>(null);

  async criar() {
    if (this.form.invalid) return;
    this.carregando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    try {
      await firstValueFrom(
        this.#demandaService.criar({
          titulo: v.titulo,
          diretoriaId: v.diretoriaId,
          prioridade: v.prioridade,
          dataPrazo: this.#formatarData(v.dataPrazo),
          valor: v.valor,
          observacoes: v.observacoes || null,
        }),
      );
      this.#router.navigateByUrl('/demandas');
    } catch {
      this.erro.set('Não foi possível criar a demanda.');
    } finally {
      this.carregando.set(false);
    }
  }

  #formatarData(data: Date | null): string | null {
    if (!data) return null;
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}
