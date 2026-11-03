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

/**
 * As tarefas do Planner que marcaram a etiqueta da pessoa. Aceitar faz a tarefa virar demanda do sistema (ou coloca a
 * pessoa na demanda que já existe com o mesmo código); recusar descarta e não volta a aparecer.
 */
@Component({
  selector: 'app-sugestoes-planner',
  imports: [DatePipe, Button, Tag, BarraPagina],
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

  ehGestao = computed(() => this.auth.temPerfil('ADMIN', 'GESTOR'));
  /** Id da sugestão em andamento, para travar só os botões dela. */
  ocupada = signal<string | null>(null);
  buscando = signal(false);
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

  async buscarAgora() {
    this.buscando.set(true);
    try {
      this.resultado.set(await firstValueFrom(this.#service.sincronizar()));
      this.sugestoes.reload();
    } catch {
      this.resultado.set(null);
    } finally {
      this.buscando.set(false);
    }
  }
}
