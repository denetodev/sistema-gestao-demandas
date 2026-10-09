import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Textarea } from 'primeng/textarea';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { DemandaItensService } from '../../data-access/demanda-itens.service';
import { Atividade, Evidencia, Peca } from '../../data-access/demanda-itens.model';

type ModoEvidencia = 'IMAGEM' | 'LINK';

const TIPOS_ACEITOS = ['image/png', 'image/jpeg'];
const TAMANHO_MAXIMO = 5 * 1024 * 1024;
const REGEX_LINK = /^https?:\/\/\S+$/i;

@Component({
  selector: 'app-secao-evidencias',
  imports: [FormsModule, ReactiveFormsModule, TableModule, Button, Dialog, Select, SelectButton, Textarea, InputText, Message],
  templateUrl: './secao-evidencias.html',
  styleUrl: '../demanda-detalhe/demanda-detalhe.scss',
})
export class SecaoEvidencias {
  /** Todas as evidências do sistema; filtradas aqui pelas atividades e peças da demanda. */
  todas = input.required<Evidencia[]>();
  atividades = input.required<Atividade[]>();
  pecas = input.required<Peca[]>();
  editavel = input(false);
  alterado = output<void>();

  #service = inject(DemandaItensService);
  #fb = inject(FormBuilder);

  dialogAberto = signal(false);
  salvando = signal(false);
  modo = signal<ModoEvidencia>('IMAGEM');
  arquivo = signal<File | null>(null);
  erroArquivo = signal<string | null>(null);

  opcoesModo = [
    { label: 'Imagem', value: 'IMAGEM' as ModoEvidencia },
    { label: 'Link externo', value: 'LINK' as ModoEvidencia },
  ];

  alvos = computed(() => [
    ...this.atividades().map((a) => ({
      value: `A:${a.id}`,
      label: `Atividade: ${a.tipoAtividadeNome}${a.descricao ? ' – ' + a.descricao : ''}`,
    })),
    ...this.pecas().map((p) => ({ value: `P:${p.id}`, label: `Peça: ${p.nome}` })),
  ]);

  itens = computed(() => {
    const atvs = new Map(this.atividades().map((a) => [a.id, a]));
    const pecas = new Map(this.pecas().map((p) => [p.id, p]));
    return this.todas()
      .filter((e) => (e.atividadeId && atvs.has(e.atividadeId)) || (e.pecaId && pecas.has(e.pecaId)))
      .map((e) => ({
        ...e,
        alvo: e.pecaId
          ? `Peça: ${pecas.get(e.pecaId)?.nome ?? ''}`
          : `Atividade: ${atvs.get(e.atividadeId!)?.tipoAtividadeNome ?? ''}`,
      }));
  });

  form = this.#fb.group({
    alvo: ['', Validators.required],
    link: [''],
    descricao: [''],
  });

  /** O valor do formulário não é signal: este espelho faz o podeSalvar reagir à digitação do link. */
  #link = toSignal(this.form.controls.link.valueChanges, { initialValue: '' });

  /** Pode salvar quando há destino e (imagem escolhida e válida, ou link http/https). */
  podeSalvar = computed(() => {
    if (this.salvando()) return false;
    return this.modo() === 'IMAGEM' ? !!this.arquivo() && !this.erroArquivo() : this.#linkValido();
  });

  #linkValido() {
    return REGEX_LINK.test((this.#link() ?? '').trim());
  }

  abrir() {
    this.form.reset({ alvo: '', link: '', descricao: '' });
    this.arquivo.set(null);
    this.erroArquivo.set(null);
    this.modo.set('IMAGEM');
    this.dialogAberto.set(true);
  }

  escolherArquivo(evento: Event) {
    const input = evento.target as HTMLInputElement;
    const escolhido = input.files?.[0] ?? null;
    this.arquivo.set(escolhido);
    if (!escolhido) {
      this.erroArquivo.set(null);
    } else if (!TIPOS_ACEITOS.includes(escolhido.type)) {
      this.erroArquivo.set('Só são aceitas imagens JPEG ou PNG.');
    } else if (escolhido.size > TAMANHO_MAXIMO) {
      this.erroArquivo.set('A imagem passa de 5 MB.');
    } else {
      this.erroArquivo.set(null);
    }
  }

  async salvar() {
    if (this.form.invalid || !this.podeSalvar()) return;
    this.salvando.set(true);
    try {
      const v = this.form.getRawValue();
      const [kind, id] = (v.alvo ?? '').split(':');
      const vinculo = { atividadeId: kind === 'A' ? id : null, pecaId: kind === 'P' ? id : null };
      const descricao = v.descricao?.trim() || null;

      if (this.modo() === 'IMAGEM') {
        await firstValueFrom(this.#service.enviarImagem(this.arquivo()!, vinculo, descricao));
      } else {
        await firstValueFrom(
          this.#service.criarEvidencia({ ...vinculo, tipo: 'LINK', conteudo: v.link!.trim(), descricao }),
        );
      }
      this.dialogAberto.set(false);
      this.alterado.emit();
    } catch {
      // toast do interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  /** O bucket é privado: pede uma URL assinada e abre a imagem numa aba nova. */
  async verImagem(e: Evidencia) {
    const aba = window.open('', '_blank'); // abre já, no clique, para o navegador não bloquear
    try {
      const { url } = await firstValueFrom(this.#service.urlDoArquivo(e.id));
      if (aba) {
        aba.opener = null;
        aba.location.href = url;
      }
    } catch {
      aba?.close(); // toast do interceptor
    }
  }

  tamanho(bytes: number | null): string {
    if (bytes === null) return '';
    return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  async remover(e: Evidencia) {
    try {
      await firstValueFrom(this.#service.removerEvidencia(e.id));
      this.alterado.emit();
    } catch {
      // toast do interceptor
    }
  }
}
