import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmationService, MessageService } from 'primeng/api';
import { PessoaService } from '../../../core/pessoas/pessoa.service';
import { AreaService } from '../../../core/dados-mestres/area/area.service';
import { CargoService } from '../../../core/dados-mestres/cargo/cargo.service';
import { AuthService } from '../../../core/auth/auth.service';
import { BarraPagina } from '../../../core/layout/barra-pagina/barra-pagina';
import { cpfValidator, formatarCpf } from '../../../core/util/cpf';
import { Ajuda } from '../../ajuda/ajuda/ajuda';
import { candidatosAVinculo } from '../candidatos-vinculo';
import type { Pessoa, StatusPessoa } from '../../../core/pessoas/pessoa.model';
import type { Perfil } from '../../../core/auth/auth.model';
import { CampoBusca } from '../../ui/campo-busca/campo-busca';
import { OpcaoPilula, Pilulas } from '../../ui/pilulas/pilulas';
import { contemTexto } from '../../util/listas';
import { DiretoriaService } from '../../dados-mestres/diretoria/diretoria.service';


@Component({
  selector: 'app-pessoa-lista',
  imports: [CampoBusca, Pilulas, 
    Ajuda, ReactiveFormsModule, TableModule, Dialog, InputText,
    Select, Button, Message, Tag, Tooltip, BarraPagina, DatePipe, FormsModule
  ],
  templateUrl: './pessoa-lista.html',
  styleUrl: './pessoa-lista.scss',
})
export class PessoaLista {
  #fb = inject(FormBuilder);
  #service = inject(PessoaService);
  #confirmationService = inject(ConfirmationService);
  #messageService = inject(MessageService);
  #cargoService = inject(CargoService);

