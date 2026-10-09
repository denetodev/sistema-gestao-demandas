import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { DatePicker } from 'primeng/datepicker';
import { DemandaItensService } from '../../data-access/demanda-itens.service';
import { Atividade } from '../../data-access/demanda-itens.model';
import { TipoAtividadeService } from '../../../../core/dados-mestres/tipo-atividade/tipo-atividade.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { formatarData, paraData } from '../../../../core/util/data';

@Component({
  selector: 'app-secao-atividades',
  imports: [ReactiveFormsModule, TableModule, Button, Dialog, Select, Textarea, DatePicker],
  templateUrl: './secao-atividades.html',
  styleUrl: '../demanda-detalhe/demanda-detalhe.scss',
})
export class SecaoAtividades {
  demandaId = input.required<string>();
  itens = input.required<Atividade[]>();
  editavel = input(false);
  alterado = output<void>();

  #service = inject(DemandaItensService);
  #fb = inject(FormBuilder);
  #auth = inject(AuthService);

  tipos = inject(TipoAtividadeService).listar;
  pessoas = this.#service.pessoas();
  dialogAberto = signal(false);
  salvando = signal(false);
  editandoId = signal<string | null>(null);

  tiposAtivos = computed(() => (this.tipos.value() ?? []).filter((t) => t.ativo));
  opcoesPessoa = computed(() =>
    (this.pessoas.value() ?? []).filter((p) => p.status === 'ATIVO' && p.aprovadoEm !== null),
  );

  form = this.#fb.group({
    tipoAtividadeId: ['', Validators.required],
    pessoaId: this.#fb.control<string | null>(null),
    dataRealizacao: this.#fb.control<Date | null>(new Date()),
    descricao: [''],
  });

  abrir() {
    this.editandoId.set(null);
    this.form.reset({
      tipoAtividadeId: '',
      pessoaId: this.#auth.pessoa()?.id ?? null,
      dataRealizacao: new Date(),
      descricao: '',
    });
    this.dialogAberto.set(true);
  }

  editar(a: Atividade) {
    this.editandoId.set(a.id);
    this.form.reset({
      tipoAtividadeId: a.tipoAtividadeId,
      pessoaId: a.pessoaId,
      dataRealizacao: paraData(a.dataRealizacao),
      descricao: a.descricao ?? '',
    });
    this.dialogAberto.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    try {
      const v = this.form.getRawValue();
      const payload = {
        demandaId: this.demandaId(),
        tipoAtividadeId: v.tipoAtividadeId!,
        pessoaId: v.pessoaId,
        descricao: v.descricao || null,
        dataRealizacao: formatarData(v.dataRealizacao),
      };
      const id = this.editandoId();
      await firstValueFrom(id ? this.#service.atualizarAtividade(id, payload) : this.#service.criarAtividade(payload));
      this.dialogAberto.set(false);
      this.alterado.emit();
    } catch {
      // toast do interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  async remover(a: Atividade) {
    try {
      await firstValueFrom(this.#service.removerAtividade(a.id));
      this.alterado.emit();
    } catch {
      // toast do interceptor
    }
  }
}
