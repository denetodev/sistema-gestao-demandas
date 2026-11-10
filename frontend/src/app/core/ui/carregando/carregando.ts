import { Component, input } from '@angular/core';
import { ProgressBar } from 'primeng/progressbar';

/**
 * Barra de carregamento indeterminada, usada em todo lugar que espera dados.
 * Reserva sempre os 6px de altura, assim a página não pula quando a barra aparece ou some.
 */
@Component({
  selector: 'app-carregando',
  imports: [ProgressBar],
  template: `
    <div class="carregando" [attr.role]="ativo() ? 'status' : null" [attr.aria-label]="ativo() ? rotulo() : null">
      @if (ativo()) {
        <p-progressbar mode="indeterminate" [style]="{ height: '6px' }" />
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
    .carregando {
      height: 6px;
    }
  `,
})
export class Carregando {
  ativo = input(false);
  rotulo = input('Carregando');
}
