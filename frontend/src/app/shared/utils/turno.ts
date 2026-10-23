export function turnoCajaId(storageKey: string): string {
  if (typeof sessionStorage === 'undefined') return '';
  const raw = sessionStorage.getItem(storageKey) ?? '';
  if (!raw) return '';
  try {
    const parsed = JSON.parse(raw) as { id?: unknown };
    return typeof parsed.id === 'string' && parsed.id ? parsed.id : raw;
  } catch {
    return raw;
  }
}

export function rangoFechaFiltro(filtros: Record<string, string>): Record<string, string> {
  return {
    ...filtros,
    desde: filtros['desde'] ? `${filtros['desde']}T00:00:00` : '',
    hasta: filtros['hasta'] ? `${filtros['hasta']}T23:59:59` : '',
  };
}
