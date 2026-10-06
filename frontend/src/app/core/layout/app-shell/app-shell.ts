import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Button } from 'primeng/button';
import { AuthService } from '../../auth/auth.service';
import { ThemeService } from '../../theme/theme.service';
import { Menu } from 'primeng/menu';
import { ContaPendente } from '../../../features/acesso/conta-pendente/conta-pendente';
import type { MenuItem } from 'primeng/api';

const CHAVE_SIDEBAR = 'sgd-sidebar-recolhida';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Button, Menu, ContaPendente],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  auth = inject(AuthService);
  tema = inject(ThemeService);
  #router = inject(Router);

  sidebarRecolhida = signal(localStorage.getItem(CHAVE_SIDEBAR) === 'true');

  /** O nome que a pessoa escolheu ou, sem ele, o nome completo. */
  nomeExibido = computed(() => this.auth.pessoa()?.nomeExibicao || this.auth.pessoa()?.nome || '');

  iniciais = computed(() => {
    const nome = this.nomeExibido();
    const partes = nome.trim().split(/\s+/);
    if (partes.length === 0 || !partes[0]) return '?';
    const primeira = partes[0][0];
    const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
    return (primeira + ultima).toUpperCase();
  });

  itensUsuario: MenuItem[] = [
    { label: 'Meu perfil', icon: 'pi pi-user', routerLink: '/meu-perfil' },
    { separator: true },
    { label: 'Sair', icon: 'pi pi-sign-out', command: () => this.sair() },
  ];

  alternarSidebar() {
    this.sidebarRecolhida.update((v) => !v);
    localStorage.setItem(CHAVE_SIDEBAR, String(this.sidebarRecolhida()));
  }

  async sair() {
    await this.auth.logout();
    this.#router.navigateByUrl('/login');
  }
}