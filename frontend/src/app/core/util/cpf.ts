import type { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function somenteDigitos(valor: string | null | undefined): string {
  return (valor ?? '').replace(/\D/g, '');
}

/** 11 dígitos, não repetidos, com os dois dígitos verificadores corretos. */
export function cpfValido(valor: string | null | undefined): boolean {
  const d = somenteDigitos(valor);
  if (!/^\d{11}$/.test(d) || /^(\d)\1{10}$/.test(d)) return false;
  const digito = (quantos: number) => {
    let soma = 0;
    for (let i = 0; i < quantos; i++) soma += Number(d[i]) * (quantos + 1 - i);
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(d[9]) && digito(10) === Number(d[10]);
}

/** Máscara progressiva: 52998224725 -> 529.982.247-25. */
export function formatarCpf(valor: string | null | undefined): string {
  const d = somenteDigitos(valor).slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2');
}

/** Vazio passa (use Validators.required junto, se for obrigatório). */
export const cpfValidator: ValidatorFn = (c: AbstractControl): ValidationErrors | null =>
  !c.value || cpfValido(c.value) ? null : { cpfInvalido: true };
