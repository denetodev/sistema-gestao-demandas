import type { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Tira espaços e põe em maiúscula: c1349864 -> C1349864. */
export function normalizarChaveC(valor: string | null | undefined): string {
  return (valor ?? '').trim().toUpperCase();
}

/** C (contratado) ou F (funcionário do Banco) seguida de 7 dígitos. */
export function chaveCValida(valor: string | null | undefined): boolean {
  return /^[CF][0-9]{7}$/.test(normalizarChaveC(valor));
}

/** Vazio passa (use Validators.required junto, se for obrigatório). */
export const chaveCValidator: ValidatorFn = (c: AbstractControl): ValidationErrors | null =>
  !c.value || chaveCValida(c.value) ? null : { chaveCInvalida: true };
