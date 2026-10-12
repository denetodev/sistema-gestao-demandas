import { Component, input } from '@angular/core';

/** Cabeçalho da página: título, uma linha explicando para que serve a tela e as ações à direita. */
@Component({
  selector: 'app-barra-pagina',
  imports: [],
  templateUrl: './barra-pagina.html',
  styleUrl: './barra-pagina.scss',
})
export class BarraPagina {
  titulo = input<string>();
  subtitulo = input<string>();
}
