import { Component, input } from '@angular/core';

@Component({
  selector: 'app-barra-pagina',
  imports: [],
  templateUrl: './barra-pagina.html',
  styleUrl: './barra-pagina.scss',
})
export class BarraPagina {
  titulo = input<string>();
}
