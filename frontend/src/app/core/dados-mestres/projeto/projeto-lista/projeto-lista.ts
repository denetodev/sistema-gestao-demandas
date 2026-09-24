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
import { ClienteService } from '../../cliente/cliente.service';
import { DiretoriaService } from '../../diretoria/diretoria.service';
import { StatusProjeto, Projeto } from '../projeto.model';
import { ProjetoService } from '../projeto.service';
import { AuthService } from '../../../auth/auth.service';


@Component({
  selector: 'app-projeto-lista',
  imports: [ReactiveFormsModule, TableModule, Dialog, InputText, Textarea, Select, Button, Message, Tag],
  templateUrl: './projeto-lista.html',
  styleUrl: './projeto-lista.scss',
})
export class ProjetoLista {
  #fb = inject(FormBuilder);
  #service = inject(ProjetoService);

  projetos = this.#service.listar;
  clientes = inject(ClienteService).listar;
  diretorias = inject(DiretoriaService).listar;

  opcoesStatus = [
    { label: 'Ativo', value: 'ATIVO' },
    { label: 'Pausado', value: 'PAUSADO' },
    { label: 'Encerrado', value: 'ENCERRADO' },
  ];

  dialogAberto = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  form = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    clienteId: this.#fb.control<string | null>(null),
    diretoriaId: this.#fb.control<string | null>(null),
    descricao: [''],
    status: ['ATIVO' as StatusProjeto],
  });
auth = inject(AuthService);

  severityStatus(status: StatusProjeto) {
    switch (status) {
      case 'ATIVO': return 'success';
      case 'PAUSADO': return 'warn';
      default: return 'secondary';
    }
  }

  abrirNovo() {
    this.editandoId.set(null);
    this.erro.set(null);
    this.form.reset({ status: 'ATIVO' });
    this.dialogAberto.set(true);
  }

  abrirEdicao(projeto: Projeto) {
    this.editandoId.set(projeto.id);
    this.erro.set(null);
    this.form.setValue({
      nome: projeto.nome,
      clienteId: projeto.clienteId,
      diretoriaId: projeto.diretoriaId,
      descricao: projeto.descricao ?? '',
      status: projeto.status,
    });
    this.dialogAberto.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    const payload = { ...v, descricao: v.descricao || null };
    const id = this.editandoId();
    try {
      await firstValueFrom(id ? this.#service.atualizar(id, payload) : this.#service.criar(payload));
      this.dialogAberto.set(false);
      this.projetos.reload();
    } catch {
      this.erro.set('Não foi possível salvar o projeto.');
    } finally {
      this.salvando.set(false);
    }
  }
}