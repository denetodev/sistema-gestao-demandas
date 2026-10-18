import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { ConfirmationService } from 'primeng/api';
import { BarraPagina } from '../../layout/barra-pagina/barra-pagina';
import { Ajuda } from '../../ajuda/ajuda/ajuda';
import { CampoBusca } from '../../ui/campo-busca/campo-busca';
import { OpcaoPilula, Pilulas } from '../../ui/pilulas/pilulas';
import { contemTexto } from '../../util/listas';
import { AreaService, type Area } from '../area/area.service';
import { DiretoriaService, type Diretoria } from '../diretoria/diretoria.service';
import { NucleoService, type Nucleo } from '../nucleo/nucleo.service';

export interface NoNucleo {
  nucleo: Nucleo;
  equipes: Area[];
}

export interface NoDiretoria {
  diretoria: Diretoria;
  nucleos: NoNucleo[];
  totalEquipes: number;
}

type Janela = 'diretoria' | 'nucleo' | 'equipe';

/**
 * Diretoria, núcleo e equipe numa árvore só: cada nível tem o botão de criar o nível de baixo,
 * e dá para renomear, mover e desativar sem sair da tela.
 */
@Component({
  selector: 'app-estrutura',
  imports: [
    FormsModule, ReactiveFormsModule, Dialog, InputText, Select, Button, Message, BarraPagina, Ajuda, CampoBusca, Pilulas,
  ],
  templateUrl: './estrutura.html',
  styleUrl: './estrutura.scss',
})
export class Estrutura {
  #fb = inject(FormBuilder);
  #confirmacao = inject(ConfirmationService);
  #diretoriaService = inject(DiretoriaService);
  #nucleoService = inject(NucleoService);
  #areaService = inject(AreaService);

  diretorias = this.#diretoriaService.listar;
  nucleos = this.#nucleoService.listar;
  areas = this.#areaService.listar;

  carregando = computed(() => this.diretorias.isLoading() || this.nucleos.isLoading() || this.areas.isLoading());

  // ---- a árvore ----
  busca = signal('');
  filtro = signal<'com' | 'todas'>('com');
  abertas = signal<ReadonlySet<string>>(new Set());

  opcoesFiltro: OpcaoPilula[] = [
    { valor: 'com', rotulo: 'Com núcleos' },
    { valor: 'todas', rotulo: 'Todas as diretorias' },
  ];

  nucleosAtivos = computed(() => (this.nucleos.value() ?? []).filter((n) => n.ativo));
  equipesAtivas = computed(() => (this.areas.value() ?? []).filter((a) => a.ativo));
  equipesSemNucleo = computed(() => this.equipesAtivas().filter((a) => !a.nucleoId));

  arvore = computed<NoDiretoria[]>(() => {
    const nucleos = this.nucleosAtivos();
    const equipes = this.equipesAtivas();
    return (this.diretorias.value() ?? [])
      .filter((d) => d.ativo !== false)
      .map((diretoria) => {
        const nos = nucleos
          .filter((n) => n.diretoriaId === diretoria.id)
          .map((nucleo) => ({
            nucleo,
            equipes: equipes.filter((a) => a.nucleoId === nucleo.id).sort(porNome),
          }))
          .sort((a, b) => porNome(a.nucleo, b.nucleo));
        const soltas = equipes.filter((a) => a.diretoriaId === diretoria.id && !a.nucleoId).length;
        return { diretoria, nucleos: nos, totalEquipes: nos.reduce((s, n) => s + n.equipes.length, 0) + soltas };
      })
      .sort((a, b) => b.nucleos.length - a.nucleos.length || porNome(a.diretoria, b.diretoria));
  });

  visiveis = computed(() => {
    const texto = this.busca();
    const soComNucleos = this.filtro() === 'com' && !texto;
    return this.arvore().filter((no) => {
      if (soComNucleos && no.nucleos.length === 0) return false;
      if (!texto) return true;
      return (
        contemTexto([no.diretoria.nome, no.diretoria.sigla], texto) ||
        no.nucleos.some((n) => contemTexto([n.nucleo.nome, ...n.equipes.map((e) => e.nome)], texto))
      );
    });
  });

