import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { AuthService } from '../../auth/auth.service';
import { BarraPagina } from '../../layout/barra-pagina/barra-pagina';
import { CampoBusca } from '../../ui/campo-busca/campo-busca';
import { Ajuda } from '../../ajuda/ajuda/ajuda';
import { contemTexto } from '../../util/listas';
import { EquipamentoService } from '../equipamento.service';
import type { ControleEquipamento, Equipamento } from '../equipamento.model';

@Component({
  selector: 'app-inventario',
  imports: [
    DatePipe, FormsModule, ReactiveFormsModule, RouterLink, TableModule, Dialog, InputText, InputNumber, Textarea,
    Select, Button, Tag, BarraPagina, CampoBusca, Ajuda,
  ],
  templateUrl: './inventario.html',
  styleUrl: './inventario.scss',
})
export class Inventario {
  #fb = inject(FormBuilder);
  #service = inject(EquipamentoService);

  auth = inject(AuthService);
  equipamentos = this.#service.equipamentos;
  categorias = this.#service.categorias;
  termo = this.#service.termo;

  busca = signal('');
  categoriaFiltro = signal<string | null>(null);

  categoriasAtivas = computed(() => (this.categorias.value() ?? []).filter((c) => c.ativo));

  filtrados = computed(() =>
    (this.equipamentos.value() ?? []).filter(
      (e) =>
        contemTexto([e.nome, e.patrimonio, e.numeroSerie, e.localizacao, e.categoriaNome], this.busca()) &&
        (!this.categoriaFiltro() || e.categoriaId === this.categoriaFiltro()),
    ),
  );

  opcoesControle = [
    { label: 'Individual (cada peça é um registro)', value: 'INDIVIDUAL' as ControleEquipamento },
    { label: 'Por quantidade (lote)', value: 'QUANTIDADE' as ControleEquipamento },
  ];
  rotuloControle(c: ControleEquipamento) {
    return c === 'INDIVIDUAL' ? 'Individual' : 'Por quantidade';
  }

  // ---- equipamento ----
  dialogEquip = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);

  form = this.#fb.nonNullable.group({
    categoriaId: this.#fb.control<string | null>(null, Validators.required),
    nome: ['', [Validators.required, Validators.maxLength(160)]],
    patrimonio: [''],
    numeroSerie: [''],
    quantidadeTotal: this.#fb.control<number | null>(1),
    localizacao: [''],
    observacoes: [''],
  });

  /** O campo de quantidade só aparece para categoria por lote. */
  porQuantidade = computed(() => {
    // lê o valor do formulário de forma reativa pela lista de categorias e pela escolha atual
    const id = this.categoriaEscolhida();
    return this.categoriasAtivas().find((c) => c.id === id)?.controle === 'QUANTIDADE';
  });
  categoriaEscolhida = signal<string | null>(null);

  escolherCategoria(id: string | null) {
    this.form.controls.categoriaId.setValue(id);
    this.categoriaEscolhida.set(id);
  }

  abrirNovo() {
    this.editandoId.set(null);
    this.form.reset({ quantidadeTotal: 1 });
    this.categoriaEscolhida.set(null);
    this.dialogEquip.set(true);
  }

  abrirEdicao(e: Equipamento) {
    this.editandoId.set(e.id);
    this.form.setValue({
      categoriaId: e.categoriaId,
      nome: e.nome,
      patrimonio: e.patrimonio ?? '',
      numeroSerie: e.numeroSerie ?? '',
      quantidadeTotal: e.quantidadeTotal,
      localizacao: e.localizacao ?? '',
      observacoes: e.observacoes ?? '',
    });
    this.categoriaEscolhida.set(e.categoriaId);
    this.dialogEquip.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    const v = this.form.getRawValue();
    const payload = {
      categoriaId: v.categoriaId!,
      nome: v.nome.trim(),
      patrimonio: v.patrimonio.trim() || null,
      numeroSerie: v.numeroSerie.trim() || null,
      quantidadeTotal: this.porQuantidade() ? v.quantidadeTotal : null,
      localizacao: v.localizacao.trim() || null,
      observacoes: v.observacoes.trim() || null,
    };
    const id = this.editandoId();
    try {
      await firstValueFrom(id ? this.#service.atualizarEquipamento(id, payload) : this.#service.criarEquipamento(payload));
      this.dialogEquip.set(false);
      this.equipamentos.reload();
    } catch {
      // aviso já exibido pelo interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  async desativar(e: Equipamento) {
    try {
      await firstValueFrom(this.#service.desativarEquipamento(e.id));
      this.equipamentos.reload();
    } catch {
      // aviso já exibido pelo interceptor
    }
  }

  // ---- categoria ----
  dialogCategoria = signal(false);
  nomeCategoria = signal('');
  controleCategoria = signal<ControleEquipamento>('INDIVIDUAL');

  abrirCategoria() {
    this.nomeCategoria.set('');
    this.controleCategoria.set('INDIVIDUAL');
    this.dialogCategoria.set(true);
  }

  async salvarCategoria() {
    if (!this.nomeCategoria().trim()) return;
    try {
      await firstValueFrom(this.#service.criarCategoria(this.nomeCategoria().trim(), this.controleCategoria()));
      this.dialogCategoria.set(false);
      this.categorias.reload();
    } catch {
      // aviso já exibido pelo interceptor
    }
  }

  // ---- termo ----
  dialogTermo = signal(false);
  textoTermo = signal('');

  abrirTermo() {
    this.textoTermo.set(this.termo.value()?.texto ?? '');
    this.dialogTermo.set(true);
  }

  async salvarTermo() {
    if (!this.textoTermo().trim()) return;
    try {
      await firstValueFrom(this.#service.publicarTermo(this.textoTermo().trim()));
      this.dialogTermo.set(false);
      this.termo.reload();
    } catch {
      // aviso já exibido pelo interceptor
    }
  }
}
