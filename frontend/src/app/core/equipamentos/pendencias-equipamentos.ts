import { Injectable, computed, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';
import type { ResumoEmprestimos } from './equipamento.model';

/**
 * Aviso do menu: quantos pedidos esperam autorização e quantos empréstimos estão atrasados.
 * Serviço à parte (e não o EquipamentoService) para só chamar o backend quem tem acesso ao módulo.
 */
@Injectable({ providedIn: 'root' })
export class PendenciasEquipamentos {
  #auth = inject(AuthService);

  resumo = httpResource<ResumoEmprestimos>(() =>
    this.#auth.acessaEquipamentos() ? `${environment.apiUrl}/emprestimos/resumo` : undefined,
  );

  /** Gestor vê pedidos esperando mais os atrasados; o Operador, só os atrasados. */
  total = computed(() => {
    const r = this.resumo.value();
    if (!r) return 0;
    return (this.#auth.gerenciaEquipamentos() ? r.aguardandoAutorizacao : 0) + r.atrasados;
  });

  /** Atrasado, ou externo esperando decisão de quem autoriza: pinta o aviso de vermelho. */
  urgente = computed(() => {
    const r = this.resumo.value();
    return !!r && (r.atrasados > 0 || (this.#auth.gerenciaEquipamentos() && r.aguardandoExterno > 0));
  });

  constructor() {
    // as pendências mudam sem a pessoa fazer nada (outro operador registra, o prazo vence): confere a cada 2 minutos
    interval(120_000)
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (this.#auth.acessaEquipamentos()) this.resumo.reload();
      });
  }
}
