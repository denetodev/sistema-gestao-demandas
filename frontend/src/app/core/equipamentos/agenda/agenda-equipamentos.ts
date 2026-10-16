import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { environment } from '../../../../environments/environment';
import { BarraPagina } from '../../layout/barra-pagina/barra-pagina';
import { OpcaoPilula, Pilulas } from '../../ui/pilulas/pilulas';
import { CampoBusca } from '../../ui/campo-busca/campo-busca';
import { Ajuda } from '../../ajuda/ajuda/ajuda';
import { contemTexto } from '../../util/listas';
import { AuthService } from '../../auth/auth.service';
import { classeBloco, rotuloBloco } from '../equipamento-apresentacao';
import { dias, inicioDoDia, posicaoAgora, posicionar, somarDias } from '../agenda-layout';
import type { Agenda, BlocoAgenda, LinhaAgenda } from '../equipamento.model';

/**
 * Agenda dos equipamentos (Gantt): uma linha por equipamento, uma barra por período de uso. Mostra com quem está
 * agora, as reservas dos próximos dias e o que foi devolvido há pouco. Clicar numa barra ou no nome abre os detalhes.
 */
@Component({
  selector: 'app-agenda-equipamentos',
  imports: [DatePipe, FormsModule, RouterLink, Dialog, Select, Button, Tag, BarraPagina, Pilulas, CampoBusca, Ajuda],
  templateUrl: './agenda-equipamentos.html',
  styleUrl: './agenda-equipamentos.scss',
})
export class AgendaEquipamentos {
  auth = inject(AuthService);

  classeBloco = classeBloco;
  rotuloBloco = rotuloBloco;

  opcoesDias: OpcaoPilula[] = [
    { valor: '7', rotulo: '7 dias' },
    { valor: '14', rotulo: '14 dias' },
    { valor: '30', rotulo: '30 dias' },
  ];

  quantidadeDias = signal(14);
  /** Começa dois dias antes de hoje: dá contexto do que acabou de acontecer e muito mais espaço para o que vem. */
  inicio = signal(inicioDoDia(somarDias(new Date(), -2)));
  fim = computed(() => somarDias(this.inicio(), this.quantidadeDias()));
  /** Último dia mostrado, para o título do período (o fim da janela é meia-noite do dia seguinte). */
  ultimoDia = computed(() => somarDias(this.fim(), -1));

  agenda = httpResource<Agenda>(() => ({
    url: `${environment.apiUrl}/equipamentos/agenda`,
    params: { de: this.inicio().toISOString(), ate: this.fim().toISOString() },
  }));

  busca = signal('');
  categoriaId = signal<string | null>(null);

  agoraData = computed(() => {
    const a = this.agenda.value();
    return a ? new Date(a.agora) : new Date();
  });

  colunas = computed(() => dias(this.inicio(), this.quantidadeDias(), this.agoraData()));
  marcadorAgora = computed(() => posicaoAgora(this.inicio(), this.fim(), this.agoraData()));

  categorias = computed(() => {
    const mapa = new Map<string, string>();
    for (const l of this.agenda.value()?.equipamentos ?? []) mapa.set(l.categoriaId, l.categoriaNome);
    return [...mapa].map(([id, nome]) => ({ id, nome }));
  });

  /** Equipamentos filtrados, com as barras já posicionadas, agrupados por categoria. */
  grupos = computed(() => {
    const filtradas = (this.agenda.value()?.equipamentos ?? []).filter(
      (l) =>
        (!this.categoriaId() || l.categoriaId === this.categoriaId()) &&
        contemTexto([l.nome, l.patrimonio, l.categoriaNome], this.busca()),
    );
    const mapa = new Map<string, { categoria: string; linhas: { linha: LinhaAgenda; itens: ReturnType<typeof posicionar>['itens']; faixas: number }[] }>();
    for (const linha of filtradas) {
      const { itens, faixas } = posicionar(linha.blocos, this.inicio(), this.fim(), this.agoraData());
      if (!mapa.has(linha.categoriaId)) mapa.set(linha.categoriaId, { categoria: linha.categoriaNome, linhas: [] });
      mapa.get(linha.categoriaId)!.linhas.push({ linha, itens, faixas });
    }
    return [...mapa.values()];
  });

  filtrosAtivos = computed(() => !!(this.busca() || this.categoriaId()));

  // ---- navegação ----
  mudarDias(valor: string) {
    this.quantidadeDias.set(Number(valor));
  }

  mover(sentido: -1 | 1) {
    // anda meia janela por vez, para sobrar contexto do que já estava na tela
    this.inicio.update((d) => somarDias(d, sentido * Math.max(1, Math.floor(this.quantidadeDias() / 2))));
  }

  hoje() {
    this.inicio.set(inicioDoDia(somarDias(new Date(), -2)));
  }

  limparFiltros() {
    this.busca.set('');
    this.categoriaId.set(null);
  }

  // ---- detalhes ----
  blocoAberto = signal<{ linha: LinhaAgenda; bloco: BlocoAgenda } | null>(null);
  equipamentoAberto = signal<LinhaAgenda | null>(null);

  abrirBloco(linha: LinhaAgenda, bloco: BlocoAgenda) {
    this.blocoAberto.set({ linha, bloco });
  }

  abrirEquipamento(linha: LinhaAgenda) {
    this.equipamentoAberto.set(linha);
  }

  /** Com quem está agora (retirado e não devolvido), depois as reservas, depois o que já voltou. */
  usosDoEquipamento = computed(() => {
    const l = this.equipamentoAberto();
    if (!l) return { agora: [], reservas: [], devolvidos: [] };
    return {
      agora: l.blocos.filter((b) => !b.devolvido && b.status === 'RETIRADO'),
      reservas: l.blocos.filter((b) => !b.devolvido && b.status !== 'RETIRADO'),
      devolvidos: l.blocos.filter((b) => b.devolvido),
    };
  });

  tituloBloco(b: BlocoAgenda): string {
    const f = (iso: string) => new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
    return `${b.retiranteNome} · ${f(b.inicio)} até ${f(b.fim)} · ${rotuloBloco(b)}`;
  }
}
