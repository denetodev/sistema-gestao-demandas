import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';
import { BarraPagina } from '../../core/layout/barra-pagina/barra-pagina';

@Component({
  selector: 'app-meu-perfil',
  imports: [ReactiveFormsModule, InputText, Password, Button, Message, BarraPagina],
  templateUrl: './meu-perfil.html',
  styleUrl: './meu-perfil.scss',
})
export class MeuPerfil {
  #fb = inject(FormBuilder);
  #messageService = inject(MessageService);
  auth = inject(AuthService);

  salvandoDados = signal(false);
  salvandoSenha = signal(false);
  erroSenha = signal<string | null>(null);

  formDados = this.#fb.nonNullable.group({
    nomeExibicao: [this.auth.pessoa()?.nomeExibicao ?? '', Validators.maxLength(80)],
  });

  formSenha = this.#fb.nonNullable.group({
    senha: ['', [Validators.required, Validators.minLength(6)]],
    confirmacao: ['', Validators.required],
  });

  async salvarDados() {
    if (this.formDados.invalid) return;
    this.salvandoDados.set(true);
    try {
      await firstValueFrom(
        this.auth.atualizarPerfil({
          nomeExibicao: this.formDados.getRawValue().nomeExibicao.trim() || null,
          fotoUrl: this.auth.pessoa()?.fotoUrl ?? null,
        }),
      );
      this.auth.me.reload();
      this.#messageService.add({ severity: 'success', summary: 'Perfil atualizado', life: 4000 });
    } catch {
      // o toast do interceptor já informou
    } finally {
      this.salvandoDados.set(false);
    }
  }

  async salvarSenha() {
    const { senha, confirmacao } = this.formSenha.getRawValue();
    if (this.formSenha.invalid) return;
    if (senha !== confirmacao) {
      this.erroSenha.set('As senhas não coincidem.');
      return;
    }
    this.salvandoSenha.set(true);
    this.erroSenha.set(null);
    try {
      await this.auth.trocarSenha(senha);
      this.formSenha.reset();
      this.#messageService.add({ severity: 'success', summary: 'Senha alterada', life: 4000 });
    } catch {
      this.erroSenha.set('Não foi possível alterar a senha.');
    } finally {
      this.salvandoSenha.set(false);
    }
  }
}
