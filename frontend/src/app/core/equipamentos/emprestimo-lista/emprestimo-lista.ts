import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, firstValueFrom, of, switchMap } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { DatePicker } from 'primeng/datepicker';
import { MultiSelect } from 'primeng/multiselect';
import { Checkbox } from 'primeng/checkbox';
import { Select } from 'primeng/select';
import { AutoComplete } from 'primeng/autocomplete';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { AuthService } from '../../auth/auth.service';
import { BarraPagina } from '../../layout/barra-pagina/barra-pagina';
import { OpcaoPilula, Pilulas } from '../../ui/pilulas/pilulas';
import { CampoBusca } from '../../ui/campo-busca/campo-busca';
import { Ajuda } from '../../ajuda/ajuda/ajuda';
import { chaveCValida, normalizarChaveC } from '../../util/chave-c';
import { EquipamentoService } from '../equipamento.service';
import {
  FILTRO_VAZIO, FiltroEmprestimos, SituacaoFiltro, filtroAtivo, filtrarEmprestimos, pedeAtencao, resumoItens,
  rotuloSituacao, severidadeSituacao,
} from '../equipamento-apresentacao';
import { PendenciasEquipamentos } from '../pendencias-equipamentos';
import type { Emprestimo, Equipamento, FotoEmprestimo, GrupoEmprestimos, MomentoFoto } from '../equipamento.model';
import type { Demanda } from '../../../features/demandas/data-access/demanda.model';

