import { Component, inject } from '@angular/core';
import { DemandaService } from '../../data-access/demanda.service';

@Component({
  selector: 'app-demanda-lista',
  imports: [],
  templateUrl: './demanda-lista.html',
  styleUrl: './demanda-lista.scss',
})
export class DemandaLista {
  #service = inject(DemandaService);
  demandas = this.#service.listar;
}
