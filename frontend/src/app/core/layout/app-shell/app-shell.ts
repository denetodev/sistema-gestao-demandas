import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { ThemeService } from '../../theme/theme.service';
import { PendenciasEquipamentos } from '../../equipamentos/pendencias-equipamentos';
import { Menu } from 'primeng/menu';
import { Sino } from '../../notificacoes/sino/sino';
import { ContaPendente } from '../../../features/acesso/conta-pendente/conta-pendente';
import { CompletarCadastro } from '../../../features/acesso/completar-cadastro/completar-cadastro';
import type { MenuItem } from 'primeng/api';

const CHAVE_SIDEBAR = 'sgd-sidebar-recolhida';
const LARGURA_CELULAR = 900;

const ROTULO_PERFIL: Record<string, string> = {
  ADMIN: 'Admin',
  GESTOR: 'Gestor',
  PROFISSIONAL: 'Profissional',
  VISUALIZADOR: 'Visualizador',
};

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Menu, ContaPendente, CompletarCadastro, Sino],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  auth = inject(AuthService);
  tema = inject(ThemeService);
  pendencias = inject(PendenciasEquipamentos);
  #router = inject(Router);

  /** No computador o menu recolhe para só os ícones; a escolha fica guardada neste navegador. */
  sidebarRecolhida = signal(lerRecolhida());
  /** No celular o menu é uma gaveta que abre por cima do conteúdo. */
  menuMovelAberto = signal(false);
  busca = signal('');

  /** O nome que a pessoa escolheu ou, sem ele, o nome completo. */
  nomeExibido = computed(() => this.auth.pessoa()?.nomeExibicao || this.auth.pessoa()?.nome || '');

  perfilRotulo = computed(() => ROTULO_PERFIL[this.auth.pessoa()?.perfil ?? ''] ?? '');

  /** Diretoria, núcleo, equipe e cargo da pessoa: o trilho que aparece no rodapé do menu. */
  hierarquia = computed(() => {
    const p = this.auth.pessoa();
    return { diretoria: p?.diretoriaNome ?? '—', nucleo: p?.nucleoNome ?? '—', equipe: p?.areaNome ?? '—', cargo: p?.cargoNome ?? '—' };
  });

  iniciais = computed(() => {
    const nome = this.nomeExibido();
    const partes = nome.trim().split(/\s+/);
    if (partes.length === 0 || !partes[0]) return '?';
    const primeira = partes[0][0];
    const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
    return (primeira + ultima).toUpperCase();
  });

  /** As ferramentas da conta ficam todas aqui: perfil, tema e saída. */
  itensUsuario = computed<MenuItem[]>(() => [
    { label: 'Meu perfil', icon: 'pi pi-user', routerLink: '/meu-perfil' },
    {
      label: this.tema.dark() ? 'Tema claro' : 'Tema escuro',
      icon: this.tema.dark() ? 'pi pi-sun' : 'pi pi-moon',
      command: () => this.tema.toggle(),
    },
    { separator: true },
    { label: 'Sair', icon: 'pi pi-sign-out', command: () => this.sair() },
  ]);

  alternarMenu() {
    if (window.innerWidth < LARGURA_CELULAR) {
      this.menuMovelAberto.update((v) => !v);
      return;
    }
    this.sidebarRecolhida.update((v) => !v);
    try {
      localStorage.setItem(CHAVE_SIDEBAR, String(this.sidebarRecolhida()));
    } catch {
      /* sem persistência, tudo bem */
    }
  }

  fecharMenuMovel() {
    this.menuMovelAberto.set(false);
  }

  /** Enter na busca do topo leva para a lista de demandas já filtrada. */
  buscar(evento: Event) {
    evento.preventDefault();
    const texto = this.busca().trim();
    this.#router.navigate(['/demandas'], { queryParams: texto ? { busca: texto } : {} });
    this.fecharMenuMovel();
  }

  async sair() {
    await this.auth.logout();
    this.#router.navigateByUrl('/login');
  }
}

function lerRecolhida(): boolean {
  try {
    return localStorage.getItem(CHAVE_SIDEBAR) === 'true';
  } catch {
    return false;
  }
}
