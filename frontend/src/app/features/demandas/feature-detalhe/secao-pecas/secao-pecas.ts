import { Component, computed, inject, input, output, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { DatePicker } from 'primeng/datepicker';
import { DemandaItensService } from '../../data-access/demanda-itens.service';
import { Peca } from '../../data-access/demanda-itens.model';
import { TipoPecaService } from '../../../../core/dados-mestres/tipo-peca/tipo-peca.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { formatarData, paraData } from '../../../../core/util/data';

@Component({
  selector: 'app-secao-pecas',
  imports: [ReactiveFormsModule, CurrencyPipe, TableModule, Button, Dialog, Select, InputText, InputNumber, DatePicker],
  templateUrl: './secao-pecas.html',
  styleUrl: '../demanda-detalhe/demanda-detalhe.scss',
})
export class SecaoPecas {
  demandaId = input.required<string>();
  itens = input.required<Peca[]>();
  editavel = input(false);
  alterado = output<void>();

  #service = inject(DemandaItensService);
  #fb = inject(FormBuilder);
  #auth = inject(AuthService);

  tipos = inject(TipoPecaService).listar;
  pessoas = this.#service.pessoas();
  dialogAberto = signal(false);
  salvando = signal(false);
  editandoId = signal<string | null>(null);

  tiposAtivos = computed(() => (this.tipos.value() ?? []).filter((t) => t.ativo));
  opcoesPessoa = computed(() =>
    (this.pessoas.value() ?? []).filter((p) => p.status === 'ATIVO' && p.aprovadoEm !== null),
  );
  total = computed(() => this.itens().reduce((s, p) => s + (p.valorTotal ?? 0), 0));

  form = this.#fb.group({
    tipoPecaId: ['', Validators.required],
    nome: ['', Validators.required],
    quantidade: this.#fb.control<number | null>(1),
    pessoaId: this.#fb.control<string | null>(null),
    dataEntrega: this.#fb.control<Date | null>(null),
  });

  #tipoSelecionado = toSignal(this.form.controls.tipoPecaId.valueChanges, { initialValue: '' });
  valorReferencia = computed(
    () => this.tiposAtivos().find((t) => t.id === this.#tipoSelecionado())?.valorReferencia ?? null,
  );

  abrir() {
    this.editandoId.set(null);
    this.form.controls.tipoPecaId.enable();
    this.form.reset({
      tipoPecaId: '',
      nome: '',
      quantidade: 1,
      pessoaId: this.#auth.pessoa()?.id ?? null,
      dataEntrega: null,
    });
    this.dialogAberto.set(true);
  }

  /** Edição: o tipo fica travado, porque o valor unitário é congelado no lançamento. */
  editar(p: Peca) {
    this.editandoId.set(p.id);
    this.form.reset({
      tipoPecaId: p.tipoPecaId,
      nome: p.nome,
      quantidade: p.quantidade,
      pessoaId: p.pessoaId,
      dataEntrega: paraData(p.dataEntrega),
    });
    this.form.controls.tipoPecaId.disable();
    this.dialogAberto.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    try {
      const v = this.form.getRawValue();
      const payload = {
        demandaId: this.demandaId(),
        tipoPecaId: v.tipoPecaId!,
        nome: v.nome!,
        descricao: this.itens().find((x) => x.id === this.editandoId())?.descricao ?? null,
        quantidade: v.quantidade,
        pessoaId: v.pessoaId,
        dataEntrega: formatarData(v.dataEntrega),
      };
      const id = this.editandoId();
      await firstValueFrom(id ? this.#service.atualizarPeca(id, payload) : this.#service.criarPeca(payload));
      this.dialogAberto.set(false);
      this.alterado.emit();
    } catch {
      // toast do interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  async remover(p: Peca) {
    try {
      await firstValueFrom(this.#service.removerPeca(p.id));
      this.alterado.emit();
    } catch {
      // toast do interceptor
    }
  }
}
