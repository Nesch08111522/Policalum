import { CommonModule } from '@angular/common';
import { Component, ContentChild, EventEmitter, Input, Output, TemplateRef } from '@angular/core';
import { ColumnDef } from '../../models/ui.models';
import { FechaPipe, NumeroPipe, PenPipe } from '../../pipes/format.pipes';
import { displayText, readPath } from '../../utils/feedback';
import { EmptyStateComponent } from '../empty-state/empty-state.component';
import { LoaderComponent } from '../loader/loader.component';
import { PaginationComponent } from '../pagination/pagination.component';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, PenPipe, FechaPipe, NumeroPipe, EmptyStateComponent, LoaderComponent, PaginationComponent],
  template: `
    <section class="table-wrap">
      <div class="table-scroll">
        <table class="table" *ngIf="!loading && !error">
          <thead>
            <tr>
              <th
                *ngFor="let column of columns"
                [style.width]="column.width"
                [ngClass]="{ sortable: !!column.sortable, 'align-end': column.align === 'end', 'align-center': column.align === 'center' }"
                (click)="sort(column)"
                [attr.aria-sort]="aria(column)">
                {{ column.label }}
                <span *ngIf="column.sortable" class="sort-mark">{{ mark(column) }}</span>
              </th>
              <th *ngIf="actions" class="col-actions">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let row of rows; let index = index; trackBy: track" (click)="rowClick.emit(row)">
              <td *ngFor="let column of columns" [ngClass]="{ 'align-end': column.align === 'end', 'align-center': column.align === 'center' }">
                <ng-container [ngSwitch]="column.kind || 'text'">
                  <span *ngSwitchCase="'money'">{{ cell(row, column.key) | pen }}</span>
                  <span *ngSwitchCase="'date'">{{ cell(row, column.key) | fecha }}</span>
                  <span *ngSwitchCase="'number'">{{ cell(row, column.key) | numero }}</span>
                  <span *ngSwitchCase="'bool'" class="badge" [class.badge-success]="truthy(row, column.key)" [class.badge-neutral]="!truthy(row, column.key)">
                    {{ truthy(row, column.key) ? 'Sí' : 'No' }}
                  </span>
                  <span *ngSwitchCase="'badge'" class="badge" [ngClass]="badgeClass(row, column)">{{ text(row, column.key) }}</span>
                  <span *ngSwitchDefault>{{ text(row, column.key) }}</span>
                </ng-container>
              </td>
              <td *ngIf="actions" class="col-actions" (click)="$event.stopPropagation()">
                <ng-container *ngTemplateOutlet="actions; context: { $implicit: row }"></ng-container>
              </td>
            </tr>
          </tbody>
        </table>
        <app-loader *ngIf="loading"></app-loader>
        <app-empty-state *ngIf="!loading && !error && !rows.length" [title]="emptyTitle" [message]="emptyMessage" [actionLabel]="emptyAction" (action)="empty.emit()"></app-empty-state>
        <div *ngIf="!loading && error" class="alert alert-error" style="margin:16px">
          <div>
            <strong>No se pudieron cargar los datos</strong>
            <p>{{ error }}</p>
            <p *ngIf="status" class="text-xs">Código {{ status }}</p>
          </div>
          <button type="button" class="btn btn-ghost btn-sm" (click)="retry.emit()">Reintentar</button>
        </div>
      </div>
      <app-pagination
        *ngIf="!error"
        [page]="page"
        [size]="size"
        [totalElements]="totalElements"
        [totalPages]="totalPages"
        (pageChange)="pageChange.emit($event)"
        (sizeChange)="sizeChange.emit($event)">
      </app-pagination>
    </section>
  `,
})
export class DataTableComponent {
  @Input() columns: ColumnDef[] = [];
  @Input() rows: unknown[] = [];
  @Input() loading = false;
  @Input() error: string | null = null;
  @Input() status: number | null = null;
  @Input() page = 0;
  @Input() size = 20;
  @Input() totalElements = 0;
  @Input() totalPages = 0;
  @Input() sortField = '';
  @Input() sortDirection: 'asc' | 'desc' = 'asc';
  @Input() emptyTitle = 'Sin registros';
  @Input() emptyMessage = 'Cuando existan datos, aparecerán en esta tabla.';
  @Input() emptyAction = '';
  @Output() sortChange = new EventEmitter<{ key: string; direction: 'asc' | 'desc' }>();
  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();
  @Output() rowClick = new EventEmitter<unknown>();
  @Output() retry = new EventEmitter<void>();
  @Output() empty = new EventEmitter<void>();
  @ContentChild('rowActions') actions?: TemplateRef<{ $implicit: unknown }>;

  cell(row: unknown, key: string): unknown {
    return readPath(row, key);
  }

  text(row: unknown, key: string): string {
    return displayText(this.cell(row, key));
  }

  truthy(row: unknown, key: string): boolean {
    return Boolean(this.cell(row, key));
  }

  badgeClass(row: unknown, column: ColumnDef): string {
    const raw = String(this.cell(row, column.key) ?? '');
    return `badge-${column.badge?.[raw] ?? 'neutral'}`;
  }

  sort(column: ColumnDef): void {
    if (!column.sortable) return;
    const key = column.sortKey ?? column.key;
    const direction = this.sortField === key && this.sortDirection === 'asc' ? 'desc' : 'asc';
    this.sortChange.emit({ key, direction });
  }

  aria(column: ColumnDef): string | null {
    const key = column.sortKey ?? column.key;
    if (!column.sortable || this.sortField !== key) return column.sortable ? 'none' : null;
    return this.sortDirection === 'asc' ? 'ascending' : 'descending';
  }

  mark(column: ColumnDef): string {
    const key = column.sortKey ?? column.key;
    if (this.sortField !== key) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  track(index: number, row: unknown): string {
    if (row && typeof row === 'object' && 'id' in row && (row as { id?: string }).id) return String((row as { id?: string }).id);
    return String(index);
  }
}
