import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'pen', standalone: true })
export class PenPipe implements PipeTransform {
  transform(value: unknown): string {
    if (value === null || value === undefined || value === '') return '—';
    const amount = Number(value);
    if (Number.isNaN(amount)) return '—';
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(amount);
  }
}

@Pipe({ name: 'fecha', standalone: true })
export class FechaPipe implements PipeTransform {
  transform(value: unknown, withTime = true): string {
    if (!value) return '—';
    const date = new Date(String(value));
    if (Number.isNaN(date.getTime())) return String(value);
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'short',
      ...(withTime ? { timeStyle: 'short' } : {}),
    }).format(date);
  }
}

@Pipe({ name: 'numero', standalone: true })
export class NumeroPipe implements PipeTransform {
  transform(value: unknown, digits = 2): string {
    if (value === null || value === undefined || value === '') return '—';
    const amount = Number(value);
    if (Number.isNaN(amount)) return '—';
    return new Intl.NumberFormat('es-PE', { maximumFractionDigits: digits }).format(amount);
  }
}