  auth = inject(AuthService);
  pessoas = this.#service.listar;
  areas = inject(AreaService).listar;
  cargos = computed(() => (this.#cargoService.listar.value() ?? []).filter(c => c.ativo));
  dialogAprovacao = signal(false);
  dialogEdicao = signal(false);

  apenasPendentes = this.#service.apenasPendentes;

  abas: OpcaoPilula[] = [
    { valor: 'todas', rotulo: 'Todas' },
    { valor: 'pendentes', rotulo: 'Aguardando aprovação' },
  ];
  aba = computed(() => (this.apenasPendentes() ? 'pendentes' : 'todas'));

  mudarAba(valor: string) {
    this.apenasPendentes.set(valor === 'pendentes');
  }

  // ---- filtros da lista (a busca e os selects estreitam o que veio do servidor) ----
  #diretorias = inject(DiretoriaService).listar;
  busca = signal('');
  nucleoFiltro = signal<string | null>(null);
  equipeFiltro = signal<string | null>(null);
  perfilFiltro = signal<string | null>(null);
  situacaoFiltro = signal<string | null>(null);

  opcoesNucleo = computed(() => (this.#diretorias.value() ?? []).map((d) => ({ valor: d.id, rotulo: d.nome })));
  /** O mesmo nome de equipe existe em diretorias diferentes (Design em CRM e em UGR), então a diretoria vai junto. */
  opcoesEquipe = computed(() =>
    (this.areas.value() ?? []).map((a) => ({
      valor: a.id,
      rotulo: a.diretoriaNome ? `${a.nome} · ${a.diretoriaNome}` : a.nome,
    })),
  );
  opcoesPerfilFiltro = [
    { label: 'Visualizador', value: 'VISUALIZADOR' },
    { label: 'Profissional', value: 'PROFISSIONAL' },
    { label: 'Gestor', value: 'GESTOR' },
    { label: 'Admin', value: 'ADMIN' },
  ];
  opcoesSituacao = [
    { label: 'Ativo', value: 'ATIVO' },
    { label: 'Inativo', value: 'INATIVO' },
    { label: 'Afastado', value: 'AFASTADO' },
    { label: 'Aguardando aprovação', value: 'AGUARDANDO' },
    { label: 'Rejeitado', value: 'REJEITADO' },
  ];

  filtrados = computed(() =>
    [...(this.pessoas.value() ?? [])]
      .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
      .filter((p) => {
        const aguardando = p.aprovadoEm === null && p.status !== 'REJEITADO';
        const situacao = this.situacaoFiltro();
        return (
          contemTexto([p.nome, p.nomeExibicao, p.email], this.busca()) &&
          (!this.nucleoFiltro() || p.diretoriaId === this.nucleoFiltro()) &&
          (!this.equipeFiltro() || p.areaId === this.equipeFiltro()) &&
          (!this.perfilFiltro() || p.perfil === this.perfilFiltro()) &&
          (!situacao || (situacao === 'AGUARDANDO' ? aguardando : !aguardando && p.status === situacao))
        );
      }),
  );

  filtrosAtivos = computed(
    () => !!(this.busca() || this.nucleoFiltro() || this.equipeFiltro() || this.perfilFiltro() || this.situacaoFiltro()),
  );

  limparFiltros() {
    this.busca.set('');
    this.nucleoFiltro.set(null);
    this.equipeFiltro.set(null);
    this.perfilFiltro.set(null);
    this.situacaoFiltro.set(null);
  }

  rotuloPerfil(perfil: string): string {
    return this.opcoesPerfilFiltro.find((o) => o.value === perfil)?.label ?? perfil;
  }

  rotuloSituacao(status: string): string {
    return this.opcoesSituacao.find((o) => o.value === status)?.label ?? status;
  }

  opcoesPerfil = [
    { label: 'Visualizador', value: 'VISUALIZADOR' },
    { label: 'Profissional', value: 'PROFISSIONAL' },
    { label: 'Gestor', value: 'GESTOR' },
    { label: 'Admin', value: 'ADMIN' },
  ];

  /** Gestor não concede ADMIN nem mexe em quem já é ADMIN (regra do backend). */
  perfisDisponiveis = computed(() =>
    this.auth.temPerfil('ADMIN')
      ? this.opcoesPerfil
      : this.opcoesPerfil.filter(o => o.value !== 'ADMIN'),
  );

  podeGerenciar(pessoa: Pessoa): boolean {
    if (this.auth.temPerfil('ADMIN')) return true;
    return this.auth.temPerfil('GESTOR') && pessoa.perfil !== 'ADMIN';
  }

  pessoaSelecionada = signal<Pessoa | null>(null);
  dialogAberto = signal(false);
  salvando = signal(false);
  erro = signal<string | null>(null);

  pendente = computed(() => this.pessoaSelecionada()?.aprovadoEm === null);

  // vínculo da conta pendente a um cadastro que já existe (ex.: importado da planilha)
  vinculoId = signal<string | null>(null);
  candidatos = computed(() => candidatosAVinculo(this.#service.todas.value() ?? []));

  confirmarVinculo() {
    const pessoa = this.pessoaSelecionada();
    const destino = this.candidatos().find((c) => c.id === this.vinculoId());
    if (!pessoa || !destino) return;
    this.#confirmationService.confirm({
      header: 'Vincular conta ao cadastro existente',
      message: `A conta de ${pessoa.nome} (${pessoa.email ?? 'sem e-mail'}) passa a ser de "${destino.label}", que mantém todo o histórico. Este cadastro novo será removido e isso não pode ser desfeito.`,
      icon: 'pi pi-link',
      acceptLabel: 'Vincular',
      rejectLabel: 'Voltar',
      accept: () => this.#vincular(pessoa, destino.id),
    });
  }

  async #vincular(pessoa: Pessoa, existenteId: string) {
    this.salvando.set(true);
    this.erro.set(null);
    try {
      await firstValueFrom(this.#service.vincularAPessoaExistente(pessoa.id, existenteId));
      this.dialogAprovacao.set(false);
      this.vinculoId.set(null);
      this.pessoas.reload();
      this.#service.todas.reload();
      this.#messageService.add({ severity: 'success', summary: 'Conta vinculada ao cadastro existente', life: 4000 });
    } catch (e) {
      this.erro.set((e as { error?: { mensagem?: string } }).error?.mensagem ?? 'Não foi possível vincular a conta.');
    } finally {
      this.salvando.set(false);
    }
  }

  form = this.#fb.nonNullable.group({
    perfil: this.#fb.control<Perfil | null>(null, Validators.required),
    areaId: this.#fb.control<string | null>(null),
    cargoId: this.#fb.control<string | null>(null),
    referenciaAreaId: this.#fb.control<string | null>(null),
  });

  abrir(pessoa: Pessoa) {
    this.pessoaSelecionada.set(pessoa);
    this.erro.set(null);
    this.vinculoId.set(null);
    if (pessoa.aprovadoEm === null) {
      this.form.setValue({
        perfil: 'PROFISSIONAL',
        areaId: pessoa.areaId,
        cargoId: pessoa.cargoId,
        referenciaAreaId: pessoa.referenciaAreaId,
      });
      this.dialogAprovacao.set(true);
    } else {
      this.formEdicao.setValue({
        nome: pessoa.nome,
        email: pessoa.email ?? '',
        cpf: '',
        areaId: pessoa.areaId,
        cargoId: pessoa.cargoId,
        status: pessoa.status,
        perfil: pessoa.perfil,
        referenciaAreaId: pessoa.referenciaAreaId,
      });
      this.dialogEdicao.set(true);
    }
  }

  async salvarEdicao() {
    const pessoa = this.pessoaSelecionada();
    if (!pessoa || this.formEdicao.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.formEdicao.getRawValue();
    try {
      await firstValueFrom(this.#service.atualizar(pessoa.id, {
        nome: v.nome,
        email: v.email || null,
        cpf: v.cpf || null,
        areaId: v.areaId!,
        cargoId: v.cargoId,
        status: v.status,
        perfil: v.perfil,
        referenciaAreaId: v.referenciaAreaId,
      }));
      this.dialogEdicao.set(false);
      this.pessoas.reload();
      this.#messageService.add({ severity: 'success', summary: 'Pessoa atualizada', life: 4000 });
    } catch {
      this.erro.set('Não foi possível salvar as alterações.');
    } finally {
      this.salvando.set(false);
    }
  }

  /** Marca o que o gestor alterou em relação ao que a pessoa informou. */
  ajustado(campo: 'areaId' | 'cargoId'): boolean {
    const original = this.pessoaSelecionada()?.[campo] ?? null;
    return this.form.controls[campo].value !== original;
  }

  async aprovar() {
    const pessoa = this.pessoaSelecionada();
    if (!pessoa || this.form.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    try {
      await firstValueFrom(this.#service.aprovar(pessoa.id, {
        perfil: v.perfil!,
        areaId: v.areaId,
        cargoId: v.cargoId,
        referenciaAreaId: v.referenciaAreaId,
      }));
      this.dialogAprovacao.set(false);
      this.pessoas.reload();
      this.#messageService.add({ severity: 'success', summary: 'Cadastro aprovado', life: 4000 });
    } catch {
      this.erro.set('Não foi possível aprovar o cadastro.');
    } finally {
      this.salvando.set(false);
    }
  }

