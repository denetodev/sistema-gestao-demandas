import { Component, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { Menubar } from 'primeng/menubar';
import { Toolbar } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { Button } from 'primeng/button';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, PrimeTemplate, Toolbar, Tag, Button, Menubar],
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

  itensMenu: MenuItem[] = [
    { label: 'Demandas', icon: 'pi pi-briefcase', routerLink: '/demandas' },
    {
      label: 'Cadastros',
      icon: 'pi pi-database',
      items: [
        { label: 'Clientes', icon: 'pi pi-users', routerLink: '/clientes' },
        { label: 'Projetos', icon: 'pi pi-folder', routerLink: '/projetos' },
        { label: 'Campanhas', icon: 'pi pi-megaphone', routerLink: '/campanhas' },
      ],
    },
  ];
}
