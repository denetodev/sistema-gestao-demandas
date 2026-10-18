import { Component, OnInit, computed, input, signal } from '@angular/core';
import { AJUDA, ChaveAjuda } from '../ajuda-conteudo';

const PREFIXO = 'sgd.ajuda.';

/** Telas cuja ajuda já foi mostrada nesta sessão (vale quando o navegador não deixa gravar no localStorage). */
const vistasNaSessao = new Set<string>();

/**
 * Faixa de ajuda no topo de cada tela, em perguntas e respostas curtas.
 * Na primeira vez que a pessoa abre a tela ela aparece aberta e destacada em laranja; nas outras vezes
 * fica recolhida, e a pessoa abre quando quiser. Recolher ou abrir não é lembrado: toda entrada na tela recomeça recolhida.
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
  aberta = signal(false);
  /** Só na primeira visita: fundo laranja para chamar a atenção. */
  destaque = signal(false);

  ngOnInit() {
    const primeiraVisita = !jaFoiVista(this.chave());
    this.aberta.set(primeiraVisita);
    this.destaque.set(primeiraVisita);
    if (primeiraVisita) marcarComoVista(this.chave());
  }

  alternar(evento: Event) {
    const aberta = (evento.target as HTMLDetailsElement).open;
    this.aberta.set(aberta);
    // fechou: o destaque cumpriu o papel
    if (!aberta) this.destaque.set(false);
  }
}

// localStorage pode lançar (janela privada, dados bloqueados): a ajuda funciona sem ele.
function jaFoiVista(chave: string): boolean {
  if (vistasNaSessao.has(chave)) return true;
  try {
    return localStorage.getItem(PREFIXO + chave) !== null;
  } catch {
    return false;
  }
}

function marcarComoVista(chave: string) {
  vistasNaSessao.add(chave);
  try {
    localStorage.setItem(PREFIXO + chave, 'vista');
  } catch {
    /* sem persistência: vale só nesta sessão */
  }
}

/** Para os testes: esquece o que já foi visto na sessão. */
export function esquecerAjudasVistas() {
  vistasNaSessao.clear();
}
