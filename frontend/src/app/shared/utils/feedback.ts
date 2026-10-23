import { AbstractControl, FormGroup } from '@angular/forms';
import { ApiProblem } from '../../core/api/openapi.types';

export function isProblem(value: unknown): value is ApiProblem {
  return typeof value === 'object' && value !== null && ('message' in value || 'status' in value);
}

export function problemText(error: unknown): string {
  if (isProblem(error)) {
    if (error.errores && error.errores.length > 0) {
      return error.errores.map((item) => `Fila ${item.fila ?? '?'}: ${item.motivo ?? 'Error'}`).join('\n');
    }
    if (error.fieldErrors && Object.keys(error.fieldErrors).length > 0) {
      return Object.entries(error.fieldErrors)
        .map(([campo, mensaje]) => `${campo}: ${mensaje}`)
        .join(' · ');
    }
    return error.message ?? 'No se pudo completar la operación.';
  }
  return 'No se pudo completar la operación.';
}

export function problemStatus(error: unknown): number | null {
  return isProblem(error) && typeof error.status === 'number' ? error.status : null;
}

export function applyServerErrors(form: FormGroup, error: unknown): void {
  if (!isProblem(error) || !error.fieldErrors) return;
  for (const [field, message] of Object.entries(error.fieldErrors)) {
    const control = form.get(field);
    if (!control) continue;
    control.setErrors({ ...(control.errors ?? {}), server: message });
    control.markAsTouched();
  }
}

export function fieldError(control: AbstractControl | null, custom: Record<string, string> = {}): string | null {
  if (!control || !control.errors || !(control.touched || control.dirty)) return null;
  const [key] = Object.keys(control.errors);
  if (!key) return null;
  if (key === 'server' && typeof control.errors['server'] === 'string') return String(control.errors['server']);
  if (key === 'minlength') {
    const detail = control.errors['minlength'] as { requiredLength?: number };
    return `Mínimo ${detail.requiredLength ?? ''} caracteres.`;
  }
  if (key === 'maxlength') {
    const detail = control.errors['maxlength'] as { requiredLength?: number };
    return `Máximo ${detail.requiredLength ?? ''} caracteres.`;
  }
  if (key === 'min') {
    const detail = control.errors['min'] as { min?: number };
    return `El mínimo es ${detail.min}.`;
  }
  if (key === 'max') {
    const detail = control.errors['max'] as { max?: number };
    return `El máximo es ${detail.max}.`;
  }
  const defaults: Record<string, string> = {
    required: 'Este campo es obligatorio.',
    email: 'Ingresa un correo válido.',
    pattern: 'El formato no es válido.',
    documento: 'El documento no coincide con el tipo seleccionado.',
    linea: 'Indica un producto o un servicio.',
    lineaUnica: 'Indica un producto, un servicio o una fracción.',
    json: 'El JSON de permisos no es válido.',
  };
  return custom[key] ?? defaults[key] ?? 'Valor no válido.';
}

export function readPath(row: unknown, path: string): unknown {
  return path.split('.').reduce<unknown>((current, part) => {
    if (!current || typeof current !== 'object' || !(part in current)) return undefined;
    return (current as Record<string, unknown>)[part];
  }, row);
}

export function displayText(value: unknown): string {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'Sí' : 'No';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}