  semDiretoriasComNucleo = computed(() => !this.carregando() && this.arvore().every((no) => no.nucleos.length === 0));

  estaAberta(id: string): boolean {
    return this.abertas().has(id) || (!!this.busca() && this.visiveis().some((no) => no.diretoria.id === id));
  }

  alternar(id: string) {
    this.abertas.update((s) => {
      const novo = new Set(s);
      if (novo.has(id)) novo.delete(id);
      else novo.add(id);
      return novo;
    });
  }

  abrir(id: string) {
    this.abertas.update((s) => new Set(s).add(id));
  }

  // ---- janelas ----
  janela = signal<Janela | null>(null);
  editando = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  formDiretoria = this.#fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    sigla: ['', Validators.maxLength(20)],
  });
  formNucleo = this.#fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    diretoriaId: this.#fb.control<string | null>(null, Validators.required),
  });
  formEquipe = this.#fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    nucleoId: this.#fb.control<string | null>(null, Validators.required),
  });

  /** Opções de diretoria para o seletor da janela de núcleo. */
  opcoesDiretoria = computed(() =>
    [...(this.diretorias.value() ?? [])]
      .filter((d) => d.ativo !== false)
      .sort(porNome)
      .map((d) => ({ id: d.id, rotulo: d.sigla ? `${d.nome} (${d.sigla})` : d.nome })),
  );

  /** Núcleos agrupados por diretoria: o nome do núcleo sozinho não diz onde ele está. */
  opcoesNucleo = computed(() => {
    const restritaA = this.diretoriaDaEquipeEmEdicao();
    return this.arvore()
      .filter((no) => no.nucleos.length > 0 && (!restritaA || no.diretoria.id === restritaA))
      .map((no) => ({
        rotulo: no.diretoria.nome,
        itens: no.nucleos.map((n) => ({ id: n.nucleo.id, rotulo: n.nucleo.nome })),
      }));
  });

  /** Mudar uma equipe de diretoria mexeria nas pessoas dela: ao editar, só núcleos da mesma diretoria. */
  diretoriaDaEquipeEmEdicao = signal<string | null>(null);

  tituloJanela = computed(() => {
    const novo = !this.editando();
    switch (this.janela()) {
      case 'diretoria': return 'Nova diretoria';
      case 'nucleo': return novo ? 'Novo núcleo' : 'Editar núcleo';
      case 'equipe': return novo ? 'Nova equipe' : 'Editar equipe';
      default: return '';
    }
  });

  aberta = computed(() => this.janela() !== null);

  fecharJanela() {
    this.janela.set(null);
  }

  novaDiretoria() {
    this.#abrir('diretoria', null);
    this.formDiretoria.reset();
  }

  novoNucleo(diretoriaId: string | null = null) {
    this.#abrir('nucleo', null);
    this.formNucleo.reset({ nome: '', diretoriaId });
  }

  editarNucleo(n: Nucleo) {
    this.#abrir('nucleo', n.id);
    this.formNucleo.reset({ nome: n.nome, diretoriaId: n.diretoriaId });
  }

  novaEquipe(nucleoId: string | null = null) {
    this.diretoriaDaEquipeEmEdicao.set(null);
    this.#abrir('equipe', null);
    this.formEquipe.reset({ nome: '', nucleoId });
  }

  editarEquipe(a: Area) {
    this.diretoriaDaEquipeEmEdicao.set(a.diretoriaId);
    this.#abrir('equipe', a.id);
    this.formEquipe.reset({ nome: a.nome, nucleoId: a.nucleoId });
  }

  #abrir(janela: Janela, id: string | null) {
    this.editando.set(id);
    this.erro.set(null);
    if (id === null && janela !== 'equipe') this.diretoriaDaEquipeEmEdicao.set(null);
    this.janela.set(janela);
  }

  async salvar() {
    const janela = this.janela();
    if (!janela || this.#formAtual().invalid) {
      this.#formAtual().markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    const id = this.editando();
    try {
      if (janela === 'diretoria') {
        const v = this.formDiretoria.getRawValue();
        await firstValueFrom(this.#diretoriaService.criar({ nome: v.nome.trim(), sigla: v.sigla.trim().toUpperCase() || null }));
        this.diretorias.reload();
      } else if (janela === 'nucleo') {
        const v = this.formNucleo.getRawValue();
        const corpo = { nome: v.nome.trim(), diretoriaId: v.diretoriaId! };
        await firstValueFrom(id ? this.#nucleoService.atualizar(id, corpo) : this.#nucleoService.criar(corpo));
        this.abrir(corpo.diretoriaId);
        this.nucleos.reload();
        this.areas.reload();
      } else {
        const v = this.formEquipe.getRawValue();
        const nucleo = this.nucleosAtivos().find((n) => n.id === v.nucleoId);
        if (!nucleo) throw new Error('núcleo');
        const corpo = { nome: v.nome.trim(), diretoriaId: nucleo.diretoriaId, nucleoId: nucleo.id };
        await firstValueFrom(id ? this.#areaService.atualizar(id, corpo) : this.#areaService.criar(corpo));
        this.abrir(nucleo.diretoriaId);
        this.areas.reload();
      }
      this.fecharJanela();
    } catch (e) {
      const mensagem = (e as { error?: { mensagem?: string } })?.error?.mensagem;
      this.erro.set(mensagem ?? 'Não foi possível salvar. Tente de novo.');
    } finally {
      this.salvando.set(false);
    }
  }

  // ---- desativar ----
  desativarNucleo(no: NoNucleo) {
    this.#confirmacao.confirm({
      header: 'Desativar núcleo',
      message: no.equipes.length
        ? `O núcleo ${no.nucleo.nome} ainda tem ${no.equipes.length} equipe(s). Mude ou desative as equipes antes.`
        : `Desativar o núcleo ${no.nucleo.nome}? Ele some das listas, e o histórico continua.`,
      acceptLabel: no.equipes.length ? 'Entendi' : 'Desativar',
      rejectVisible: no.equipes.length === 0,
      accept: async () => {
        if (no.equipes.length) return;
        await this.#desativar(() => this.#nucleoService.desativar(no.nucleo.id), () => this.nucleos.reload());
      },
    });
  }

  desativarEquipe(a: Area) {
    this.#confirmacao.confirm({
      header: 'Desativar equipe',
      message: `Desativar a equipe ${a.nome}? Se ainda houver pessoas ativas nela, o sistema avisa e não deixa.`,
      acceptLabel: 'Desativar',
      accept: () => this.#desativar(() => this.#areaService.desativar(a.id), () => this.areas.reload()),
    });
  }

  async #desativar(chamada: () => import('rxjs').Observable<unknown>, recarregar: () => void) {
    try {
      await firstValueFrom(chamada());
      recarregar();
    } catch {
      // o aviso com o motivo (409) vem do interceptor
    }
  }

  #formAtual() {
    switch (this.janela()) {
      case 'diretoria': return this.formDiretoria;
      case 'nucleo': return this.formNucleo;
      default: return this.formEquipe;
    }
  }

  mostrarErro(form: 'diretoria' | 'nucleo' | 'equipe', campo: string): boolean {
    const c = (this[`form${form[0].toUpperCase()}${form.slice(1)}` as 'formNucleo'].controls as Record<string, { invalid: boolean; touched: boolean }>)[campo];
    return !!c && c.invalid && c.touched;
  }
}

function porNome(a: { nome: string }, b: { nome: string }): number {
  return a.nome.localeCompare(b.nome, 'pt-BR');
}
