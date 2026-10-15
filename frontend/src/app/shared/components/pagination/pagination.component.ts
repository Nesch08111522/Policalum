import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="pagination" aria-label="Paginación" *ngIf="totalElements || totalPages">
      <span class="text-sm text-muted">{{ range }}</span>
      <div class="cluster">
        <label class="text-sm text-muted" for="page-size">Filas</label>
        <select id="page-size" class="select" style="width:auto" [value]="size" (change)="onSize($event)">
          <option *ngFor="let option of sizes" [value]="option">{{ option }}</option>
        </select>
        <button type="button" class="page-btn" (click)="go(page - 1)" [disabled]="page <= 0" aria-label="Página anterior">‹</button>
        <button
          *ngFor="let item of visible"
          type="button"
          class="page-btn"
          [attr.aria-current]="item === page ? 'page' : null"
          (click)="go(item)">
          {{ item + 1 }}
        </button>
        <button type="button" class="page-btn" (click)="go(page + 1)" [disabled]="page + 1 >= totalPages" aria-label="Página siguiente">›</button>
      </div>
    </nav>
  `,
})
export class PaginationComponent {
  @Input() page = 0;
  @Input() size = 20;
  @Input() totalElements = 0;
  @Input() totalPages = 0;
  @Input() sizes = [10, 20, 50];
  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();

  get range(): string {
    if (!this.totalElements) return '0 registros';
    const start = this.page * this.size + 1;
    const end = Math.min((this.page + 1) * this.size, this.totalElements);
    return `${start}–${end} de ${this.totalElements}`;
  }

  get visible(): number[] {
    const start = Math.max(0, this.page - 2);
    const end = Math.min(this.totalPages - 1, this.page + 2);
    const pages: number[] = [];
    for (let index = start; index <= end; index++) pages.push(index);
    return pages;
  }

  go(next: number): void {
    if (next < 0 || next >= this.totalPages || next === this.page) return;
    this.pageChange.emit(next);
  }

  onSize(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.sizeChange.emit(value);
  }
}
