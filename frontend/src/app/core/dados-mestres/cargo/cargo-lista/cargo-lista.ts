import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { ConfirmationService } from 'primeng/api';
import { BarraPagina } from '../../../layout/barra-pagina/barra-pagina';
import { Ajuda } from '../../../ajuda/ajuda/ajuda';
import { CampoBusca } from '../../../ui/campo-busca/campo-busca';
import { PessoaService } from '../../../pessoas/pessoa.service';
import { contemTexto } from '../../../util/listas';
import { CargoService, type Cargo } from '../cargo.service';

export interface LinhaCargo {
  cargo: Cargo;
  pessoas: number;
}

/** Cargos do time (Designer, Operador, Videomaker...): cadastro solto, sem ligação com equipe ou núcleo. */
@Component({
  selector: 'app-cargo-lista',
  imports: [ReactiveFormsModule, TableModule, Dialog, InputText, Textarea, Button, Message, BarraPagina, Ajuda, CampoBusca],
  templateUrl: './cargo-lista.html',
  styleUrl: './cargo-lista.scss',
})
export class CargoLista {
  #fb = inject(FormBuilder);
  #confirmacao = inject(ConfirmationService);
  #service = inject(CargoService);

  cargos = this.#service.listar;
  pessoas = inject(PessoaService).todas;

  busca = signal('');

  /** Cada cargo ativo com quantas pessoas ativas o ocupam. */
  linhas = computed<LinhaCargo[]>(() => {
    const ocupacao = new Map<string, number>();
    for (const p of this.pessoas.value() ?? []) {
      if (p.cargoId && p.status === 'ATIVO') ocupacao.set(p.cargoId, (ocupacao.get(p.cargoId) ?? 0) + 1);
    }
    return (this.cargos.value() ?? [])
      .filter((c) => c.ativo)
      .map((cargo) => ({ cargo, pessoas: ocupacao.get(cargo.id) ?? 0 }))
      .sort((a, b) => a.cargo.nome.localeCompare(b.cargo.nome, 'pt-BR'));
  });

  filtradas = computed(() => this.linhas().filter((l) => contemTexto([l.cargo.nome, l.cargo.descricao], this.busca())));

  // ---- janela ----
  aberta = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  form = this.#fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120), semSoEspacos]],
    descricao: ['', Validators.maxLength(500)],
  });

  mostrarErroNome(): boolean {
    const c = this.form.controls.nome;
    return c.invalid && (c.touched || c.dirty);
  }

  novo() {
    this.editandoId.set(null);
    this.erro.set(null);
    this.form.reset();
    this.aberta.set(true);
  }

  editar(c: Cargo) {
    this.editandoId.set(c.id);
    this.erro.set(null);
    this.form.reset({ nome: c.nome, descricao: c.descricao ?? '' });
    this.aberta.set(true);
  }

  fechar() {
    this.aberta.set(false);
  }

  async salvar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    const corpo = { nome: v.nome.trim(), descricao: v.descricao.trim() || null };
    const id = this.editandoId();
    try {
      await firstValueFrom(id ? this.#service.atualizar(id, corpo) : this.#service.criar(corpo));
      this.aberta.set(false);
      this.cargos.reload();
    } catch (e) {
      const mensagem = (e as { error?: { mensagem?: string } })?.error?.mensagem;
      this.erro.set(mensagem ?? 'Não foi possível salvar o cargo. Tente de novo.');
    } finally {
      this.salvando.set(false);
    }
  }

  desativar(l: LinhaCargo) {
    if (l.pessoas > 0) {
      this.#confirmacao.confirm({
        header: 'Desativar cargo',
        message: `${l.pessoas} pessoa(s) ativa(s) ainda ocupam o cargo ${l.cargo.nome}. Mude o cargo delas em Pessoas antes de desativar.`,
        acceptLabel: 'Entendi',
        rejectVisible: false,
      });
      return;
    }
    this.#confirmacao.confirm({
      header: 'Desativar cargo',
      message: `Desativar o cargo ${l.cargo.nome}? Ele some das listas de escolha, e o histórico continua.`,
      acceptLabel: 'Desativar',
      accept: async () => {
        try {
          await firstValueFrom(this.#service.desativar(l.cargo.id));
          this.cargos.reload();
        } catch {
          // o aviso com o motivo vem do interceptor
        }
      },
    });
  }
}

/** Nome só com espaços conta como vazio. */
function semSoEspacos(c: AbstractControl): ValidationErrors | null {
  return typeof c.value === 'string' && c.value.length > 0 && c.value.trim() === '' ? { required: true } : null;
}
