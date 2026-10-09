import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { AuthService } from '../../../core/auth/auth.service';
import { AreaService } from '../../../core/dados-mestres/area/area.service';
import { CargoService } from '../../../core/dados-mestres/cargo/cargo.service';
import { cpfValidator, formatarCpf } from '../../../core/util/cpf';

/**
 * Tela cheia para quem criou a conta (e-mail/senha) mas ainda não tem cadastro de pessoa.
 * Depois de enviar, o cadastro fica aguardando aprovação de um Gestor/Admin.
 */
@Component({
  selector: 'app-completar-cadastro',
  imports: [ReactiveFormsModule, InputText, Select, Button, Message],
  templateUrl: './completar-cadastro.html',
  styleUrl: './completar-cadastro.scss',
})
export class CompletarCadastro {
  #fb = inject(FormBuilder);
  #auth = inject(AuthService);
  #router = inject(Router);
  #cargoService = inject(CargoService);

  areas = inject(AreaService).listar;
  cargos = computed(() => (this.#cargoService.listar.value() ?? []).filter((c) => c.ativo));
  email = this.#auth.emailSessao;

  form = this.#fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(180)]],
    cpf: ['', [Validators.required, cpfValidator]],
    nomeExibicao: ['', Validators.maxLength(80)],
    areaId: this.#fb.control<string | null>(null, Validators.required),
    cargoId: this.#fb.control<string | null>(null),
  });

  enviando = signal(false);
  erro = signal<string | null>(null);

  mascararCpf() {
    const c = this.form.controls.cpf;
    const formatado = formatarCpf(c.value);
    if (formatado !== c.value) c.setValue(formatado);
  }

  async enviar() {
    if (this.form.invalid) return;
    this.enviando.set(true);
    this.erro.set(null);
    const v = this.form.getRawValue();
    try {
      await firstValueFrom(this.#auth.completarCadastro({
        nome: v.nome.trim(),
        cpf: v.cpf,
        nomeExibicao: v.nomeExibicao.trim() || null,
        areaId: v.areaId!,
        cargoId: v.cargoId,
      }));
      this.#auth.me.reload();
    } catch (e) {
      this.erro.set(e instanceof HttpErrorResponse && e.error?.mensagem
        ? e.error.mensagem
        : 'Não foi possível enviar o cadastro. Tente novamente.');
    } finally {
      this.enviando.set(false);
    }
  }

  async sair() {
    await this.#auth.logout();
    this.#router.navigateByUrl('/login');
  }
}
