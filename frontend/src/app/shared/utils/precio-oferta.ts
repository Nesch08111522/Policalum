export interface ConOferta {
  precioVentaBase?: number | null;
  precioOferta?: number | null;
  ofertaDesde?: string | null;
  ofertaHasta?: string | null;
}

export function hoyLima(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima' }).format(new Date());
}

export function ofertaVigente(item: ConOferta | null | undefined): boolean {
  if (!item?.precioOferta || !item.ofertaDesde || !item.ofertaHasta) return false;
  const hoy = hoyLima();
  return item.ofertaDesde <= hoy && hoy <= item.ofertaHasta;
}

export function precioVigente(base: number, item: ConOferta | null | undefined): number {
  return ofertaVigente(item) ? Number(item?.precioOferta) : base;
}

export function precioFraccionVigente(precioLista: number, item: ConOferta | null | undefined): number {
  if (!ofertaVigente(item) || !item?.precioVentaBase) return precioLista;
  return Math.round(precioLista * Number(item.precioOferta) / Number(item.precioVentaBase) * 100) / 100;
}
