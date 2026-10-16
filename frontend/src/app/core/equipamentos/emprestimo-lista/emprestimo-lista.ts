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
import { DatePicker } from 'primeng/datepicker';
import { MultiSelect } from 'primeng/multiselect';
import { Checkbox } from 'primeng/checkbox';
import { AutoComplete } from 'primeng/autocomplete';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { AuthService } from '../../auth/auth.service';
import { BarraPagina } from '../../layout/barra-pagina/barra-pagina';
import { OpcaoPilula, Pilulas } from '../../ui/pilulas/pilulas';
import { chaveCValida, normalizarChaveC } from '../../util/chave-c';
import { EquipamentoService } from '../equipamento.service';
import { pedeAtencao, resumoItens, rotuloSituacao, severidadeSituacao } from '../equipamento-apresentacao';
import type { Emprestimo, Equipamento, FotoEmprestimo, GrupoEmprestimos, MomentoFoto } from '../equipamento.model';
import type { Demanda } from '../../../features/demandas/data-access/demanda.model';

@Component({
  selector: 'app-emprestimo-lista',
  imports: [
    DatePipe, FormsModule, ReactiveFormsModule, RouterLink, TableModule, Dialog, InputText, InputNumber, Textarea,
    DatePicker, MultiSelect, Checkbox, AutoComplete, Button, Tag, BarraPagina, Pilulas,
  ],
  templateUrl: './emprestimo-lista.html',
  styleUrl: './emprestimo-lista.scss',
})
export class EmprestimoLista {
  #fb = inject(FormBuilder);
  #service = inject(EquipamentoService);

  auth = inject(AuthService);
  emprestimos = this.#service.emprestimos;
  equipamentos = this.#service.equipamentos;
  termo = this.#service.termo;

  rotuloSituacao = rotuloSituacao;
  severidadeSituacao = severidadeSituacao;
  pedeAtencao = pedeAtencao;
  resumoItens = resumoItens;

  opcoesGrupo: OpcaoPilula[] = [
    { valor: 'ATIVOS', rotulo: 'Em andamento' },
    { valor: 'HISTORICO', rotulo: 'Histórico' },
  ];
  grupo = this.#service.grupo;
  mudarGrupo(valor: string) {
    this.grupo.set(valor as GrupoEmprestimos);
  }

  /** Quem só consulta vê "quem está com quê": o backend já devolve só o que está retirado. */
  titulo = computed(() => (this.auth.operaEquipamentos() ? 'Empréstimos' : 'Quem está com quê'));

  aguardando = computed(() => (this.emprestimos.value() ?? []).filter((e) => e.status === 'AGUARDANDO_AUTORIZACAO').length);
  atrasados = computed(() => (this.emprestimos.value() ?? []).filter((e) => e.atrasado).length);

  // ---- novo empréstimo ----
  dialogNovo = signal(false);
  etapa = signal<'dados' | 'termo'>('dados');
  salvando = signal(false);
  selecionados = signal<Equipamento[]>([]);
  quantidades = signal<Record<string, number>>({});

  /** Só o que tem unidade livre entra na escolha. */
  disponiveis = computed(() => (this.equipamentos.value() ?? []).filter((e) => e.quantidadeDisponivel > 0));

