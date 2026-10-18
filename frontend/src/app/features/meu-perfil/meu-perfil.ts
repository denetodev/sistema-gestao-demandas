import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';
import { TAMANHO_MAXIMO_ORIGINAL, TIPOS_DE_FOTO, reduzirParaQuadrado } from '../../core/util/imagem';
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
  enviandoFoto = signal(false);
  erroFoto = signal<string | null>(null);

  iniciais = computed(() => {
    const partes = (this.auth.pessoa()?.nomeExibicao || this.auth.pessoa()?.nome || '?').trim().split(/\s+/);
    return ((partes[0]?.[0] ?? '?') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase();
  });

  async escolherFoto(evento: Event) {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    input.value = '';
    if (!arquivo) return;
    this.erroFoto.set(null);
    if (!TIPOS_DE_FOTO.includes(arquivo.type)) {
      this.erroFoto.set('Escolha uma imagem JPEG, PNG ou WebP.');
      return;
    }
    if (arquivo.size > TAMANHO_MAXIMO_ORIGINAL) {
      this.erroFoto.set('A imagem é grande demais (máximo 15 MB).');
      return;
    }
    this.enviandoFoto.set(true);
    try {
      const pronta = await reduzirParaQuadrado(arquivo);
      await firstValueFrom(this.auth.enviarFotoDePerfil(pronta));
      this.auth.me.reload();
      this.#messageService.add({ severity: 'success', summary: 'Foto atualizada', life: 4000 });
    } catch (e) {
      const mensagem = (e as { error?: { mensagem?: string } })?.error?.mensagem;
      this.erroFoto.set(mensagem ?? 'Não foi possível enviar a foto. Tente outra imagem.');
    } finally {
      this.enviandoFoto.set(false);
    }
  }

  async removerFoto() {
    this.erroFoto.set(null);
    this.enviandoFoto.set(true);
    try {
      await firstValueFrom(this.auth.removerFotoDePerfil());
      this.auth.me.reload();
      this.#messageService.add({ severity: 'success', summary: 'Foto removida', life: 4000 });
    } catch {
      this.erroFoto.set('Não foi possível remover a foto. Tente de novo.');
    } finally {
      this.enviandoFoto.set(false);
    }
  }

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
          fotoUrl: null, // a foto tem envio próprio; o servidor ignora este campo
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
