import { Component, computed, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { AuthService } from '../../../core/auth/auth.service';

export type SituacaoConta = 'pendente' | 'rejeitada' | 'erro';

/** Tela cheia mostrada no lugar do app para quem está logado mas não pode usá-lo ainda. */
@Component({
  selector: 'app-conta-pendente',
  imports: [Button],
  templateUrl: './conta-pendente.html',
  styleUrl: './conta-pendente.scss',
})
export class ContaPendente {
  situacao = input.required<SituacaoConta>();

  #auth = inject(AuthService);
  #router = inject(Router);

  email = this.#auth.emailSessao;
  carregando = computed(() => this.#auth.me.isLoading());

  verificarNovamente() {
    this.#auth.me.reload();
  }

  async sair() {
    await this.#auth.logout();
    this.#router.navigateByUrl('/login');
  }
}
