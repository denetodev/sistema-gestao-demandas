import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { AreaService } from '../../area/area.service';
import { AuthService } from '../../../auth/auth.service';
import { BarraPagina } from '../../../layout/barra-pagina/barra-pagina';
import type { TipoPeca } from '../tipo-peca.model';
import { TipoPecaService } from '../tipo-peca.service';
import { CurrencyPipe } from '@angular/common';

@Component({
  selector: 'app-tipo-peca-lista',
  imports: [
    ReactiveFormsModule,
    TableModule,
    Dialog,
    InputText,
    InputNumber,
    Textarea,
    Select,
    Button,
    Message,
    Tag,
    BarraPagina,
    CurrencyPipe,
  ],
  templateUrl: './tipo-peca-lista.html',
  styleUrl: './tipo-peca-lista.scss',
})
export class TipoPecaLista {
  #fb = inject(FormBuilder);
  #service = inject(TipoPecaService);

  auth = inject(AuthService);
  tipos = this.#service.listar;
  areas = inject(AreaService).listar;

  tiposOrdenados = computed(() =>
    [...(this.tipos.value() ?? [])].sort(
      (a, b) =>
        a.areaNome.localeCompare(b.areaNome, 'pt-BR') || a.nome.localeCompare(b.nome, 'pt-BR'),
    ),
  );

  /** Admin e Gestor gerenciam tudo; Referência de Equipe só a própria área. */
  podeCriar = computed(
    () => this.auth.temPerfil('ADMIN', 'GESTOR') || !!this.auth.pessoa()?.referenciaAreaId,
  );

  podeGerenciar(tipo: TipoPeca): boolean {
    return this.auth.temPerfil('ADMIN', 'GESTOR') || this.auth.ehReferenciaDa(tipo.areaId);
  }

  /** Referência que não é Admin/Gestor só cadastra na própria área. */
  areaTravada = computed(
    () => !this.auth.temPerfil('ADMIN', 'GESTOR') && !!this.auth.pessoa()?.referenciaAreaId,
  );

  dialogAberto = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  form = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    descricao: [''],
    areaId: this.#fb.control<string | null>(null, Validators.required),
    valorReferencia: this.#fb.control<number | null>(null, Validators.required),
  });

  abrirNovo() {
    this.editandoId.set(null);
    this.erro.set(null);
    this.form.reset({
      areaId: this.areaTravada() ? this.auth.pessoa()!.referenciaAreaId : null,
    });
    this.dialogAberto.set(true);
  }

  abrirEdicao(tipo: TipoPeca) {
    this.editandoId.set(tipo.id);
    this.erro.set(null);
    this.form.setValue({
      nome: tipo.nome,
      descricao: tipo.descricao ?? '',
      areaId: tipo.areaId,
      valorReferencia: tipo.valorReferencia,
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
      descricao: v.descricao || null,
      areaId: v.areaId!,
      valorReferencia: v.valorReferencia!,
    };
    const id = this.editandoId();
    try {
      await firstValueFrom(
        id ? this.#service.atualizar(id, payload) : this.#service.criar(payload),
      );
      this.dialogAberto.set(false);
      this.tipos.reload();
    } catch {
      this.erro.set('Não foi possível salvar o tipo de peça.');
    } finally {
      this.salvando.set(false);
    }
  }
}
