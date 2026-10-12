import { Component, effect, input, model, OnDestroy, signal } from '@angular/core';

/**
 * Campo de busca que só avisa a tela depois que a pessoa para de digitar (padrão 300 ms),
 * para não pedir dados a cada tecla. Enter busca na hora; o X limpa.
 */
@Component({
  selector: 'app-campo-busca',
  templateUrl: './campo-busca.html',
  styleUrl: './campo-busca.scss',
})
export class CampoBusca implements OnDestroy {
  valor = model<string>('');
  placeholder = input<string>('Buscar...');
  descricao = input<string>('Buscar');
  atrasoMs = input<number>(300);

  /** O que está na caixa agora; `valor` só muda depois do atraso. */
  texto = signal('');
  #espera: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // mudança vinda de fora (por exemplo da URL) aparece na caixa
    effect(() => this.texto.set(this.valor()));
  }

  digitar(evento: Event) {
    const novo = (evento.target as HTMLInputElement).value;
    this.texto.set(novo);
    clearTimeout(this.#espera);
    this.#espera = setTimeout(() => this.valor.set(novo.trim()), this.atrasoMs());
  }

  buscarAgora() {
    clearTimeout(this.#espera);
    this.valor.set(this.texto().trim());
  }

  limpar() {
    clearTimeout(this.#espera);
    this.texto.set('');
    this.valor.set('');
  }

  ngOnDestroy() {
    clearTimeout(this.#espera);
  }
}
