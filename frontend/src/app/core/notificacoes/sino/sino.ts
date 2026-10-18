import { Component, DestroyRef, computed, inject, viewChild } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Popover } from 'primeng/popover';
import { environment } from '../../../../environments/environment';

export type TipoNotificacao = 'CADASTROS_PENDENTES' | 'EMPRESTIMOS_AGUARDANDO' | 'EMPRESTIMOS_ATRASADOS' | 'ADICIONADO_EM_DEMANDA';

export interface Notificacao {
  id: string;
  tipo: TipoNotificacao;
  titulo: string;
  texto: string;
  link: string;
  quantidade: number;
  lida: boolean;
  em: string | null;
}

export interface NotificacoesResposta {
  itens: Notificacao[];
  naoLidas: number;
}

const ICONE: Record<TipoNotificacao, string> = {
  CADASTROS_PENDENTES: 'pi-user-plus',
  EMPRESTIMOS_AGUARDANDO: 'pi-inbox',
  EMPRESTIMOS_ATRASADOS: 'pi-exclamation-triangle',
  ADICIONADO_EM_DEMANDA: 'pi-briefcase',
};

/** De quanto em quanto tempo o sininho pergunta se há aviso novo (e também ao voltar para a aba). */
export const INTERVALO_DO_SININHO_MS = 60_000;

/** O sininho do topo, com a quantidade de avisos que a pessoa ainda não viu e a lista deles. */
@Component({
  selector: 'app-sino',
  imports: [Popover, DatePipe],
  templateUrl: './sino.html',
  styleUrl: './sino.scss',
})
export class Sino {
  #http = inject(HttpClient);
  #router = inject(Router);

  popover = viewChild.required(Popover);

  avisos = httpResource<NotificacoesResposta>(() => `${environment.apiUrl}/notificacoes`);

  itens = computed(() => this.avisos.value()?.itens ?? []);
  naoLidas = computed(() => this.avisos.value()?.naoLidas ?? 0);
  /** O número no selo; acima de 9 vira "9+". */
  selo = computed(() => (this.naoLidas() > 9 ? '9+' : String(this.naoLidas())));

  constructor() {
    const atualizar = () => {
      if (document.visibilityState === 'visible') this.avisos.reload();
    };
    const intervalo = setInterval(atualizar, INTERVALO_DO_SININHO_MS);
    document.addEventListener('visibilitychange', atualizar);
    inject(DestroyRef).onDestroy(() => {
      clearInterval(intervalo);
      document.removeEventListener('visibilitychange', atualizar);
    });
  }

  icone(tipo: TipoNotificacao): string {
    return ICONE[tipo];
  }

  alternar(evento: Event) {
    this.popover().toggle(evento);
  }

  /** Ao abrir, avisa o servidor que os "te adicionaram" foram vistos; na tela continuam destacados até fechar. */
  async aoAbrir() {
    if (!this.itens().some((i) => i.tipo === 'ADICIONADO_EM_DEMANDA' && !i.lida)) return;
    try {
      await firstValueFrom(this.#http.post<void>(`${environment.apiUrl}/notificacoes/vistas`, {}));
    } catch {
      /* sem conexão: o aviso continua não lido e tenta de novo na próxima abertura */
    }
  }

  aoFechar() {
    this.avisos.reload();
  }

  abrir(item: Notificacao) {
    this.popover().hide();
    void this.#router.navigateByUrl(item.link);
  }
}
