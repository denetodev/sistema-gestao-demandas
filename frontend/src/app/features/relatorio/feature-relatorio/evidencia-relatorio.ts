import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DemandaItensService } from '../../demandas/data-access/demanda-itens.service';
import type { ItemEvidencia } from '../data-access/relatorio.model';

/**
 * Uma evidência dentro do relatório: link vira endereço clicável, imagem aparece em miniatura
 * (o endereço temporário do arquivo é pedido só quando a evidência entra na tela) e os outros
 * arquivos abrem numa aba.
 */
@Component({
  selector: 'app-evidencia-relatorio',
  template: `
    @let e = item();
    @if (e.tipo === 'LINK' && e.link) {
      <a class="ev ev--link" [href]="e.link" target="_blank" rel="noopener noreferrer">
        <i class="pi pi-link"></i>
        <span>{{ e.descricao || e.link }}</span>
      </a>
    } @else if (ehImagem()) {
      <figure class="ev ev--imagem">
        @if (url(); as u) {
          <a [href]="u" target="_blank" rel="noopener noreferrer"><img [src]="u" [alt]="e.descricao || 'Evidência'" loading="lazy" /></a>
        } @else if (falhou()) {
          <div class="ev__vazio"><i class="pi pi-image"></i> Não foi possível carregar</div>
        } @else {
          <div class="ev__vazio"><i class="pi pi-spin pi-spinner"></i></div>
        }
        @if (e.descricao) { <figcaption>{{ e.descricao }}</figcaption> }
      </figure>
    } @else if (e.temArquivo) {
      <button type="button" class="ev ev--arquivo" (click)="abrir()">
        <i class="pi pi-file"></i>
        <span>{{ e.descricao || 'Abrir arquivo' }}</span>
      </button>
    } @else {
      <span class="ev ev--texto"><i class="pi pi-align-left"></i> {{ e.descricao || e.tipo }}</span>
    }
  `,
  styles: `
    :host { display: contents; }
    .ev {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      max-width: 100%;
      padding: 0.3rem 0.65rem;
      border: 1px solid #d9dde6;
      border-radius: 999px;
      background: #f3f4f8;
      color: #111;
      font: inherit;
      font-size: 0.82rem;
      text-decoration: none;
      i { font-size: 0.78rem; color: var(--sgd-laranja); }
      span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    }
    a.ev:hover, button.ev:hover { border-color: var(--sgd-laranja); background: #fff1e6; cursor: pointer; }
    /* a imagem aparece inteira, na proporção dela, sem cortar nem rolar dentro de uma janelinha */
    .ev--imagem {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 0.35rem;
      width: auto;
      max-width: 100%;
      margin: 0;
      padding: 0.4rem;
      border-radius: 0.7rem;
      break-inside: avoid;
      img { display: block; width: auto; max-width: 100%; height: auto; border-radius: 0.4rem; background: #fff; }
      figcaption { font-size: 0.75rem; color: #5b6478; line-height: 1.3; }
    }
    .ev__vazio { display: grid; place-items: center; min-height: 5rem; min-width: 12rem; border-radius: 0.5rem; background: #fff; color: #5b6478; font-size: 0.78rem; gap: 0.3rem; }
  `,
})
export class EvidenciaRelatorio {
  item = input.required<ItemEvidencia>();
  #service = inject(DemandaItensService);

  url = signal<string | null>(null);
  falhou = signal(false);

  ehImagem() {
    const e = this.item();
    return e.temArquivo && (e.arquivoMime?.startsWith('image/') ?? ['IMAGEM', 'SCREENSHOT', 'MOCKUP', 'CAPTURA_SISTEMA'].includes(e.tipo));
  }

  constructor() {
    effect(() => {
      const e = this.item();
      if (!this.ehImagem()) return;
      untracked(async () => {
        try {
          this.url.set((await firstValueFrom(this.#service.urlDoArquivo(e.id))).url);
        } catch {
          this.falhou.set(true);
        }
      });
    });
  }

  async abrir() {
    const aba = window.open('', '_blank');
    try {
      const { url } = await firstValueFrom(this.#service.urlDoArquivo(this.item().id));
      if (aba) {
        aba.opener = null;
        aba.location.href = url;
      }
    } catch {
      aba?.close();
    }
  }
}
