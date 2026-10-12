import { Component, OnInit, computed, input, signal } from '@angular/core';
import { AJUDA, ChaveAjuda } from '../ajuda-conteudo';

const PREFIXO = 'sgd.ajuda.';

/**
 * Faixa recolhível de ajuda no topo de cada tela, em perguntas e respostas curtas.
 * Abre sozinha na primeira visita e lembra, por navegador, se a pessoa recolheu.
 * O texto vem de ajuda-conteudo.ts.
 */
@Component({
  selector: 'app-ajuda',
  templateUrl: './ajuda.html',
  styleUrl: './ajuda.scss',
})
export class Ajuda implements OnInit {
  chave = input.required<ChaveAjuda>();

  conteudo = computed(() => AJUDA[this.chave()]);
  aberta = signal(true);

  ngOnInit() {
    this.aberta.set(lerPreferencia(this.chave()) !== 'fechada');
  }

  alternar(evento: Event) {
    const aberta = (evento.target as HTMLDetailsElement).open;
    this.aberta.set(aberta);
    gravarPreferencia(this.chave(), aberta ? 'aberta' : 'fechada');
  }
}

// localStorage pode lançar (janela privada, dados bloqueados): a ajuda funciona sem ele.
function lerPreferencia(chave: string): string | null {
  try {
    return localStorage.getItem(PREFIXO + chave);
  } catch {
    return null;
  }
}

function gravarPreferencia(chave: string, valor: string) {
  try {
    localStorage.setItem(PREFIXO + chave, valor);
  } catch {
    /* sem persistência, tudo bem */
  }
}
