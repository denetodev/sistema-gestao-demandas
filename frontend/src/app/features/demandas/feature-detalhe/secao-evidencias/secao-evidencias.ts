import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
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
import { DemandaItensService } from '../../data-access/demanda-itens.service';
import { Atividade, Evidencia, Peca } from '../../data-access/demanda-itens.model';

type ModoEvidencia = 'IMAGEM' | 'LINK';

const TIPOS_ACEITOS = ['image/png', 'image/jpeg'];
const TAMANHO_MAXIMO = 5 * 1024 * 1024;
const REGEX_LINK = /^https?:\/\/\S+$/i;

/** Uma imagem escolhida para enviar: a miniatura é criada na hora e o erro (se houver) fica junto dela. */
interface ArquivoEscolhido {
  id: string;
  arquivo: File;
  previa: string;
  erro: string | null;
}

function erroDoArquivo(arquivo: File): string | null {
  if (!TIPOS_ACEITOS.includes(arquivo.type)) return 'Só são aceitas imagens JPEG ou PNG.';
  if (arquivo.size > TAMANHO_MAXIMO) return 'A imagem passa de 5 MB.';
  return null;
}

@Component({
  selector: 'app-secao-evidencias',
  imports: [FormsModule, ReactiveFormsModule, TableModule, Button, Dialog, Select, SelectButton, Textarea, InputText],
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

  constructor() {
    inject(DestroyRef).onDestroy(() => this.#limparArquivos());
  }

  dialogAberto = signal(false);
  salvando = signal(false);
  modo = signal<ModoEvidencia>('IMAGEM');
  /** As imagens escolhidas (várias de uma vez). As com erro ficam à vista, mas não são enviadas. */
  arquivos = signal<ArquivoEscolhido[]>([]);
  validos = computed(() => this.arquivos().filter((a) => !a.erro));
  arrastando = signal(false);
  /** Quantas já foram enviadas neste lote. */
  enviadas = signal(0);
  rotuloAdicionar = computed(() => {
    const n = this.validos().length;
    return this.modo() === 'IMAGEM' && n > 1 ? `Adicionar ${n} evidências` : 'Adicionar evidência';
  });

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
    return this.modo() === 'IMAGEM' ? this.validos().length > 0 : this.#linkValido();
  });

  #linkValido() {
    return REGEX_LINK.test((this.#link() ?? '').trim());
  }

  abrir() {
    this.form.reset({ alvo: '', link: '', descricao: '' });
    this.#limparArquivos();
    this.modo.set('IMAGEM');
    this.dialogAberto.set(true);
  }

  #limparArquivos() {
    this.arquivos().forEach((a) => URL.revokeObjectURL(a.previa));
    this.arquivos.set([]);
    this.enviadas.set(0);
  }

  /** Vindo do seletor de arquivos: acrescenta ao que já foi escolhido, para dar para juntar de pastas diferentes. */
  escolherArquivo(evento: Event) {
    const input = evento.target as HTMLInputElement;
    this.adicionarArquivos(Array.from(input.files ?? []));
    try {
      input.value = ''; // permite escolher de novo o mesmo arquivo depois de remover
    } catch {
      // alguns navegadores não deixam limpar
    }
  }

  aoArrastar(evento: DragEvent) {
    evento.preventDefault();
    this.arrastando.set(true);
  }

  aoSoltar(evento: DragEvent) {
    evento.preventDefault();
    this.arrastando.set(false);
    this.adicionarArquivos(Array.from(evento.dataTransfer?.files ?? []));
  }

  adicionarArquivos(novos: File[]) {
    const chave = (f: File) => `${f.name}|${f.size}|${f.lastModified}`;
    const jaTem = new Set(this.arquivos().map((a) => chave(a.arquivo)));
    const acrescentar: ArquivoEscolhido[] = [];
    for (const f of novos) {
      if (jaTem.has(chave(f))) continue;
      jaTem.add(chave(f));
      const erro = erroDoArquivo(f);
      acrescentar.push({ id: crypto.randomUUID(), arquivo: f, previa: erro ? '' : URL.createObjectURL(f), erro });
    }
    if (acrescentar.length) this.arquivos.update((atual) => [...atual, ...acrescentar]);
  }

  removerArquivo(id: string) {
    const alvo = this.arquivos().find((a) => a.id === id);
    if (alvo?.previa) URL.revokeObjectURL(alvo.previa);
    this.arquivos.update((atual) => atual.filter((a) => a.id !== id));
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
        // uma evidência por imagem, em sequência; se uma falhar, as que já subiram ficam e as outras continuam na lista
        let subiu = false;
        try {
          for (const a of [...this.validos()]) {
            await firstValueFrom(this.#service.enviarImagem(a.arquivo, vinculo, descricao));
            subiu = true;
            this.enviadas.update((n) => n + 1);
            this.removerArquivo(a.id);
          }
        } finally {
          if (subiu) this.alterado.emit();
        }
        if (this.arquivos().length === 0) this.dialogAberto.set(false);
        return;
      }
      await firstValueFrom(
        this.#service.criarEvidencia({ ...vinculo, tipo: 'LINK', conteudo: v.link!.trim(), descricao }),
      );
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
