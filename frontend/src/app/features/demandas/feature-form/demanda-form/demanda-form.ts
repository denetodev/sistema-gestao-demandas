import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
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
import { formatarData, paraData } from '../../../../core/util/data';
import { DiretoriaService } from '../../../../core/dados-mestres/diretoria/diretoria.service';

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
  #route = inject(ActivatedRoute);
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
  id = signal<string | null>(null);
  edicao = computed(() => this.id() !== null);
  carregandoDemanda = signal(false);

 async ngOnInit() {
  const id = this.#route.snapshot.paramMap.get('id');
  if (!id) return;

  this.id.set(id);
  this.carregandoDemanda.set(true);
  try {
    const d = await firstValueFrom(this.#demandaService.buscarPorId(id));
    this.form.patchValue({
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
  const id = this.id();
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
    this.#router.navigateByUrl('/demandas');
  } catch {
    this.erro.set(id ? 'Não foi possível salvar a demanda.' : 'Não foi possível criar a demanda.');
  } finally {
    this.carregando.set(false);
  }
}



}
