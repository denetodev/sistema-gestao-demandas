import type { Translation } from 'primeng/api';

/** Textos do PrimeNG em português (calendário, filtros e botões padrão). */
export const PRIMENG_PT_BR: Translation = {
  firstDayOfWeek: 0,
  dayNames: ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado'],
  dayNamesShort: ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'],
  dayNamesMin: ['D', 'S', 'T', 'Q', 'Q', 'S', 'S'],
  monthNames: [
    'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
    'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
  ],
  monthNamesShort: ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'],
  today: 'Hoje',
  clear: 'Limpar',
  weekHeader: 'Sem',
  dateFormat: 'dd/mm/yy',
  apply: 'Aplicar',
  cancel: 'Cancelar',
  accept: 'Sim',
  reject: 'Não',
  emptyMessage: 'Nenhum resultado',
  emptyFilterMessage: 'Nenhum resultado',
};