@Component({
  selector: 'app-emprestimo-lista',
  imports: [
    DatePipe, FormsModule, ReactiveFormsModule, RouterLink, TableModule, Dialog, InputText, InputNumber, Textarea,
    DatePicker, MultiSelect, Checkbox, AutoComplete, Select, Button, Tag, BarraPagina, Pilulas, CampoBusca, Ajuda,
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
  #pendencias = inject(PendenciasEquipamentos);

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
    this.limparFiltros(); // as situações mudam de um grupo para o outro
  }

  // ---- busca e filtros (a lista carregada é pequena: filtra no navegador) ----
  filtro = signal<FiltroEmprestimos>({ ...FILTRO_VAZIO });
  periodo = signal<(Date | null)[] | null>(null);

  filtrados = computed(() => filtrarEmprestimos(this.emprestimos.value() ?? [], this.filtro()));
  filtrosAtivos = computed(() => filtroAtivo(this.filtro()));

  opcoesSituacao = computed<{ label: string; value: SituacaoFiltro }[]>(() =>
    this.grupo() === 'ATIVOS'
      ? [
          { label: 'Aguardando autorização', value: 'AGUARDANDO_AUTORIZACAO' },
          { label: 'Autorizado', value: 'AUTORIZADO' },
          { label: 'Com a pessoa', value: 'RETIRADO' },
          { label: 'Atrasado', value: 'ATRASADO' },
        ]
      : [
          { label: 'Devolvido', value: 'DEVOLVIDO' },
          { label: 'Recusado', value: 'RECUSADO' },
          { label: 'Cancelado', value: 'CANCELADO' },
        ],
  );
  opcoesTipo = [
    { label: 'Externos (saem do prédio)', value: true },
    { label: 'Internos', value: false },
  ];

  ajustarFiltro(parcial: Partial<FiltroEmprestimos>) {
    this.filtro.update((f) => ({ ...f, ...parcial }));
  }

  mudarPeriodo(valor: (Date | null)[] | null) {
    this.periodo.set(valor);
    this.ajustarFiltro({ de: valor?.[0] ?? null, ate: valor?.[1] ?? valor?.[0] ?? null });
  }

  limparFiltros() {
    this.filtro.set({ ...FILTRO_VAZIO });
    this.periodo.set(null);
  }

  aguardando = computed(() => (this.emprestimos.value() ?? []).filter((e) => e.status === 'AGUARDANDO_AUTORIZACAO').length);
  atrasados = computed(() => (this.emprestimos.value() ?? []).filter((e) => e.atrasado).length);

  // ---- novo empréstimo ----
  dialogNovo = signal(false);
  etapa = signal<'dados' | 'termo'>('dados');
  salvando = signal(false);
  selecionados = signal<Equipamento[]>([]);
  quantidades = signal<Record<string, number>>({});


  form = this.#fb.nonNullable.group({
    retiranteNome: ['', [Validators.required, Validators.maxLength(160)]],
    retiranteChaveC: [''],
    retiranteContato: [''],
    finalidade: [''],
    externo: [false],
    localUso: [''],
    retiradaPrevista: this.#fb.control<Date | null>(null),
    previstaDevolucao: this.#fb.control<Date | null>(null, Validators.required),
  });

  agora = new Date();

  // ---- disponibilidade no período escolhido ----
  // O que vale não é "livre agora", e sim livre durante TODO o período pedido: um equipamento que volta amanhã
  // pode ser reservado para a semana que vem.
  #valoresDoForm = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  /** Retirada vazia significa agora (calculado na hora da consulta, para a chave não mudar a cada tecla). */
  janela = computed(
    () => {
      const v = this.#valoresDoForm();
      const fim = v.previstaDevolucao ?? null;
      if (!fim) return null;
      return { retirada: v.retiradaPrevista ? v.retiradaPrevista.toISOString() : null, fim: fim.toISOString() };
    },
    { equal: (a, b) => a?.retirada === b?.retirada && a?.fim === b?.fim },
  );

  #livresNoPeriodo = toSignal(
    toObservable(this.janela).pipe(
      debounceTime(250),
      switchMap((j) => {
        if (!j || this.editando()) return of(null);
        const inicio = j.retirada ?? new Date().toISOString();
        if (new Date(j.fim) <= new Date(inicio)) return of(null);
        return this.#service.disponibilidade(inicio, j.fim).pipe(catchError(() => of(null)));
      }),
    ),
    { initialValue: null },
  );

  /** Com o período definido, só entra o que está livre nele; sem período, tudo que está ativo. */
  opcoesEquipamentos = computed(() => {
    const livres = this.#livresNoPeriodo();
    const mapa = livres ? new Map(livres.map((d) => [d.equipamentoId, d.quantidadeDisponivel])) : null;
    return (this.equipamentos.value() ?? [])
      .map((e) => ({ ...e, livre: mapa ? (mapa.get(e.id) ?? 0) : e.quantidadeDisponivel }))
      .filter((e) => (mapa ? e.livre > 0 : true));
  });

  periodoDefinido = computed(() => this.#livresNoPeriodo() !== null);

  /** Quanto cabe no período (lote); sem período, o que está livre agora. */
  livreDe(e: Equipamento): number {
    return this.opcoesEquipamentos().find((o) => o.id === e.id)?.livre ?? 0;
  }

  /** O que já estava escolhido e deixou de estar livre ao mudar as datas. */
  indisponiveisSelecionados = computed(() =>
    this.periodoDefinido() ? this.selecionados().filter((e) => !this.opcoesEquipamentos().some((o) => o.id === e.id)) : [],
  );

  periodoValido(): boolean {
    const v = this.form.getRawValue();
    return !v.retiradaPrevista || !v.previstaDevolucao || v.previstaDevolucao > v.retiradaPrevista;
  }

  /** Preenchido quando o diálogo está corrigindo um pedido que espera autorização. */
  editando = signal<Emprestimo | null>(null);

  abrirEdicao(e: Emprestimo) {
    this.editando.set(e);
    this.form.reset({
      retiranteNome: e.retiranteNome,
      retiranteChaveC: e.retiranteChaveC ?? '',
      retiranteContato: e.retiranteContato ?? '',
      finalidade: e.finalidade ?? '',
      externo: e.externo,
      localUso: e.localUso ?? '',
      retiradaPrevista: new Date(e.retiradaPrevista),
      previstaDevolucao: new Date(e.previstaDevolucao),
    });
    this.demandaEscolhida.set(e.demandaId ? ({ id: e.demandaId, titulo: e.demandaTitulo ?? '' } as Demanda) : null);
    this.etapa.set('dados');
    this.dialogNovo.set(true);
  }

  podeSalvarEdicao(): boolean {
    return this.form.valid && this.chaveOk() && this.periodoValido();
  }

  /** Em edição salva direto (os equipamentos e o termo não mudam); no pedido novo segue para o termo. */
  enviar() {
    if (this.editando()) void this.salvarEdicao();
    else this.continuar();
  }

  async salvarEdicao() {
    const e = this.editando();
    if (!e || !this.podeSalvarEdicao()) return;
    this.salvando.set(true);
    const v = this.form.getRawValue();
    try {
      await firstValueFrom(
        this.#service.editar(e.id, {
          retiranteNome: v.retiranteNome.trim(),
          retiranteChaveC: normalizarChaveC(v.retiranteChaveC) || null,
          retiranteContato: v.retiranteContato.trim() || null,
          finalidade: v.finalidade.trim() || null,
          externo: v.externo,
          localUso: v.localUso.trim() || null,
          retiradaPrevista: v.retiradaPrevista ? v.retiradaPrevista.toISOString() : null,
          previstaDevolucao: v.previstaDevolucao!.toISOString(),
          demandaId: this.demandaEscolhida()?.id ?? null,
        }),
      );
      this.dialogNovo.set(false);
      this.recarregar();
    } catch {
      // aviso já exibido pelo interceptor; o diálogo fica aberto para corrigir
    } finally {
      this.salvando.set(false);
    }
  }

  abrirNovo() {
    this.editando.set(null);
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
    return this.form.valid && this.selecionados().length > 0 && this.chaveOk() && this.periodoValido()
      && this.indisponiveisSelecionados().length === 0;
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
          retiradaPrevista: v.retiradaPrevista ? v.retiradaPrevista.toISOString() : null,
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
  podeEditar(e: Emprestimo) {
    return this.auth.operaEquipamentos() && e.status === 'AGUARDANDO_AUTORIZACAO';
  }
  podeProrrogar(e: Emprestimo) {
    return this.auth.operaEquipamentos() && e.status === 'RETIRADO';
  }
  podeCancelar(e: Emprestimo) {
    return this.auth.operaEquipamentos() && (e.status === 'AGUARDANDO_AUTORIZACAO' || e.status === 'AUTORIZADO');
  }

  recarregar() {
    this.emprestimos.reload();
    this.equipamentos.reload();
    this.#pendencias.resumo.reload();
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
  erroFoto = signal<string | null>(null);

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
    this.erroFoto.set(null);
    try {
      await firstValueFrom(this.#service.enviarFoto(e.id, momento, arquivo));
      await this.carregarFotos(e.id);
    } catch (erro) {
      // o interceptor só avisa em alguns casos: a foto que não subiu precisa aparecer na própria janela
      const mensagem = (erro as { error?: { mensagem?: string } })?.error?.mensagem;
      this.erroFoto.set(mensagem ? mensagem : 'Não foi possível enviar a foto. Tente de novo.');
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

  // ---- prorrogar ----
  prorrogando = signal<Emprestimo | null>(null);
  novaPrevista = signal<Date | null>(null);
  motivoProrrogacao = signal('');

  abrirProrrogacao(e: Emprestimo) {
    this.novaPrevista.set(null);
    this.motivoProrrogacao.set('');
    this.prorrogando.set(e);
  }

  async prorrogar() {
    const e = this.prorrogando();
    const nova = this.novaPrevista();
    if (!e || !nova) return;
    await this.agir(firstValueFrom(this.#service.prorrogar(e.id, nova.toISOString(), this.motivoProrrogacao().trim() || null)));
    this.prorrogando.set(null);
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
