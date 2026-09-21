import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, InputText, Password, Button, Message],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  #fb = inject(FormBuilder);
  #auth = inject(AuthService);
  #router = inject(Router);

  form = this.#fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  carregando = signal(false);
  erro = signal<string | null>(null);

  async entrar() {
    if (this.form.invalid) return;
    this.carregando.set(true);
    this.erro.set(null);
    const { email, senha } = this.form.getRawValue();
    try {
      await this.#auth.login(email, senha);
      this.#router.navigateByUrl('/demandas');
    } catch {
      this.erro.set('E-mail ou senha inválidos.');
    } finally {
      this.carregando.set(false);
    }
  }
}
