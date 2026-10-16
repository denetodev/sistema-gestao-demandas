import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { BarraPagina } from '../../layout/barra-pagina/barra-pagina';
import { Ajuda } from '../../ajuda/ajuda/ajuda';
import { AreaService, type Area } from '../area/area.service';
import { DiretoriaService } from '../diretoria/diretoria.service';
import { NucleoService, type Nucleo } from '../nucleo/nucleo.service';

/** Diretoria > núcleo > equipe: cadastra os núcleos e diz em qual núcleo cada equipe está. */
@Component({
  selector: 'app-estrutura',
  imports: [ReactiveFormsModule, TableModule, Dialog, InputText, Select, Button, Message, BarraPagina, Ajuda],
  templateUrl: './estrutura.html',
  styleUrl: './estrutura.scss',
})
export class Estrutura {
  #fb = inject(FormBuilder);
  #nucleoService = inject(NucleoService);
  #areaService = inject(AreaService);


  nucleos = this.#nucleoService.listar;
  areas = this.#areaService.listar;
  diretorias = inject(DiretoriaService).listar;

  nucleosAtivos = computed(() => (this.nucleos.value() ?? []).filter((n) => n.ativo));
  equipes = computed(() =>
    [...(this.areas.value() ?? [])]
      .filter((a) => a.ativo)
      .sort(
        (a, b) =>
          (a.diretoriaNome ?? '').localeCompare(b.diretoriaNome ?? '', 'pt-BR') ||
          a.nome.localeCompare(b.nome, 'pt-BR'),
      ),
  );
  equipesSemNucleo = computed(() => this.equipes().filter((a) => !a.nucleoId).length);

  qtdEquipes(nucleoId: string): number {
    return this.equipes().filter((a) => a.nucleoId === nucleoId).length;
  }

  salvando = signal(false);
  erro = signal<string | null>(null);

  // ---- núcleo ----
  nucleoAberto = signal(false);
  nucleoEditando = signal<string | null>(null);

  formNucleo = this.#fb.nonNullable.group({
    nome: ['', Validators.required],
    diretoriaId: this.#fb.control<string | null>(null, Validators.required),
  });

  novoNucleo() {
    this.nucleoEditando.set(null);
    this.erro.set(null);
    this.formNucleo.reset();
    this.nucleoAberto.set(true);
  }

  editarNucleo(n: Nucleo) {
    this.nucleoEditando.set(n.id);
    this.erro.set(null);
    this.formNucleo.setValue({ nome: n.nome, diretoriaId: n.diretoriaId });
    this.nucleoAberto.set(true);
  }

  async salvarNucleo() {
    if (this.formNucleo.invalid) return;
    this.salvando.set(true);
    this.erro.set(null);
    const v = this.formNucleo.getRawValue();
    const corpo = { nome: v.nome, diretoriaId: v.diretoriaId! };
    const id = this.nucleoEditando();
    try {
      await firstValueFrom(id ? this.#nucleoService.atualizar(id, corpo) : this.#nucleoService.criar(corpo));
      this.nucleoAberto.set(false);
      this.nucleos.reload();
      this.areas.reload();
    } catch {
      this.erro.set('Não foi possível salvar o núcleo.');
    } finally {
      this.salvando.set(false);
    }
  }

  // ---- equipe ----
  equipeAberta = signal(false);
  equipeEditando = signal<Area | null>(null);
  nucleoDaEquipe = this.#fb.control<string | null>(null);

  /** Só os núcleos da diretoria da equipe. */
  nucleosDaEquipe = computed(() => {
    const e = this.equipeEditando();
    return this.nucleosAtivos().filter((n) => n.diretoriaId === e?.diretoriaId);
  });

  editarEquipe(a: Area) {
    this.equipeEditando.set(a);
    this.erro.set(null);
    this.nucleoDaEquipe.setValue(a.nucleoId);
    this.equipeAberta.set(true);
  }

  async salvarEquipe() {
    const e = this.equipeEditando();
    if (!e || !e.diretoriaId) return;
    this.salvando.set(true);
    this.erro.set(null);
    try {
      await firstValueFrom(
        this.#areaService.atualizar(e.id, {
          nome: e.nome,
          diretoriaId: e.diretoriaId,
          nucleoId: this.nucleoDaEquipe.value,
        }),
      );
      this.equipeAberta.set(false);
      this.areas.reload();
    } catch {
      this.erro.set('Não foi possível mudar o núcleo da equipe.');
    } finally {
      this.salvando.set(false);
    }
  }
}
