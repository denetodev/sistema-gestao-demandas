import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Button } from 'primeng/button';
import { AuthService } from '../../auth/auth.service';
import { ThemeService } from '../../theme/theme.service';

const CHAVE_SIDEBAR = 'sgd-sidebar-recolhida';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Button],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  auth = inject(AuthService);
  tema = inject(ThemeService);
  #router = inject(Router);

  sidebarRecolhida = signal(localStorage.getItem(CHAVE_SIDEBAR) === 'true');

  iniciais = computed(() => {
    const nome = this.auth.pessoa()?.nome ?? '';
    const partes = nome.trim().split(/\s+/);
    if (partes.length === 0 || !partes[0]) return '?';
    const primeira = partes[0][0];
    const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
    return (primeira + ultima).toUpperCase();
  });

  alternarSidebar() {
    this.sidebarRecolhida.update(v => !v);
    localStorage.setItem(CHAVE_SIDEBAR, String(this.sidebarRecolhida()));
  }

  async sair() {
    await this.auth.logout();
    this.#router.navigateByUrl('/login');
  }
}