  form = this.#fb.nonNullable.group({
    retiranteNome: ['', [Validators.required, Validators.maxLength(160)]],
    retiranteChaveC: [''],
    retiranteContato: [''],
    finalidade: [''],
    externo: [false],
    localUso: [''],
    previstaDevolucao: this.#fb.control<Date | null>(null, Validators.required),
  });

  agora = new Date();

  abrirNovo() {
    this.form.reset({ externo: false });
    this.selecionados.set([]);
    this.quantidades.set({});
    this.demandaEscolhida.set(null);
    this.etapa.set('dados');
    this.equipamentos.reload();
    this.termo.reload();
    this.dialogNovo.set(true);
  }

  quantidadeDe(e: Equipamento): number {
    return this.quantidades()[e.id] ?? 1;
  }

  mudarQuantidade(e: Equipamento, valor: number | null) {
    this.quantidades.update((q) => ({ ...q, [e.id]: valor ?? 1 }));
  }

  // ---- vínculo opcional com demanda ----
  demandaEscolhida = signal<Demanda | null>(null);
  sugestoesDemanda = signal<Demanda[]>([]);

  async buscarDemandas(evento: { query: string }) {
    try {
      const pagina = await firstValueFrom(this.#service.buscarDemandas(evento.query));
      this.sugestoesDemanda.set(pagina.content);
    } catch {
      this.sugestoesDemanda.set([]);
    }
  }

  chaveOk(): boolean {
    const chave = normalizarChaveC(this.form.controls.retiranteChaveC.value);
    return !chave || chaveCValida(chave);
  }

  podeContinuar(): boolean {
    return this.form.valid && this.selecionados().length > 0 && this.chaveOk();
  }

  continuar() {
    if (this.podeContinuar()) this.etapa.set('termo');
  }

  /** O OK do termo é o botão que registra: sem ele o empréstimo não é criado. */
  async confirmarTermoERegistrar() {
    if (!this.podeContinuar()) return;
    this.salvando.set(true);
    const v = this.form.getRawValue();
    try {
      await firstValueFrom(
        this.#service.registrar({
          retiranteNome: v.retiranteNome.trim(),
          retiranteChaveC: normalizarChaveC(v.retiranteChaveC) || null,
          retiranteContato: v.retiranteContato.trim() || null,
          retirantePessoaId: null,
          finalidade: v.finalidade.trim() || null,
          externo: v.externo,
          localUso: v.localUso.trim() || null,
          previstaDevolucao: v.previstaDevolucao!.toISOString(),
          demandaId: this.demandaEscolhida()?.id ?? null,
          atividadeId: null,
          itens: this.selecionados().map((e) => ({
            equipamentoId: e.id,
            quantidade: e.controle === 'QUANTIDADE' ? this.quantidadeDe(e) : null,
          })),
          termoAceito: true,
        }),
      );
      this.dialogNovo.set(false);
      this.recarregar();
    } catch {
      // o erro já vira aviso na tela (interceptor); o diálogo fica aberto para corrigir
    } finally {
      this.salvando.set(false);
    }
  }

  // ---- ações sobre um empréstimo ----
  podeAutorizar(e: Emprestimo) {
    return this.auth.gerenciaEquipamentos() && e.status === 'AGUARDANDO_AUTORIZACAO';
  }
  podeEntregar(e: Emprestimo) {
    return this.auth.operaEquipamentos() && e.status === 'AUTORIZADO';
  }
  podeReceber(e: Emprestimo) {
    return this.auth.operaEquipamentos() && e.status === 'RETIRADO';
  }
  podeCancelar(e: Emprestimo) {
    return this.auth.operaEquipamentos() && (e.status === 'AGUARDANDO_AUTORIZACAO' || e.status === 'AUTORIZADO');
  }

  recarregar() {
    this.emprestimos.reload();
    this.equipamentos.reload();
  }

  private async agir(chamada: Promise<unknown>) {
    try {
      await chamada;
      this.recarregar();
    } catch {
      // aviso já exibido pelo interceptor
    }
  }

  autorizar(e: Emprestimo) {
    return this.agir(firstValueFrom(this.#service.autorizar(e.id)));
  }
  entregar(e: Emprestimo) {
    return this.agir(firstValueFrom(this.#service.retirar(e.id)));
  }
  cancelar(e: Emprestimo) {
    return this.agir(firstValueFrom(this.#service.cancelar(e.id)));
  }

  // ---- detalhes e fotos ----
  momentos: MomentoFoto[] = ['SAIDA', 'DEVOLUCAO'];
  detalhe = signal<Emprestimo | null>(null);
  fotos = signal<FotoEmprestimo[]>([]);
  enviandoFoto = signal(false);

  fotosDe(momento: MomentoFoto) {
    return this.fotos().filter((f) => f.momento === momento);
  }

  async abrirDetalhe(e: Emprestimo) {
    this.detalhe.set(e);
    await this.carregarFotos(e.id);
  }

  private async carregarFotos(id: string) {
    try {
      this.fotos.set(await firstValueFrom(this.#service.fotos(id)));
    } catch {
      this.fotos.set([]);
    }
  }

  /** Foto de saída só depois de autorizado; a de devolução, depois que o equipamento saiu. */
  podeFotografar(e: Emprestimo, momento: MomentoFoto) {
    if (!this.auth.operaEquipamentos()) return false;
    return momento === 'SAIDA'
      ? e.status === 'AUTORIZADO' || e.status === 'RETIRADO' || e.status === 'DEVOLVIDO'
      : e.status === 'RETIRADO' || e.status === 'DEVOLVIDO';
  }

  async escolherFoto(e: Emprestimo, momento: MomentoFoto, evento: Event) {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    input.value = '';
    if (!arquivo) return;
    this.enviandoFoto.set(true);
    try {
      await firstValueFrom(this.#service.enviarFoto(e.id, momento, arquivo));
      await this.carregarFotos(e.id);
    } catch {
      // aviso já exibido pelo interceptor
    } finally {
      this.enviandoFoto.set(false);
    }
  }

  async removerFoto(e: Emprestimo, foto: FotoEmprestimo) {
    try {
      await firstValueFrom(this.#service.removerFoto(e.id, foto.id));
      await this.carregarFotos(e.id);
    } catch {
      // aviso já exibido pelo interceptor
    }
  }

  // ---- recusar ----
  recusando = signal<Emprestimo | null>(null);
  motivo = signal('');

  abrirRecusa(e: Emprestimo) {
    this.motivo.set('');
    this.recusando.set(e);
  }

  async recusar() {
    const e = this.recusando();
    if (!e || !this.motivo().trim()) return;
    await this.agir(firstValueFrom(this.#service.recusar(e.id, this.motivo().trim())));
    this.recusando.set(null);
  }

  // ---- devolução ----
  devolvendo = signal<Emprestimo | null>(null);
  itensDevolvidos = signal<Set<string>>(new Set());
  observacoesDevolucao = signal('');

  abrirDevolucao(e: Emprestimo) {
    this.itensDevolvidos.set(new Set(e.itens.filter((i) => !i.devolvidoEm).map((i) => i.id)));
    this.observacoesDevolucao.set('');
    this.devolvendo.set(e);
  }

  alternarItem(id: string, marcado: boolean) {
    this.itensDevolvidos.update((s) => {
      const novo = new Set(s);
      if (marcado) novo.add(id);
      else novo.delete(id);
      return novo;
    });
  }

  async devolver() {
    const e = this.devolvendo();
    if (!e || this.itensDevolvidos().size === 0) return;
    const pendentes = e.itens.filter((i) => !i.devolvidoEm).length;
    const todos = this.itensDevolvidos().size === pendentes;
    await this.agir(
      firstValueFrom(this.#service.devolver(e.id, todos ? null : [...this.itensDevolvidos()], this.observacoesDevolucao().trim() || null)),
    );
    this.devolvendo.set(null);
  }
}
