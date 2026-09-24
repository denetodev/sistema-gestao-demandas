import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { ClienteService } from '../cliente.service';
import { TipoCliente, type Cliente } from '../cliente.model';
import { Select } from 'primeng/select';
import { AuthService } from '../../../auth/auth.service';
import { BarraPagina } from '../../../layout/barra-pagina/barra-pagina';

@Component({
  selector: 'app-cliente-lista',
  imports: [ReactiveFormsModule, TableModule, Dialog, InputText, Button, Message, Tag, Select, BarraPagina],
  templateUrl: './cliente-lista.html',
  styleUrl: './cliente-lista.scss',
})
export class ClienteLista {
  #fb = inject(FormBuilder);
  #service = inject(ClienteService);

  clientes = this.#service.listar;

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
    tipo: this.#fb.control<TipoCliente | null>(null, Validators.required),
    observacao: ['',]
  });
auth = inject(AuthService);

  abrirNovo() {
    this.editandoId.set(null);
    this.erro.set(null);
    this.form.reset();
    this.dialogAberto.set(true);
  }

  abrirEdicao(cliente: Cliente) {
    this.editandoId.set(cliente.id);
    this.erro.set(null);
    this.form.setValue({
      nome: cliente.nome,
      tipo: cliente.tipo,
      observacao: cliente.observacao ?? '',
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
    };
    const id = this.editandoId();
    try {
      await firstValueFrom(id ? this.#service.atualizar(id, payload) : this.#service.criar(payload));
      this.dialogAberto.set(false);
      this.clientes.reload();
    } catch {
      this.erro.set('Não foi possível salvar o cliente.');
    } finally {
      this.salvando.set(false);
    }
  }
}