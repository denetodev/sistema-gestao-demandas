import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { Button } from 'primeng/button';
import { AuthService } from '../../../core/auth/auth.service';

/** Destino dos guards de perfil quando a pessoa tenta abrir uma página que o papel dela não cobre. */
@Component({
  selector: 'app-acesso-negado',
  imports: [RouterLink, Button],
  templateUrl: './acesso-negado.html',
  styleUrl: './acesso-negado.scss',
})
export class AcessoNegado {
  auth = inject(AuthService);

  /** Rota que o guard barrou (vem em ?de=). */
  de = toSignal(inject(ActivatedRoute).queryParamMap.pipe(map((p) => p.get('de'))), { initialValue: null });
}
