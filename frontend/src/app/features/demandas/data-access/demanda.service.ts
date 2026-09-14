import { Injectable, inject } from '@angular/core';
import { HttpClient, httpResource } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Demanda } from './demanda.model';

@Injectable({ providedIn: 'root' })
export class DemandaService {
    #http = inject(HttpClient);
    #base = `${environment.apiUrl}/demandas`;

    listar = httpResource<Demanda[]>(() => this.#base);

    criar(payload: Partial<Demanda>) {
        return this.#http.post<Demanda>(this.#base, payload);
    }
}