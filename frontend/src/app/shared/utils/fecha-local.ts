/** Fecha de calendario local, YYYY-MM-DD. Evita que toISOString adelante el día en Lima. */
export function fechaLocal(fecha: Date = new Date()): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

export function haceDias(dias: number, base: Date = new Date()): string {
  const fecha = new Date(base);
  fecha.setDate(fecha.getDate() - dias);
  return fechaLocal(fecha);
}

export function periodoLocal(fecha: Date = new Date()): string {
  return fechaLocal(fecha).slice(0, 7);
}

/** Valor para input datetime-local, en la hora del navegador. */
export function fechaHoraLocal(fecha: Date = new Date()): string {
  const hora = String(fecha.getHours()).padStart(2, '0');
  const minuto = String(fecha.getMinutes()).padStart(2, '0');
  return `${fechaLocal(fecha)}T${hora}:${minuto}`;
}