  confirmarRejeicao() {
    const pessoa = this.pessoaSelecionada();
    if (!pessoa) return;
    this.#confirmationService.confirm({
      header: 'Rejeitar cadastro',
      message: `Rejeitar o cadastro de ${pessoa.nome}? Essa ação não pode ser desfeita: a pessoa não poderá ser aprovada depois.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Rejeitar',
      rejectLabel: 'Voltar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.#rejeitar(pessoa),
    });
  }

  async #rejeitar(pessoa: Pessoa) {
    this.salvando.set(true);
    try {
      await firstValueFrom(this.#service.rejeitar(pessoa.id, { motivo: null }));
      this.dialogAprovacao.set(false);
      this.pessoas.reload();
    } catch {
      this.erro.set('Não foi possível rejeitar o cadastro.');
    } finally {
      this.salvando.set(false);
    }
  }

  severityStatus(status: StatusPessoa) {
    switch (status) {
      case 'ATIVO': return 'success';
      case 'AFASTADO': return 'warn';
      case 'REJEITADO': return 'danger';
      default: return 'secondary';
    }
  }

  formEdicao = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    email: [''],
    cpf: ['', cpfValidator],
    areaId: this.#fb.control<string | null>(null, Validators.required),
    cargoId: this.#fb.control<string | null>(null),
    status: this.#fb.control<StatusPessoa | null>(null),
    perfil: this.#fb.control<Perfil | null>(null),
    referenciaAreaId: this.#fb.control<string | null>(null),
  });

  mascararCpf() {
    const c = this.formEdicao.controls.cpf;
    const formatado = formatarCpf(c.value);
    if (formatado !== c.value) c.setValue(formatado);
  }

  opcoesStatus = [
    { label: 'Ativo', value: 'ATIVO' },
    { label: 'Inativo', value: 'INATIVO' },
    { label: 'Afastado', value: 'AFASTADO' },
  ];
}