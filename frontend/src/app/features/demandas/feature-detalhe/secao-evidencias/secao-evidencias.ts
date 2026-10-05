import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { InputText } from 'primeng/inputtext';
import { DemandaItensService } from '../../data-access/demanda-itens.service';
import { Atividade, Evidencia, Peca, TipoEvidencia } from '../../data-access/demanda-itens.model';

const TIPOS: TipoEvidencia[] = [
  'IMAGEM', 'SCREENSHOT', 'HTML', 'PDF', 'VIDEO', 'ARQUIVO', 'LINK', 'CODIGO',
  'MOCKUP', 'CAPTURA_SISTEMA', 'OBSERVACAO', 'CONFIRMACAO_MANUAL',
];

@Component({
  selector: 'app-secao-evidencias',
  imports: [ReactiveFormsModule, TableModule, Button, Dialog, Select, Textarea, InputText],
  templateUrl: './secao-evidencias.html',
  styleUrl: '../demanda-detalhe/demanda-detalhe.scss',
})
export class SecaoEvidencias {
  /** Todas as evidências do sistema; filtradas aqui pelas atividades e peças da demanda. */
  todas = input.required<Evidencia[]>();
  atividades = input.required<Atividade[]>();
  pecas = input.required<Peca[]>();
  editavel = input(false);
  alterado = output<void>();

  #service = inject(DemandaItensService);
  #fb = inject(FormBuilder);

  dialogAberto = signal(false);
  salvando = signal(false);
  opcoesTipo = TIPOS.map((t) => ({ value: t, label: t.replaceAll('_', ' ').toLowerCase() }));

  alvos = computed(() => [
    ...this.atividades().map((a) => ({
      value: `A:${a.id}`,
      label: `Atividade: ${a.tipoAtividadeNome}${a.descricao ? ' – ' + a.descricao : ''}`,
    })),
    ...this.pecas().map((p) => ({ value: `P:${p.id}`, label: `Peça: ${p.nome}` })),
  ]);

  itens = computed(() => {
    const atvs = new Map(this.atividades().map((a) => [a.id, a]));
    const pecas = new Map(this.pecas().map((p) => [p.id, p]));
    return this.todas()
      .filter((e) => (e.atividadeId && atvs.has(e.atividadeId)) || (e.pecaId && pecas.has(e.pecaId)))
      .map((e) => ({
        ...e,
        alvo: e.pecaId
          ? `Peça: ${pecas.get(e.pecaId)?.nome ?? ''}`
          : `Atividade: ${atvs.get(e.atividadeId!)?.tipoAtividadeNome ?? ''}`,
      }));
  });

  form = this.#fb.group({
    alvo: ['', Validators.required],
    tipo: this.#fb.control<TipoEvidencia>('IMAGEM', Validators.required),
    conteudo: [''],
    descricao: [''],
  });

  abrir() {
    this.form.reset({ alvo: '', tipo: 'IMAGEM', conteudo: '', descricao: '' });
    this.dialogAberto.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    try {
      const v = this.form.getRawValue();
      const [kind, id] = (v.alvo ?? '').split(':');
      await firstValueFrom(
        this.#service.criarEvidencia({
          atividadeId: kind === 'A' ? id : null,
          pecaId: kind === 'P' ? id : null,
          tipo: v.tipo!,
          conteudo: v.conteudo || null,
          descricao: v.descricao || null,
        }),
      );
      this.dialogAberto.set(false);
      this.alterado.emit();
    } catch {
      // toast do interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  async remover(e: Evidencia) {
    try {
      await firstValueFrom(this.#service.removerEvidencia(e.id));
      this.alterado.emit();
    } catch {
      // toast do interceptor
    }
  }
}
