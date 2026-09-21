import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { PrimeTemplate } from 'primeng/api';
import { Toolbar } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { Button } from 'primeng/button';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, PrimeTemplate, Toolbar, Tag, Button],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  auth = inject(AuthService);
  #router = inject(Router);

  async sair() {
    await this.auth.logout();
    this.#router.navigateByUrl('/login');
  }
}
