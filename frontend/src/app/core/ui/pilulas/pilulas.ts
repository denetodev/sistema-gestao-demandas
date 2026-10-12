import { Component, input, model } from '@angular/core';

export interface OpcaoPilula {
  valor: string;
  rotulo: string;
  /** Complemento mais discreto ao lado do rótulo, por exemplo o nome da equipe em "Minha equipe · HTML". */
  detalhe?: string | null;
}

/**
 * Filtro em pílulas: uma escolha entre poucas opções, sempre à vista.
 * Usa botões de verdade (teclado e leitor de tela funcionam) dentro de um grupo de seleção única.
 */
@Component({
  selector: 'app-pilulas',
  templateUrl: './pilulas.html',
  styleUrl: './pilulas.scss',
})
export class Pilulas {
  opcoes = input.required<OpcaoPilula[]>();
  valor = model<string>('');
  /** O que o grupo escolhe, para leitores de tela. */
  descricao = input<string>('Filtro');

  escolher(valor: string) {
    if (valor !== this.valor()) this.valor.set(valor);
  }
}
