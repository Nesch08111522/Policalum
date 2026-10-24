export type BadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export interface ColumnDef {
  key: string;
  label: string;
  sortable?: boolean;
  sortKey?: string;
  width?: string;
  align?: 'start' | 'center' | 'end';
  kind?: 'text' | 'money' | 'date' | 'badge' | 'bool' | 'number';
  badge?: Record<string, BadgeTone>;
}

export interface SelectOption {
  value: string;
  label: string;
}

export interface Breadcrumb {
  label: string;
  link?: string;
}

export interface TabItem {
  id: string;
  label: string;
}

export interface PageResult<T> {
  content?: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
}
