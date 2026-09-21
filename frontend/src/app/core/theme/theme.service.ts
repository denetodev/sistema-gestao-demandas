import { Injectable, signal, effect } from '@angular/core';

const STORAGE_KEY = 'sgd-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  #dark = signal<boolean>(localStorage.getItem(STORAGE_KEY) === 'dark');
  dark = this.#dark.asReadonly();

  constructor() {
    effect(() => {
      const isDark = this.#dark();
      document.documentElement.classList.toggle('app-dark', isDark);
      localStorage.setItem(STORAGE_KEY, isDark ? 'dark' : 'light');
    });
  }

  toggle() {
    this.#dark.update((v) => !v);
  }
}
