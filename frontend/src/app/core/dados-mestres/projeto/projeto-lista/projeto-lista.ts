import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { DemandanteService } from '../../demandante/demandante.service';
import { DiretoriaService } from '../../diretoria/diretoria.service';
import { StatusProjeto, Projeto } from '../projeto.model';
import { ProjetoService } from '../projeto.service';
import { AuthService } from '../../../auth/auth.service';
import { BarraPagina } from '../../../layout/barra-pagina/barra-pagina';

import { Ajuda } from '../../../ajuda/ajuda/ajuda';
import { CampoBusca } from '../../../ui/campo-busca/campo-busca';
import { OpcaoPilula, Pilulas } from '../../../ui/pilulas/pilulas';
import { contemTexto, maisRecentesPrimeiro } from '../../../util/listas';

@Component({
  selector: 'app-projeto-lista',
  imports: [FormsModule, CampoBusca, Pilulas, Ajuda, 
    ReactiveFormsModule,
    TableModule,
    Dialog,
    InputText,
    Textarea,
    Select,
    Button,
    Message,
    Tag,
    BarraPagina,
  ],
  templateUrl: './projeto-lista.html',
  styleUrl: './projeto-lista.scss',
})
export class ProjetoLista {
  #fb = inject(FormBuilder);
  #service = inject(ProjetoService);

  projetos = this.#service.listar;
  demandantes = inject(DemandanteService).listar;
  diretorias = inject(DiretoriaService).listar;

  opcoesStatus = [
    { label: 'Ativo', value: 'ATIVO' },
    { label: 'Pausado', value: 'PAUSADO' },
    { label: 'Encerrado', value: 'ENCERRADO' },
  ];

  // filtros da lista: os últimos cadastrados primeiro, 10 por página
  busca = signal('');
  situacaoFiltro = signal('');
  diretoriaFiltro = signal<string | null>(null);
  opcoesSituacao: OpcaoPilula[] = [{ valor: '', rotulo: 'Todos' }, ...[
    { valor: 'ATIVO', rotulo: 'Ativos' },
    { valor: 'PAUSADO', rotulo: 'Pausados' },
    { valor: 'ENCERRADO', rotulo: 'Encerrados' },
  ]];
  opcoesDiretoria = computed(() => (this.diretorias.value() ?? []).map((d) => ({ valor: d.id, rotulo: d.nome })));

  filtrados = computed(() =>
    maisRecentesPrimeiro(this.projetos.value() ?? []).filter(
      (p) =>
        contemTexto([p.nome, p.demandanteNome, p.descricao], this.busca()) &&
        (!this.situacaoFiltro() || p.status === this.situacaoFiltro()) &&
        (!this.diretoriaFiltro() || p.diretoriaId === this.diretoriaFiltro()),
    ),
  );

  filtrosAtivos = computed(() => !!(this.busca() || this.situacaoFiltro() || this.diretoriaFiltro()));

  limparFiltros() {
    this.busca.set('');
    this.situacaoFiltro.set('');
    this.diretoriaFiltro.set(null);
  }

  rotuloStatus(status: StatusProjeto): string {
    return this.opcoesStatus.find((o) => o.value === status)?.label ?? status;
  }

  dialogAberto = signal(false);
  editandoId = signal<string | null>(null);
  salvando = signal(false);
  erro = signal<string | null>(null);

  form = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    demandanteId: this.#fb.control<string | null>(null),
    diretoriaId: this.#fb.control<string | null>(null),
    descricao: [''],
    status: ['ATIVO' as StatusProjeto],
  });
  auth = inject(AuthService);

  severityStatus(status: StatusProjeto) {
    switch (status) {
      case 'ATIVO':
        return 'success';
      case 'PAUSADO':
        return 'warn';
      default:
        return 'secondary';
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
      demandanteId: projeto.demandanteId,
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
      await firstValueFrom(
        id ? this.#service.atualizar(id, payload) : this.#service.criar(payload),
      );
      this.dialogAberto.set(false);
      this.projetos.reload();
    } catch {
      this.erro.set('Não foi possível salvar o projeto.');
    } finally {
      this.salvando.set(false);
    }
  }
}
