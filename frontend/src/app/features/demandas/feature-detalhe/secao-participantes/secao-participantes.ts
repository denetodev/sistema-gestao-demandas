import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TableModule } from 'primeng/table';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { DemandaItensService } from '../../data-access/demanda-itens.service';
import { Participante, PapelParticipante, ROTULO_PAPEL } from '../../data-access/demanda-itens.model';

@Component({
  selector: 'app-secao-participantes',
  imports: [FormsModule, ReactiveFormsModule, TableModule, Button, Dialog, Select],
  templateUrl: './secao-participantes.html',
  styleUrl: '../demanda-detalhe/demanda-detalhe.scss',
})
export class SecaoParticipantes {
  demandaId = input.required<string>();
  itens = input.required<Participante[]>();
  editavel = input(false);
  alterado = output<void>();

  #service = inject(DemandaItensService);
  #fb = inject(FormBuilder);

  pessoas = this.#service.pessoas();
  dialogAberto = signal(false);
  salvando = signal(false);
  rotuloPapel = (papel: PapelParticipante) => ROTULO_PAPEL[papel];

  opcoesPessoa = computed(() => {
    const jaNoTime = new Set(this.itens().map((p) => p.pessoaId));
    return (this.pessoas.value() ?? []).filter(
      (p) => p.status === 'ATIVO' && p.aprovadoEm !== null && !jaNoTime.has(p.id),
    );
  });

  opcoesPapel = (Object.keys(ROTULO_PAPEL) as PapelParticipante[]).map((v) => ({
    value: v,
    label: ROTULO_PAPEL[v],
  }));

  form = this.#fb.group({
    pessoaId: ['', Validators.required],
    papel: this.#fb.control<PapelParticipante | null>('PARTICIPANTE'),
  });

  abrir() {
    this.form.reset({ pessoaId: '', papel: 'PARTICIPANTE' });
    this.dialogAberto.set(true);
  }

  async salvar() {
    if (this.form.invalid) return;
    this.salvando.set(true);
    try {
      const v = this.form.getRawValue();
      await firstValueFrom(
        this.#service.adicionarParticipante(this.demandaId(), { pessoaId: v.pessoaId!, papel: v.papel }),
      );
      this.dialogAberto.set(false);
      this.alterado.emit();
    } catch {
      // toast do interceptor
    } finally {
      this.salvando.set(false);
    }
  }

  async mudarPapel(p: Participante, papel: PapelParticipante) {
    if (papel === p.papel) return;
    try {
      await firstValueFrom(this.#service.atualizarPapel(this.demandaId(), p.id, papel));
    } catch {
      // toast do interceptor
    }
    this.alterado.emit(); // recarrega: em caso de erro, o seletor volta ao valor do servidor
  }

  async remover(p: Participante) {
    try {
      await firstValueFrom(this.#service.removerParticipante(this.demandaId(), p.id));
      this.alterado.emit();
    } catch {
      // toast do interceptor
    }
  }
}
