import { AbstractControl, ValidationErrors } from '@angular/forms';

export function documentoCliente(control: AbstractControl): ValidationErrors | null {
  const tipo = control.parent?.get('tipoDocumento')?.value as string | undefined;
  const value = String(control.value ?? '').trim();
  if (!value) return null;
  if (tipo === 'DNI' && !/^\d{8}$/.test(value)) return { documento: true };
  if (tipo === 'RUC' && !/^\d{11}$/.test(value)) return { documento: true };
  if ((tipo === 'CE' || tipo === 'PASAPORTE') && value.length < 6) return { documento: true };
  return null;
}

export function jsonObject(control: AbstractControl): ValidationErrors | null {
  const raw = String(control.value ?? '').trim();
  if (!raw) return { required: true };
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return { json: true };
    if (Object.keys(parsed).length === 0) return { json: true };
    return null;
  } catch {
    return { json: true };
  }
}

export function lineaProductoOServicio(control: AbstractControl): ValidationErrors | null {
  const producto = control.get('productoId')?.value;
  const servicio = control.get('servicioId')?.value;
  if (!producto && !servicio) return { linea: true };
  return null;
}
