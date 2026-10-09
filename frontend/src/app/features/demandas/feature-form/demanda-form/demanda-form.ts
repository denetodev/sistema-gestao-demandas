import { Component, computed, effect, inject, input, model, output, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { DemandaService } from '../../data-access/demanda.service';
import type { Prioridade } from '../../data-access/demanda.model';
import { toSignal } from '@angular/core/rxjs-interop';
import { DemandanteService } from '../../../../core/dados-mestres/demandante/demandante.service';
import { ProjetoService } from '../../../../core/dados-mestres/projeto/projeto.service';
import { CampanhaService } from '../../../../core/dados-mestres/campanha/campanha.service';
import { DiretoriaService } from '../../../../core/dados-mestres/diretoria/diretoria.service';
import { formatarData, paraData } from '../../../../core/util/data';

@Component({
  selector: 'app-demanda-form',
  imports: [ReactiveFormsModule, Dialog, InputText, Select, DatePicker, Textarea, Button, Message],
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
  #demandantes = inject(DemandanteService).listar;
  #projetos = inject(ProjetoService).listar;
  #campanhas = inject(CampanhaService).listar;

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
    descricao: [''],
    diretoriaId: ['', Validators.required],
    demandanteId: this.#fb.control<string | null>(null),
    projetoId: this.#fb.control<string | null>(null),
    campanhaId: this.#fb.control<string | null>(null),
    prioridade: ['NORMAL' as Prioridade, Validators.required],
    dataPrazo: this.#fb.control<Date | null>(null),
    observacoes: [''],
    linkExterno: ['', Validators.pattern(/^\s*$|^https?:\/\/\S+$/i)],
  });

  // Cascata: diretoria filtra demandante e projeto; demandante filtra projeto; projeto filtra campanha.
  #diretoria = toSignal(this.form.controls.diretoriaId.valueChanges, { initialValue: '' });
  #demandante = toSignal(this.form.controls.demandanteId.valueChanges, { initialValue: null });
  #projeto = toSignal(this.form.controls.projetoId.valueChanges, { initialValue: null });

  opcoesDemandante = computed(() =>
    (this.#demandantes.value() ?? []).filter(
      (d) => d.ativo && (!this.#diretoria() || !d.diretoriaId || d.diretoriaId === this.#diretoria()),
    ),
  );
  opcoesProjeto = computed(() =>
    (this.#projetos.value() ?? []).filter(
      (p) =>
        p.status !== 'ENCERRADO' &&
        (!this.#diretoria() || !p.diretoriaId || p.diretoriaId === this.#diretoria()) &&
        (!this.#demandante() || p.demandanteId === this.#demandante()),
    ),
  );
  opcoesCampanha = computed(() =>
    (this.#campanhas.value() ?? []).filter((c) => !!this.#projeto() && c.projetoId === this.#projeto()),
  );

  constructor() {
    this.form.controls.diretoriaId.valueChanges.subscribe(() => {
      this.#limparSePerdido('demandanteId', this.opcoesDemandante());
      this.#limparSePerdido('projetoId', this.opcoesProjeto());
      this.#limparSePerdido('campanhaId', this.opcoesCampanha());
    });
    this.form.controls.demandanteId.valueChanges.subscribe(() => {
      this.#limparSePerdido('projetoId', this.opcoesProjeto());
      this.#limparSePerdido('campanhaId', this.opcoesCampanha());
    });
    this.form.controls.projetoId.valueChanges.subscribe(() =>
      this.#limparSePerdido('campanhaId', this.opcoesCampanha()),
    );
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

  /** Zera o campo se o valor atual saiu das opções disponíveis. */
  #limparSePerdido(campo: 'demandanteId' | 'projetoId' | 'campanhaId', opcoes: { id: string }[]) {
    const ctrl = this.form.controls[campo];
    if (ctrl.value && !opcoes.some((o) => o.id === ctrl.value)) ctrl.setValue(null);
  }

  async #carregar(id: string) {
    this.carregandoDemanda.set(true);
    try {
      const d = await firstValueFrom(this.#demandaService.buscarPorId(id));
      this.form.reset({
        titulo: d.titulo,
        descricao: d.descricao ?? '',
        diretoriaId: d.diretoriaId,
        demandanteId: d.demandanteId,
        projetoId: d.projetoId,
        campanhaId: d.campanhaId,
        prioridade: d.prioridade,
        dataPrazo: paraData(d.dataPrazo),
        observacoes: d.observacoes ?? '',
        linkExterno: d.linkExterno ?? '',
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
      descricao: v.descricao || null,
      diretoriaId: v.diretoriaId,
      demandanteId: v.demandanteId,
      projetoId: v.projetoId,
      campanhaId: v.campanhaId,
      prioridade: v.prioridade,
      dataPrazo: formatarData(v.dataPrazo),
      observacoes: v.observacoes || null,
      linkExterno: v.linkExterno.trim() || null,
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