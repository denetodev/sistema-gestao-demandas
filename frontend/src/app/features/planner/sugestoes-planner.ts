import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { ConfirmationService, MessageService } from 'primeng/api';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { BarraPagina } from '../../core/layout/barra-pagina/barra-pagina';
import { PlannerService, ResultadoSincronizacao, SugestaoPlanner } from '../../core/planner/planner.service';
import { separarCodigo } from '../demandas/data-access/demanda-apresentacao';
import { Carregando } from '../../core/ui/carregando/carregando';

/**
 * As tarefas do Planner que marcaram a etiqueta da pessoa. Aceitar faz a tarefa virar demanda do sistema (ou coloca a
 * pessoa na demanda que já existe com o mesmo código); recusar descarta e não volta a aparecer.
 */
@Component({
  selector: 'app-sugestoes-planner',
  imports: [Carregando, DatePipe, Button, Tag, BarraPagina],
  templateUrl: './sugestoes-planner.html',
  styleUrl: './sugestoes-planner.scss',
})
export class SugestoesPlanner {
  #service = inject(PlannerService);
  #router = inject(Router);
  #confirmacao = inject(ConfirmationService);
  #mensagens = inject(MessageService);

  auth = inject(AuthService);
  sugestoes = this.#service.sugestoes;

  /** Admin, Gestor e Referência de equipe enviam a planilha (mesma regra de projetos e campanhas; o backend confere). */
  podeAtualizar = computed(() => this.auth.podeGerenciarProjetosECampanhas());
  /** Id da sugestão em andamento, para travar só os botões dela. */
  ocupada = signal<string | null>(null);
  enviando = signal(false);
  resultado = signal<ResultadoSincronizacao | null>(null);

  apresentar(s: SugestaoPlanner) {
    return separarCodigo(s.titulo, s.codigo);
  }

  async aceitar(s: SugestaoPlanner) {
    this.ocupada.set(s.id);
    try {
      const { demandaId } = await firstValueFrom(this.#service.aceitar(s.id));
      this.#mensagens.add({
        severity: 'success',
        summary: s.demandaExistenteId ? 'Você entrou na demanda' : 'Demanda criada',
        life: 4000,
      });
      this.#router.navigate(['/demandas', demandaId]);
    } catch {
      // o aviso do interceptor já explicou o motivo; a lista é atualizada para refletir o que mudou
      this.sugestoes.reload();
    } finally {
      this.ocupada.set(null);
    }
  }

  confirmarRecusa(s: SugestaoPlanner) {
    this.#confirmacao.confirm({
      header: 'Não é minha',
      message: `Descartar "${s.titulo}"? Ela não volta a aparecer para você.`,
      icon: 'pi pi-question-circle',
      acceptLabel: 'Descartar',
      rejectLabel: 'Voltar',
      accept: () => this.#recusar(s),
    });
  }

  async #recusar(s: SugestaoPlanner) {
    this.ocupada.set(s.id);
    try {
      await firstValueFrom(this.#service.recusar(s.id));
      this.sugestoes.reload();
    } catch {
      this.sugestoes.reload();
    } finally {
      this.ocupada.set(null);
    }
  }

  /** Lê o arquivo escolhido (o .xlsx do Exportar plano para o Excel) e mostra o que o sistema achou nele. */
  async atualizarPeloPlanner(evento: Event) {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.[0];
    campo.value = ''; // permite escolher o mesmo arquivo de novo depois
    if (!arquivo) return;
    this.enviando.set(true);
    try {
      this.resultado.set(await firstValueFrom(this.#service.importar(arquivo)));
      this.sugestoes.reload();
    } catch {
      // o aviso do interceptor já explicou o motivo (arquivo errado, sem permissão...)
      this.resultado.set(null);
    } finally {
      this.enviando.set(false);
    }
  }
}
