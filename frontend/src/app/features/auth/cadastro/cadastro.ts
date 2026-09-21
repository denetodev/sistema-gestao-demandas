import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-cadastro',
  imports: [ReactiveFormsModule, RouterLink, InputText, Password, Button, Message],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.scss',
})
export class Cadastro {
  #fb = inject(FormBuilder);
  #auth = inject(AuthService);
  #router = inject(Router);

  form = this.#fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6)]],
  });

  carregando = signal(false);
  erro = signal<string | null>(null);
  sucesso = signal<string | null>(null);

  async criarConta() {
    if (this.form.invalid) return;
    this.carregando.set(true);
    this.erro.set(null);
    this.sucesso.set(null);
    const { email, senha } = this.form.getRawValue();
    try {
      const { confirmado } = await this.#auth.cadastrar(email, senha);
      if (confirmado) {
        this.#router.navigateByUrl('/demandas');
      } else {
        this.sucesso.set('Conta criada. Verifique seu e-mail para confirmar antes de entrar.');
      }
    } catch {
      this.erro.set('Não foi possível criar a conta. Tente outro e-mail ou senha.');
    } finally {
      this.carregando.set(false);
    }
  }
}
